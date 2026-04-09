package com.example.androidwake.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.Toast
import com.example.androidwake.MainActivity
import com.example.androidwake.R
import com.example.androidwake.app.AppServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickWakeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        QuickWakeWidgetUpdater.requestUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_WAKE_ALL -> handleWakeAll(context)
            ACTION_WAKE_MACHINE -> {
                val mac = intent.getStringExtra(EXTRA_MACHINE_MAC) ?: return
                handleWakeMachine(context, mac)
            }
        }
    }

    private fun handleWakeAll(context: Context) {
        if (!QuickWakeWidgetActionRateLimiter.shouldAllowWakeAll()) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val actionResult = makeActionHandler(context).wakeAll()
                showToastForResult(context, actionResult)
                QuickWakeWidgetUpdater.requestUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleWakeMachine(context: Context, mac: String) {
        if (!QuickWakeWidgetActionRateLimiter.shouldAllowWakeMachine(mac)) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val actionResult = makeActionHandler(context).wakeMachineByMac(mac)
                showToastForResult(context, actionResult)
                QuickWakeWidgetUpdater.requestUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun makeActionHandler(context: Context): QuickWakeWidgetActionHandler {
        val repository = AppServices.wakeRepository(context)
        val wifiProvider = AppServices.wifiIdentityProvider(context)
        val wolSender = AppServices.wolSender()
        val resolver = QuickWakeWidgetStateResolver(repository, wifiProvider)
        return QuickWakeWidgetActionHandler(resolver, wolSender)
    }

    private suspend fun showToastForResult(context: Context, result: QuickWakeActionResult) {
        val text = when (result) {
            is QuickWakeActionResult.Success -> {
                if (result.count == 1) "Wake sent."
                else "Wake sent to ${result.count} machine(s)."
            }

            QuickWakeActionResult.NotApproved -> "Not connected to an approved network."
            QuickWakeActionResult.MachineNotFound -> "Machine not available on current approved network."
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        scope.launch {
            val repository = AppServices.wakeRepository(context)
            val wifiProvider = AppServices.wifiIdentityProvider(context)
            val resolver = QuickWakeWidgetStateResolver(repository, wifiProvider)
            val resolved = resolver.resolve()
            val state = QuickWakeWidgetRenderStateStore.choose(resolved)

            appWidgetIds.forEach { widgetId ->
                val views = when (state) {
                    is QuickWakeWidgetState.NonApproved -> buildNonApprovedViews(context, widgetId, state)
                    is QuickWakeWidgetState.Approved -> buildApprovedViews(context, widgetId, state)
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun buildNonApprovedViews(
        context: Context,
        appWidgetId: Int,
        state: QuickWakeWidgetState.NonApproved,
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.quick_wake_widget_disconnected).apply {
            setTextViewText(R.id.widget_not_connected_text, state.message)
            setOnClickPendingIntent(R.id.widget_open_app_button, appLaunchPendingIntent(context, appWidgetId))
        }
    }

    private fun buildApprovedViews(
        context: Context,
        appWidgetId: Int,
        state: QuickWakeWidgetState.Approved,
    ): RemoteViews {
        val serviceIntent = Intent(context, QuickWakeWidgetRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

        return RemoteViews(context.packageName, R.layout.quick_wake_widget).apply {
            setTextViewText(R.id.widget_network_title, "${state.network.ssid}")
            setTextViewText(R.id.widget_empty_text, context.getString(R.string.widget_no_machines))
            setRemoteAdapter(R.id.widget_machine_list, serviceIntent)
            setEmptyView(R.id.widget_machine_list, R.id.widget_empty_text)
            setOnClickPendingIntent(
                R.id.widget_wake_all_button,
                wakeAllPendingIntent(context, appWidgetId),
            )
            setPendingIntentTemplate(
                R.id.widget_machine_list,
                machineTemplatePendingIntent(context, appWidgetId),
            )
        }
    }

    private fun appLaunchPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun wakeAllPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, QuickWakeWidgetProvider::class.java).apply {
            action = ACTION_WAKE_ALL
            component = ComponentName(context, QuickWakeWidgetProvider::class.java)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun machineTemplatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, QuickWakeWidgetProvider::class.java).apply {
            action = ACTION_WAKE_MACHINE
            component = ComponentName(context, QuickWakeWidgetProvider::class.java)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId + 10_000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_WAKE_ALL = "com.example.androidwake.widget.ACTION_WAKE_ALL"
        const val ACTION_WAKE_MACHINE = "com.example.androidwake.widget.ACTION_WAKE_MACHINE"
        const val EXTRA_MACHINE_MAC = "extra_machine_mac"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

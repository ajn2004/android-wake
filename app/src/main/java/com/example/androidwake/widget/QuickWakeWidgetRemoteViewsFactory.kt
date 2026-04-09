package com.example.androidwake.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.androidwake.R
import com.example.androidwake.app.AppServices
import com.example.androidwake.domain.Machine
import kotlinx.coroutines.runBlocking

class QuickWakeWidgetRemoteViewsFactory(
    private val context: android.content.Context,
) : RemoteViewsService.RemoteViewsFactory {

    private var machines: List<Machine> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val repository = AppServices.wakeRepository(context)
        val wifiProvider = AppServices.wifiIdentityProvider(context)
        val resolver = QuickWakeWidgetStateResolver(repository, wifiProvider)

        val resolved = runBlocking { resolver.resolve() }
        val state = QuickWakeWidgetRenderStateStore.choose(resolved)
        machines = if (state is QuickWakeWidgetState.Approved) state.machines else emptyList()
    }

    override fun onDestroy() {
        machines = emptyList()
    }

    override fun getCount(): Int = machines.size

    override fun getViewAt(position: Int): RemoteViews {
        val machine = machines[position]
        return RemoteViews(context.packageName, R.layout.quick_wake_widget_machine_row).apply {
            setTextViewText(R.id.widget_machine_name, machine.name)
            setTextViewText(R.id.widget_machine_mac, machine.macAddress)
            setOnClickFillInIntent(
                R.id.widget_machine_row,
                Intent().apply {
                    action = QuickWakeWidgetProvider.ACTION_WAKE_MACHINE
                    putExtra(QuickWakeWidgetProvider.EXTRA_MACHINE_MAC, machine.macAddress)
                }
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = machines[position].id

    override fun hasStableIds(): Boolean = true
}

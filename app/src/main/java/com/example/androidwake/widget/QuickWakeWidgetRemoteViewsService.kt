package com.example.androidwake.widget

import android.content.Intent
import android.widget.RemoteViewsService

class QuickWakeWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return QuickWakeWidgetRemoteViewsFactory(applicationContext)
    }
}

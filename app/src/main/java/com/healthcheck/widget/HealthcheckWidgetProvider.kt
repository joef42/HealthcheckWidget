package com.healthcheck.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class HealthcheckWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.healthcheck.widget.ACTION_REFRESH"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Show a loading state immediately, then kick off a background fetch
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#607D8B"))
            views.setTextViewText(R.id.status_text, "…")
            appWidgetManager.updateAppWidget(id, views)
        }
        enqueueImmediateUpdate(context)
    }

    override fun onEnabled(context: Context) {
        schedulePeriodicWork(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WidgetUpdateWorker.WORK_NAME_PERIODIC)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            enqueueImmediateUpdate(context)
        }
    }

    private fun enqueueImmediateUpdate(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WidgetUpdateWorker.WORK_NAME_ONCE,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun schedulePeriodicWork(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

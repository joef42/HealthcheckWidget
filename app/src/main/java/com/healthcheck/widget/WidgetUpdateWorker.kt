package com.healthcheck.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(PREF_API_KEY, "") ?: ""

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, HealthcheckWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return Result.success()

        val status: ChecksStatus? = if (apiKey.isNotEmpty()) {
            var result = ApiClient.fetchStatus(apiKey)
            if (result == null) {
                // Retry up to ~60s before showing ERR (handles transient network unavailability
                // e.g. right after screen unlock): 5s + 10s + 15s + 30s = 60s total
                val retryDelays = longArrayOf(5_000, 10_000, 15_000, 30_000)
                for (delay in retryDelays) {
                    Thread.sleep(delay)
                    result = ApiClient.fetchStatus(apiKey)
                    if (result != null) break
                }
            }
            result
        } else null

        for (id in ids) {
            applyViews(context, appWidgetManager, id, apiKey, status)
        }

        return Result.success()
    }

    companion object {
        const val PREFS_NAME = "HealthcheckWidgetPrefs"
        const val PREF_API_KEY = "api_key"
        const val WORK_NAME_PERIODIC = "healthcheck_periodic"
        const val WORK_NAME_ONCE = "healthcheck_once"

        fun applyViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            apiKey: String,
            status: ChecksStatus?
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            when {
                apiKey.isEmpty() -> {
                    // Prompt user to configure
                    views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#607D8B"))
                    views.setTextViewText(R.id.status_text, "TAP\nSETUP")
                    views.setTextViewText(R.id.timestamp_text, "")
                    val configIntent = android.content.Intent(context, WidgetConfigActivity::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val pi = android.app.PendingIntent.getActivity(
                        context, widgetId, configIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pi)
                }

                status == null -> {
                    // Network/parse error
                    views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#FF9800"))
                    views.setTextViewText(R.id.status_text, "ERR")
                    views.setTextViewText(R.id.timestamp_text, currentTime())
                    setRefreshClickListener(context, widgetId, views)
                }

                else -> {
                    val isOk = status.allOk
                    val bg = if (isOk) Color.parseColor("#1E1E1E") else Color.parseColor("#D32F2F")
                    views.setInt(R.id.widget_root, "setBackgroundColor", bg)
                    views.setTextColor(R.id.status_text, if (isOk) Color.parseColor("#388E3C") else Color.WHITE)
                    views.setTextViewText(R.id.status_text, statusSpan(status))
                    views.setTextViewText(R.id.timestamp_text, currentTime())
                    setRefreshClickListener(context, widgetId, views)
                }
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun statusSpan(status: ChecksStatus): CharSequence {
            val text = "${status.ok}/${status.total}"
            val span = SpannableString(text)
            // Make the ok count ~50% larger than the base text size
            span.setSpan(RelativeSizeSpan(1.5f), 0, status.ok.toString().length, 0)
            return span
        }

        private fun currentTime(): String =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        private fun setRefreshClickListener(context: Context, widgetId: Int, views: RemoteViews) {
            val refreshIntent = android.content.Intent(context, HealthcheckWidgetProvider::class.java).apply {
                action = HealthcheckWidgetProvider.ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val pi = android.app.PendingIntent.getBroadcast(
                context, widgetId, refreshIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
        }
    }
}

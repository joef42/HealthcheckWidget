package com.healthcheck.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Default result is CANCELED so backing out doesn't add the widget
        setResult(RESULT_CANCELED)

        setContentView(R.layout.activity_config)

        val prefs = getSharedPreferences(WidgetUpdateWorker.PREFS_NAME, Context.MODE_PRIVATE)
        val apiKeyInput = findViewById<EditText>(R.id.api_key_input)
        apiKeyInput.setText(prefs.getString(WidgetUpdateWorker.PREF_API_KEY, ""))

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val apiKey = apiKeyInput.text.toString().trim()
            if (apiKey.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_key), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString(WidgetUpdateWorker.PREF_API_KEY, apiKey).apply()

            // Kick off an immediate fetch so the widget shows data right away
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            WorkManager.getInstance(this).enqueueUniqueWork(
                WidgetUpdateWorker.WORK_NAME_ONCE,
                ExistingWorkPolicy.REPLACE,
                request
            )

            setResult(RESULT_OK, Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            })
            finish()
        }
    }
}

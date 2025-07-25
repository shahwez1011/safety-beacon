package com.example.safetybeacon.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.safetybeacon.R
import com.example.safetybeacon.service.EmergencySosService

class EmergencySosWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (ACTION_SOS_CLICKED == intent.action) {
            // Start the emergency SOS service
            val sosIntent = Intent(context, EmergencySosService::class.java)
            context.startForegroundService(sosIntent)
        }
    }

    companion object {
        private const val ACTION_SOS_CLICKED = "com.example.safetybeacon.SOS_CLICKED"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_emergency_sos)

            // Create an Intent to handle the SOS button click
            val sosIntent = Intent(context, EmergencySosWidget::class.java).apply {
                action = ACTION_SOS_CLICKED
            }
            val sosPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                sosIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set the click listener for the SOS button
            views.setOnClickPendingIntent(R.id.widget_sos_button, sosPendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, EmergencySosWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
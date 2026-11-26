package app.daytick.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import android.appwidget.AppWidgetManager
import app.daytick.data.HabitStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

object WidgetSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch { runCatching { HabitWidget().updateAll(app) } }
    }

    /** 排一個「過午夜」的鬧鐘，讓小工具在新的一天自動清空打勾。 */
    fun scheduleMidnight(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val nextMidnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli() + 5_000
        val pi = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, DayChangeReceiver::class.java).setAction(ACTION_MIDNIGHT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarm.set(AlarmManager.RTC, nextMidnight, pi)
    }

    const val ACTION_MIDNIGHT = "app.daytick.action.MIDNIGHT"
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        HabitStore.get(context).refreshDate()
        WidgetSync.scheduleMidnight(context)
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetSync.scheduleMidnight(context)
    }
}

/** 午夜、開機、手動改時間或時區時，更新日期並重新整理小工具。 */
class DayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        HabitStore.get(context).refreshDate()
        WidgetSync.scheduleMidnight(context)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                HabitWidget().updateAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}

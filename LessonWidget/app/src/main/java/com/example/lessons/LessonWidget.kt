package com.example.lessons

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

class LessonWidget : AppWidgetProvider() {

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = update(c)

    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        if (i.action == REFRESH) update(c)
    }

    private fun refreshIntent(c: Context) = PendingIntent.getBroadcast(
        c, 0, Intent(c, LessonWidget::class.java).setAction(REFRESH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun openIntent(c: Context) = PendingIntent.getActivity(
        c, 1, Intent(c, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun update(c: Context) {
        val mgr = AppWidgetManager.getInstance(c)
        val ids = mgr.getAppWidgetIds(ComponentName(c, LessonWidget::class.java))
        val now = Calendar.getInstance()
        val list = Schedule.upcoming(now, 3)
        val r = list.firstOrNull()

        for (id in ids) {
            val v = RemoteViews(c.packageName, R.layout.widget)
            if (r == null) {
                v.setTextViewText(R.id.label, "УРОКОВ НЕТ")
                v.setTextViewText(R.id.time, "—")
                v.setTextViewText(R.id.subject, "Отдыхай")
                v.setTextViewText(R.id.after, "")
            } else {
                v.setTextViewText(R.id.label, if (r.current) "СЕЙЧАС УРОК" else "СЛЕДУЮЩИЙ УРОК")
                val day = when (r.dayOffset) {
                    0 -> ""
                    1 -> "Завтра · "
                    else -> Schedule.shortDays[r.dayOfWeek] + " · "
                }
                v.setTextViewText(R.id.time,
                    "$day${r.lesson.n}-й урок · ${Schedule.fmt(r.start)}–${Schedule.fmt(r.end)}")
                v.setTextViewText(R.id.subject, r.lesson.name)
                val rest = list.drop(1).joinToString("\n") { x ->
                    val d = when {
                        x.dayOffset == r.dayOffset -> ""
                        x.dayOffset == 1 -> "Завтра "
                        else -> Schedule.shortDays[x.dayOfWeek] + " "
                    }
                    "Дальше: $d${Schedule.fmt(x.start)} · ${x.lesson.name}"
                }
                v.setTextViewText(R.id.after, rest)
            }
            v.setOnClickPendingIntent(R.id.root, openIntent(c))
            mgr.updateAppWidget(id, v)
        }
        scheduleNext(c, now, r)
    }

    // Обновляем виджет ровно в момент начала/конца урока
    private fun scheduleNext(c: Context, now: Calendar, r: Result?) {
        val t = (now.clone() as Calendar).apply { set(Calendar.SECOND, 5); set(Calendar.MILLISECOND, 0) }
        if (r != null && r.dayOffset == 0) {
            val target = if (r.current) r.end else r.start
            t.set(Calendar.HOUR_OF_DAY, target / 60); t.set(Calendar.MINUTE, target % 60)
        } else {
            t.add(Calendar.DAY_OF_YEAR, 1); t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 5)
        }
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.set(AlarmManager.RTC, t.timeInMillis, refreshIntent(c))
    }

    companion object { const val REFRESH = "com.example.lessons.REFRESH" }
}

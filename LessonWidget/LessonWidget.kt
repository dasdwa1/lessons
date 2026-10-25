package com.example.lessons

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import java.util.Calendar

class LessonWidget : AppWidgetProvider() {

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = update(c)

    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        when (i.action) {
            REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> update(c)
        }
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
        val now = Schedule.now(c)
        val list = Schedule.upcoming(now, 3)
        val r = list.firstOrNull()

        for (id in ids) {
            val v = RemoteViews(c.packageName, R.layout.widget)
            if (r == null) {
                v.setTextViewText(R.id.label, "УРОКОВ НЕТ")
                v.setTextViewText(R.id.time, "—")
                v.setTextViewText(R.id.subject, "Отдыхай")
                v.setTextViewText(R.id.after, "")
                v.setViewVisibility(R.id.timer, View.GONE)
            } else {
                v.setTextViewText(R.id.label, if (r.current) "СЕЙЧАС" else "СЛЕДУЮЩИЙ")
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

                // Обратный отсчёт: тикает сам внутри виджета, приложение не просыпается
                val target = (now.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, r.dayOffset)
                    val m = if (r.current) r.end else r.start
                    set(Calendar.HOUR_OF_DAY, m / 60)
                    set(Calendar.MINUTE, m % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val left = target.timeInMillis - now.timeInMillis
                v.setViewVisibility(R.id.timer, View.VISIBLE)
                v.setChronometerCountDown(R.id.timer, true)
                v.setChronometer(R.id.timer, SystemClock.elapsedRealtime() + left,
                    if (r.current) "до конца %s" else "через %s", true)
            }
            v.setOnClickPendingIntent(R.id.root, openIntent(c))
            mgr.updateAppWidget(id, v)
        }
        scheduleNext(c, now, r)
    }

    // Обновляем виджет ровно в момент начала/конца урока (а вне уроков — в полночь)
    private fun scheduleNext(c: Context, now: Calendar, r: Result?) {
        val t = (now.clone() as Calendar).apply { set(Calendar.SECOND, 5); set(Calendar.MILLISECOND, 0) }
        if (r != null && r.dayOffset == 0) {
            val target = if (r.current) r.end else r.start
            t.set(Calendar.HOUR_OF_DAY, target / 60); t.set(Calendar.MINUTE, target % 60)
        } else {
            t.add(Calendar.DAY_OF_YEAR, 1); t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 5)
        }
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.timeInMillis - Schedule.offset, refreshIntent(c))
    }

    companion object { const val REFRESH = "com.example.lessons.REFRESH" }
}

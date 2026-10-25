package com.example.lessons

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import java.util.Calendar

class MainActivity : Activity() {

    private val gray = Color.parseColor("#9AA3B2")
    private val blue = Color.parseColor("#5AB0F5")
    private val dayNames = arrayOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
    private val dayCodes = intArrayOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)

    override fun onResume() {
        super.onResume()
        refreshWidget()
        if (Schedule.isSet(this)) render() else showSetup()
    }

    private fun refreshWidget() =
        sendBroadcast(Intent(this, LessonWidget::class.java).setAction(LessonWidget.REFRESH))

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun screen(box: LinearLayout) = setContentView(ScrollView(this).apply {
        setBackgroundColor(Color.parseColor("#0F1218"))
        addView(box)
    })

    private fun newBox() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(48), dp(20), dp(24))
    }

    // Экран, где один раз задаём своё время
    private fun showSetup() {
        val now = Schedule.now(this)
        val box = newBox()
        box.addView(tv("Настрой время", 26f, Color.WHITE, true))
        box.addView(tv("Укажи, какой сейчас день и время. Приложение запомнит поправку и дальше будет считать время само.", 14f, gray, false))

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dayNames)
        val todayIdx = dayCodes.indexOf(now.get(Calendar.DAY_OF_WEEK))
        spinner.setSelection(if (todayIdx >= 0) todayIdx else 0)
        box.addView(spinner)

        val picker = TimePicker(this)
        picker.setIs24HourView(true)
        picker.hour = now.get(Calendar.HOUR_OF_DAY)
        picker.minute = now.get(Calendar.MINUTE)
        box.addView(picker)

        val save = Button(this)
        save.text = "Сохранить"
        save.setOnClickListener {
            picker.clearFocus()
            Schedule.setClock(this, dayCodes[spinner.selectedItemPosition], picker.hour, picker.minute)
            refreshWidget()
            render()
        }
        box.addView(save)
        screen(box)
    }

    // Главный экран: ближайшие уроки
    private fun render() {
        val now = Schedule.now(this)
        val box = newBox()
        box.addView(tv("Ближайшие уроки", 26f, Color.WHITE, true))
        box.addView(tv("Сейчас: %02d:%02d, %s".format(
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE),
            Schedule.fullDays[now.get(Calendar.DAY_OF_WEEK)]), 14f, gray, false))

        val list = Schedule.upcoming(now, 15)
        if (list.isEmpty()) box.addView(tv("Уроков нет", 16f, Color.WHITE, false))

        var last = -1
        for (r in list) {
            if (r.dayOffset != last) {
                last = r.dayOffset
                val full = Schedule.fullDays[r.dayOfWeek]
                val title = when (r.dayOffset) {
                    0 -> "Сегодня, $full"
                    1 -> "Завтра, $full"
                    else -> full
                }
                val h = tv(title, 15f, blue, true)
                h.setPadding(0, dp(18), 0, dp(4))
                box.addView(h)
            }
            val mark = if (r.current) "  ● идёт сейчас" else ""
            box.addView(tv(
                "${Schedule.fmt(r.start)}–${Schedule.fmt(r.end)}   ${r.lesson.name}$mark",
                16f, Color.WHITE, r.current))
        }

        val change = Button(this)
        change.text = "Изменить время"
        change.setOnClickListener { showSetup() }
        box.addView(change)
        screen(box)
    }
}

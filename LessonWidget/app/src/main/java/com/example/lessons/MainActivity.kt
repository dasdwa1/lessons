package com.example.lessons

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

class MainActivity : Activity() {

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun render() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(24))
        }
        box.addView(tv("Ближайшие уроки", 26f, Color.WHITE, true))

        val list = Schedule.upcoming(Calendar.getInstance(), 15)
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
                val h = tv(title, 15f, Color.parseColor("#5AB0F5"), true)
                h.setPadding(0, dp(18), 0, dp(4))
                box.addView(h)
            }
            val mark = if (r.current) "  ● идёт сейчас" else ""
            box.addView(tv(
                "${Schedule.fmt(r.start)}–${Schedule.fmt(r.end)}   ${r.lesson.name}$mark",
                16f, Color.WHITE, r.current))
        }

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F1218"))
            addView(box)
        })
    }
}

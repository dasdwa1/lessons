package com.example.lessons

import java.util.Calendar

data class Lesson(val n: Int, val name: String, val room: String)

data class Result(
    val lesson: Lesson, val start: Int, val end: Int,
    val dayOffset: Int, val dayOfWeek: Int, val current: Boolean
)

object Schedule {
    private const val GYM = "Спортзал №1"

    // Расписание 8в. Меняй здесь. Пустого урока просто нет в списке.
    val days: Map<Int, List<Lesson>> = mapOf(
        Calendar.MONDAY to listOf(
            Lesson(1, "Алгебра", "407"), Lesson(2, "Биология", "304"),
            Lesson(3, "Русский язык", "402"), Lesson(4, "Литература", "402"),
            Lesson(5, "Решение задач по математике", "407"), Lesson(6, "История", "413"),
            Lesson(7, "Разговор о важном", "102")),
        Calendar.TUESDAY to listOf(
            Lesson(1, "Физкультура", GYM), Lesson(2, "Физика", "204"),
            Lesson(3, "ОБЗР", "?"), Lesson(4, "История", "413"),
            Lesson(5, "Геометрия", "407"), Lesson(6, "Английский", "403 / 411")),
        Calendar.WEDNESDAY to listOf(
            Lesson(2, "Информатика", "318 / 312"), Lesson(3, "Семьеведение", "415"),
            Lesson(4, "Музыка", "102"), Lesson(5, "Труд (технология)", "201 / 107"),
            Lesson(6, "Химия", "302"), Lesson(7, "Алгебра", "407")),
        Calendar.THURSDAY to listOf(
            Lesson(1, "Геометрия", "407"), Lesson(2, "История", "413"),
            Lesson(3, "Биология", "304"), Lesson(4, "География", "409"),
            Lesson(5, "Русский язык", "402"), Lesson(6, "Английский", "403 / 411"),
            Lesson(7, "Практикум по русскому языку", "402")),
        Calendar.FRIDAY to listOf(
            Lesson(2, "Физкультура", GYM), Lesson(3, "Вероятность и статистика", "407"),
            Lesson(4, "Физика", "204"), Lesson(5, "Литература", "402"),
            Lesson(6, "Химия", "302"), Lesson(7, "Россия — мои горизонты", "102")),
        Calendar.SATURDAY to listOf(
            Lesson(1, "Алгебра", "407"), Lesson(2, "Английский", "403 / 411"),
            Lesson(3, "Русский язык", "402"), Lesson(4, "Физкультура", GYM),
            Lesson(5, "География", "409"))
    )

    // Звонки в минутах от полуночи (начало, конец)
    private fun m(h: Int, min: Int) = h * 60 + min
    private val normal = listOf(
        m(8,0) to m(8,40), m(8,50) to m(9,30), m(9,45) to m(10,25), m(10,40) to m(11,20),
        m(11,35) to m(12,15), m(12,25) to m(13,5), m(13,15) to m(13,55))
    private val tuesday = listOf(
        m(8,40) to m(9,20), m(9,30) to m(10,10), m(10,20) to m(11,0), m(11,15) to m(11,55),
        m(12,10) to m(12,50), m(13,0) to m(13,40), m(13,50) to m(14,30))

    private fun bells(dow: Int) = if (dow == Calendar.TUESDAY) tuesday else normal

    fun fmt(min: Int) = "%02d:%02d".format(min / 60, min % 60)

    fun find(now: Calendar): Result? {
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        for (offset in 0..7) {
            val d = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dow = d.get(Calendar.DAY_OF_WEEK)
            val lessons = days[dow] ?: continue
            for (l in lessons) {
                val (s, e) = bells(dow)[l.n - 1]
                if (offset == 0 && nowMin >= e) continue
                return Result(l, s, e, offset, dow, offset == 0 && nowMin >= s)
            }
        }
        return null
    }
}

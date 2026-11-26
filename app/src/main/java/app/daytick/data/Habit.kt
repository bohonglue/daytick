package app.daytick.data

import java.time.LocalDate

enum class Section(val label: String, val emoji: String) {
    MORNING("早上", "🌅"),
    AFTERNOON("下午", "☀️"),
    EVENING("晚上", "🌙"),
    ANYTIME("隨時", "⏰"),
}

enum class Kind(val label: String) {
    /** 依星期重複的例行事項 */
    ROUTINE("每週重複"),

    /** 只做一次的待辦，可以指定日期 */
    TODO("一次性待辦"),
}

data class Habit(
    val id: String,
    val name: String,
    val emoji: String = "✅",
    val color: Long = 0xFF4CAF50,
    val section: Section = Section.ANYTIME,
    /** 例行事項要執行的星期，1 = 星期一 … 7 = 星期日 */
    val days: Set<Int> = ALL_DAYS,
    val order: Int = 0,
    val kind: Kind = Kind.ROUTINE,
    /** 一次性待辦的日期；null 表示「現在就要做」 */
    val dueDate: LocalDate? = null,
) {
    val isTodo get() = kind == Kind.TODO

    fun isScheduled(date: LocalDate) = !isTodo && date.dayOfWeek.value in days

    companion object {
        val ALL_DAYS = (1..7).toSet()
    }
}

data class AppData(
    val habits: List<Habit> = emptyList(),
    /** id -> 已完成日期（yyyy-MM-dd）。一次性待辦只會有一筆：完成的那天。 */
    val done: Map<String, Set<String>> = emptyMap(),
) {
    /** 例行事項：那天有沒有打卡；一次性待辦：是否已完成。 */
    fun isDone(habit: Habit, date: LocalDate): Boolean {
        val set = done[habit.id].orEmpty()
        return if (habit.isTodo) set.isNotEmpty() else date.toString() in set
    }

    fun completedOn(habit: Habit): LocalDate? = done[habit.id]?.maxOrNull()?.let(LocalDate::parse)

    fun sortedHabits(): List<Habit> = habits.sortedWith(compareBy({ it.section.ordinal }, { it.order }))

    fun routines(): List<Habit> = sortedHabits().filterNot { it.isTodo }

    /**
     * 某一天要顯示的項目：
     * - 當天排定的例行事項
     * - 當天完成的待辦
     * - 還沒完成、日期是那一天的待辦；若是「今天」，也包含沒有日期和已逾期的
     */
    fun itemsFor(date: LocalDate, today: LocalDate = LocalDate.now()): List<Habit> = sortedHabits().filter { h ->
        if (!h.isTodo) {
            h.isScheduled(date)
        } else {
            val completed = completedOn(h)
            when {
                completed != null -> completed == date
                date == today -> h.dueDate == null || !h.dueDate.isAfter(today)
                else -> h.dueDate == date
            }
        }
    }

    /** 日期還沒到的待辦，依日期排序 */
    fun upcomingTodos(today: LocalDate): List<Habit> = habits
        .filter { it.isTodo && it.dueDate != null && it.dueDate.isAfter(today) && completedOn(it) == null }
        .sortedWith(compareBy({ it.dueDate }, { it.order }))

    /** 之前已完成的待辦（今天以前完成） */
    fun finishedTodos(today: LocalDate): List<Habit> = habits
        .filter { h -> h.isTodo && completedOn(h)?.isBefore(today) == true }
        .sortedByDescending { completedOn(it) }
}

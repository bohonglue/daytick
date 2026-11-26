package app.daytick.data

import java.time.LocalDate

private const val MAX_LOOKBACK_DAYS = 3650L

/** 目前連續天數：今天還沒打卡時從昨天開始算，未排定的日子不會中斷連續。 */
fun currentStreak(habit: Habit, done: Set<String>, today: LocalDate): Int {
    if (habit.days.isEmpty()) return 0
    var day = if (today.toString() in done) today else today.minusDays(1)
    var streak = 0
    var steps = 0L
    while (steps++ < MAX_LOOKBACK_DAYS) {
        if (habit.isScheduled(day)) {
            if (day.toString() in done) streak++ else break
        }
        day = day.minusDays(1)
    }
    return streak
}

fun bestStreak(habit: Habit, done: Set<String>, today: LocalDate): Int {
    if (done.isEmpty() || habit.days.isEmpty()) return 0
    var day = done.minOf { LocalDate.parse(it) }
    var run = 0
    var best = 0
    while (!day.isAfter(today)) {
        if (habit.isScheduled(day)) {
            if (day.toString() in done) {
                run++
                if (run > best) best = run
            } else if (day != today) {
                run = 0
            }
        }
        day = day.plusDays(1)
    }
    return best
}

/** 最近 [days] 天（含今天）中排定日子的完成率，0~100。 */
fun completionRate(habit: Habit, done: Set<String>, today: LocalDate, days: Int = 30): Int {
    var scheduled = 0
    var completed = 0
    for (i in 0 until days) {
        val d = today.minusDays(i.toLong())
        if (habit.isScheduled(d)) {
            scheduled++
            if (d.toString() in done) completed++
        }
    }
    return if (scheduled == 0) 0 else completed * 100 / scheduled
}

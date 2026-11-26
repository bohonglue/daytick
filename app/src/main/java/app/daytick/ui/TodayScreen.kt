package app.daytick.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.daytick.data.AppData
import app.daytick.data.Habit
import app.daytick.data.Section
import app.daytick.data.currentStreak
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    data: AppData,
    today: LocalDate,
    modifier: Modifier = Modifier,
    onToggle: (Habit) -> Unit,
    onEdit: (Habit) -> Unit,
) {
    val habits = data.itemsFor(today)
    val doneCount = habits.count { data.isDone(it, today) }
    val upcoming = data.upcomingTodos(today)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.TAIWAN)
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(
                    "${today.monthValue}月${today.dayOfMonth}日 $weekday",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (habits.isNotEmpty() && doneCount == habits.size) "今天全部完成了，太棒了！🎉"
                    else "已完成 $doneCount / ${habits.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (habits.isEmpty()) 0f else doneCount.toFloat() / habits.size },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
            }
        }

        if (habits.isEmpty()) {
            item {
                Text(
                    if (data.habits.isEmpty()) "還沒有任何事項，按右下角的 + 新增一個吧。"
                    else "今天沒有要做的事，好好休息 ☕",
                    modifier = Modifier.padding(vertical = 32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Section.entries.forEach { section ->
            val group = habits.filter { it.section == section }
            if (group.isNotEmpty()) {
                item(key = "header-${section.name}") {
                    Text(
                        "${section.emoji} ${section.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(group, key = { it.id }) { habit ->
                    HabitRow(
                        habit = habit,
                        done = data.isDone(habit, today),
                        subtitle = subtitleFor(habit, data, today),
                        onToggle = { onToggle(habit) },
                        onEdit = { onEdit(habit) },
                    )
                }
            }
        }

        if (upcoming.isNotEmpty()) {
            item(key = "header-upcoming") {
                Text(
                    "📅 之後的待辦（到了那天會自動出現在清單和小工具）",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
                )
            }
            items(upcoming, key = { "up-" + it.id }) { habit ->
                UpcomingRow(habit, today, onClick = { onEdit(habit) })
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun HabitRow(
    habit: Habit,
    done: Boolean,
    subtitle: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val color = Color(habit.color)
    val container by animateColorAsState(
        if (done) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "container",
    )
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) { Text(habit.emoji, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CheckCircle(done = done, color = color, onClick = onToggle)
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "編輯", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CheckCircle(done: Boolean, color: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (done) color else Color.Transparent, label = "check")
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, color, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, contentDescription = "已完成", tint = Color.White)
    }
}

fun subtitleFor(habit: Habit, data: AppData, today: LocalDate): String {
    if (!habit.isTodo) {
        val streak = currentStreak(habit, data.done[habit.id].orEmpty(), today)
        return if (streak > 0) "🔥 連續 $streak 天" else "今天開始累積吧"
    }
    val due = habit.dueDate
    return when {
        data.isDone(habit, today) -> "已完成"
        due == null -> "待辦"
        due.isBefore(today) -> "⚠️ 逾期（${due.monthValue}/${due.dayOfMonth}）"
        else -> "今天"
    }
}

fun formatDate(date: LocalDate, today: LocalDate): String {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.TAIWAN)
    val base = "${date.monthValue}/${date.dayOfMonth}（$weekday）"
    return when (date) {
        today.plusDays(1) -> "明天 $base"
        today.plusDays(2) -> "後天 $base"
        else -> base
    }
}

@Composable
private fun UpcomingRow(habit: Habit, today: LocalDate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(habit.emoji, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(habit.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                habit.dueDate?.let { formatDate(it, today) } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

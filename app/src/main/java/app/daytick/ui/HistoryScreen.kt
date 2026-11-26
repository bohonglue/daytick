package app.daytick.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.daytick.data.AppData
import app.daytick.data.Habit
import app.daytick.data.bestStreak
import app.daytick.data.completionRate
import app.daytick.data.currentStreak
import java.time.DayOfWeek
import java.time.LocalDate

private const val WEEKS = 12

@Composable
fun HistoryScreen(
    data: AppData,
    today: LocalDate,
    modifier: Modifier = Modifier,
    onToggleDay: (Habit, LocalDate) -> Unit,
    onEdit: (Habit) -> Unit,
    onClearFinished: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("打卡紀錄", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "每週重複事項的最近 $WEEKS 週。忘記打卡？點一下格子就能補打卡或取消。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(data.routines(), key = { it.id }) { habit ->
            HabitHistoryCard(habit, data.done[habit.id].orEmpty(), today, onToggleDay, onEdit)
        }
        val finished = data.finishedTodos(today)
        if (finished.isNotEmpty()) {
            item(key = "finished-header") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "已完成的待辦",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearFinished) { Text("全部清除") }
                }
            }
            items(finished, key = { "done-" + it.id }) { habit ->
                val doneOn = data.completedOn(habit)
                Row(
                    Modifier.fillMaxWidth().clickable { onToggleDay(habit, today) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✔️ ${habit.name}", modifier = Modifier.weight(1f), textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(doneOn?.let { "${it.monthValue}/${it.dayOfMonth}" } ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Text("點一下可以改回未完成。", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun HabitHistoryCard(
    habit: Habit,
    done: Set<String>,
    today: LocalDate,
    onToggleDay: (Habit, LocalDate) -> Unit,
    onEdit: (Habit) -> Unit,
) {
    val color = Color(habit.color)
    Card(shape = RoundedCornerShape(16.dp), onClick = { onEdit(habit) }) {
        Column(Modifier.padding(14.dp)) {
            Text("${habit.emoji}  ${habit.name}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("目前連續", "${currentStreak(habit, done, today)} 天", Modifier.weight(1f))
                Stat("最佳紀錄", "${bestStreak(habit, done, today)} 天", Modifier.weight(1f))
                Stat("30 天完成率", "${completionRate(habit, done, today)}%", Modifier.weight(1f))
                Stat("總次數", "${done.size}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Heatmap(habit, done, today, color) { onToggleDay(habit, it) }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Heatmap(habit: Habit, done: Set<String>, today: LocalDate, color: Color, onTap: (LocalDate) -> Unit) {
    val start = today.with(DayOfWeek.MONDAY).minusWeeks((WEEKS - 1).toLong())
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            labels.forEach { l ->
                Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                    Text(l, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        for (w in 0 until WEEKS) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (dow in 0 until 7) {
                    val day = start.plusDays((w * 7 + dow).toLong())
                    val future = day.isAfter(today)
                    val isDone = day.toString() in done
                    val cellColor = when {
                        future -> Color.Transparent
                        isDone -> color
                        habit.isScheduled(day) -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(cellColor)
                            .then(if (future) Modifier else Modifier.clickable { onTap(day) }),
                    ) {
                        if (day == today) {
                            Box(
                                Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

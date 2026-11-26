package app.daytick.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.daytick.data.Habit
import app.daytick.data.HabitStore
import app.daytick.data.Kind
import app.daytick.data.Section
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val Palette = listOf(
    0xFF4CAF50, 0xFF29B6F6, 0xFF7E57C2, 0xFFFF7043,
    0xFFEC407A, 0xFFFFCA28, 0xFF26A69A, 0xFF8D6E63,
)
private val QuickEmoji = listOf("✅", "💧", "🏃", "📖", "🧘", "💊", "🦷", "🛏️", "🥗", "✍️", "🎸", "🧹")
private val WeekLabels = listOf("一", "二", "三", "四", "五", "六", "日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorDialog(
    initial: Habit?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
    onMove: (Habit, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "✅") }
    var color by remember { mutableStateOf(initial?.color ?: Palette.first()) }
    var section by remember { mutableStateOf(initial?.section ?: Section.ANYTIME) }
    var days by remember { mutableStateOf(initial?.days ?: Habit.ALL_DAYS) }
    var kind by remember { mutableStateOf(initial?.kind ?: Kind.TODO) }
    var dueDate by remember { mutableStateOf(initial?.dueDate) }
    var pickingDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增事項" else "編輯事項") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Kind.entries.forEach { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名稱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                Label("圖示（可直接輸入任何表情符號）")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 8) emoji = it },
                        singleLine = true,
                        modifier = Modifier.size(width = 72.dp, height = 56.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        QuickEmoji.forEach { e ->
                            Text(
                                e,
                                fontSize = 22.sp,
                                modifier = Modifier.clip(CircleShape).clickable { emoji = e }.size(38.dp)
                                    .wrapContentSize(Alignment.Center),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Label("顏色")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Palette.forEach { c ->
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(Color(c))
                                .clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (c == color) Icon(Icons.Filled.Check, null, tint = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Label("時段")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Section.entries.forEach { s ->
                        FilterChip(
                            selected = section == s,
                            onClick = { section = s },
                            label = { Text("${s.emoji} ${s.label}") },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (kind == Kind.TODO) {
                    Label("日期（到了那天才會出現在清單和小工具）")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(selected = dueDate == null || dueDate == today, onClick = { dueDate = null }, label = { Text("今天") })
                        FilterChip(selected = dueDate == today.plusDays(1), onClick = { dueDate = today.plusDays(1) }, label = { Text("明天") })
                        val custom = dueDate?.takeIf { it != today && it != today.plusDays(1) }
                        FilterChip(
                            selected = custom != null,
                            onClick = { pickingDate = true },
                            label = { Text(custom?.let { formatDate(it, today) } ?: "選日期…") },
                        )
                    }
                } else {
                Label("重複（星期）")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WeekLabels.forEachIndexed { i, label ->
                        val day = i + 1
                        val on = day in days
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { days = if (on) days - day else days + day },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                if (days.isEmpty()) {
                    Text("至少選一天", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                }

                if (initial != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("排序", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = { onMove(initial, true) }) {
                            Icon(Icons.Filled.KeyboardArrowUp, null); Text("上移")
                        }
                        TextButton(onClick = { onMove(initial, false) }) {
                            Icon(Icons.Filled.KeyboardArrowDown, null); Text("下移")
                        }
                    }
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("刪除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (kind == Kind.TODO || days.isNotEmpty()),
                onClick = {
                    val base = initial ?: Habit(id = HabitStore.newId(), name = "")
                    onSave(
                        base.copy(
                            name = name.trim(),
                            emoji = emoji.ifBlank { "✅" },
                            color = color,
                            section = section,
                            days = days,
                            kind = kind,
                            dueDate = if (kind == Kind.TODO) dueDate?.takeIf { it.isAfter(today) } else null,
                        ),
                    )
                },
            ) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    if (pickingDate) {
        val zone = ZoneOffset.UTC
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (dueDate ?: today.plusDays(1)).atStartOfDay(zone).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate().isBefore(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dueDate = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                    }
                    pickingDate = false
                }) { Text("確定") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("取消") } },
        ) {
            DatePicker(state = state)
        }
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("刪除「${initial.name}」？") },
            text = { Text("所有打卡紀錄也會一起刪除，無法復原。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(initial) }) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.height(24.dp))
}

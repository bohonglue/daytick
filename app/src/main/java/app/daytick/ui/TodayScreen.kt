package app.daytick.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.daytick.data.AppData
import app.daytick.data.Habit
import app.daytick.data.currentStreak
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val CENTER_PAGE = 5000
private val WeekNames = listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")

/** 首頁的動作，集中在一起方便傳遞 */
class TodayActions(
    val onToggle: (Habit, LocalDate) -> Unit,
    val onQuickAdd: (String, LocalDate) -> Unit,
    val onEdit: (Habit) -> Unit,
    val onDelete: (Habit) -> Unit,
    val onReschedule: (Habit, LocalDate) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    data: AppData,
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    actions: TodayActions,
    topBarActions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thisMonday = today.with(DayOfWeek.MONDAY)
    fun pageOf(date: LocalDate) =
        CENTER_PAGE + ChronoUnit.WEEKS.between(thisMonday, date.with(DayOfWeek.MONDAY)).toInt()

    val pager = rememberPagerState(initialPage = pageOf(selected)) { CENTER_PAGE * 2 }
    var showCalendar by remember { mutableStateOf(false) }

    // 日期從別處改變（月曆、今天按鈕）時，週列跟著捲過去
    LaunchedEffect(selected, today) {
        val target = pageOf(selected)
        if (pager.currentPage != target) pager.animateScrollToPage(target)
    }
    // 左右滑動週列時，選取的日期換到新的那一週的同一個星期幾
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(pager, thisMonday) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val weekStart = thisMonday.plusWeeks((page - CENTER_PAGE).toLong())
            val sel = currentSelected
            if (sel.with(DayOfWeek.MONDAY) != weekStart) {
                currentOnSelect(weekStart.plusDays((sel.dayOfWeek.value - 1).toLong()))
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        // ── 標題列：年月（點了開月曆）＋「今天」 ──
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { showCalendar = true }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${selected.year}年${selected.monthValue}月",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "選擇日期")
            }
            Spacer(Modifier.weight(1f))
            if (selected != today) {
                TextButton(onClick = { onSelect(today) }) {
                    Icon(Icons.Filled.KeyboardArrowLeft, null, modifier = Modifier.size(18.dp))
                    Text("今天")
                }
            }
            topBarActions()
        }

        // ── 週列（可左右滑動） ──
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()) { page ->
            val weekStart = thisMonday.plusWeeks((page - CENTER_PAGE).toLong())
            WeekRow(weekStart, selected, today, data, onSelect)
        }

        // ── 待辦卡片 ──
        DayCard(
            data = data,
            date = selected,
            today = today,
            actions = actions,
            onPickDateForMove = { habit -> pendingMove = habit; showCalendar = true },
            modifier = Modifier.weight(1f, fill = false),
        )
    }

    if (showCalendar) {
        CalendarDialog(
            initial = selected,
            onDismiss = { showCalendar = false; pendingMove = null },
            onPick = { date ->
                showCalendar = false
                val moving = pendingMove
                if (moving != null) {
                    actions.onReschedule(moving, date)
                    pendingMove = null
                } else {
                    onSelect(date)
                }
            },
        )
    }
}

/** 要改日期的待辦（從 ⋮ 選單「改到其他日期」來的） */
private var pendingMove by mutableStateOf<Habit?>(null)

@Composable
private fun WeekRow(
    weekStart: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    data: AppData,
    onSelect: (LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        for (i in 0 until 7) {
            val day = weekStart.plusDays(i.toLong())
            val isSelected = day == selected
            val isToday = day == today
            val items = data.itemsFor(day, today)
            val allDone = items.isNotEmpty() && items.all { data.isDone(it, day) }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onSelect(day) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    WeekNames[i],
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${day.dayOfMonth}",
                    fontSize = 16.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isToday -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                // 小圓點：那天有事項（全部完成時是實心）
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                items.isEmpty() -> Color.Transparent
                                allDone -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun DayCard(
    data: AppData,
    date: LocalDate,
    today: LocalDate,
    actions: TodayActions,
    onPickDateForMove: (Habit) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 未完成在上、完成的在下
    val items = data.itemsFor(date, today).sortedBy { data.isDone(it, date) }
    val doneCount = items.count { data.isDone(it, date) }
    var expanded by rememberSaveable { mutableStateOf(true) }
    var adding by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (date == today) "✅ 今日待辦" else "📅 ${date.monthValue}/${date.dayOfMonth}（${weekdayShort(date)}）待辦",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(6.dp))
            Text("$doneCount/${items.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "收合" else "展開",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(expanded) {
            LazyColumn(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                item(key = "add") {
                    QuickAddRow(
                        adding = adding,
                        onStart = { adding = true },
                        onAdd = { actions.onQuickAdd(it, date) },
                        onClose = { adding = false },
                    )
                }
                if (items.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            if (date == today) "今天沒有待辦，好好休息 ☕" else "這天還沒有待辦",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                        )
                    }
                }
                items(items, key = { it.id }) { habit ->
                    ItemRow(
                        habit = habit,
                        done = data.isDone(habit, date),
                        note = noteFor(habit, data, date, today),
                        actions = actions,
                        date = date,
                        today = today,
                        onPickDateForMove = onPickDateForMove,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddRow(adding: Boolean, onStart: () -> Unit, onAdd: (String) -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !adding, onClick = onStart).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(24.dp).border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
            if (adding) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("輸入待辦，按完成鍵新增") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (text.isBlank()) onClose() else { onAdd(text); text = "" }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f).focusRequester(focus),
                )
                TextButton(onClick = { if (text.isNotBlank()) onAdd(text); text = ""; onClose() }) { Text("完成") }
                LaunchedEffect(Unit) { focus.requestFocus() }
            } else {
                Text(
                    "＋ 新增待辦",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                )
            }
        }
        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun ItemRow(
    habit: Habit,
    done: Boolean,
    note: String?,
    actions: TodayActions,
    date: LocalDate,
    today: LocalDate,
    onPickDateForMove: (Habit) -> Unit,
) {
    val color = Color(habit.color)
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable { actions.onToggle(habit, date) }.padding(start = 16.dp, end = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckCircle(done, color) { actions.onToggle(habit, date) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                if (note != null) {
                    Text(
                        note,
                        fontSize = 12.sp,
                        color = if (note.startsWith("⚠")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("編輯") }, onClick = { menu = false; actions.onEdit(habit) })
                    if (habit.isTodo && !done) {
                        val tomorrow = (if (date.isBefore(today)) today else date).plusDays(1)
                        DropdownMenuItem(
                            text = { Text("改到${if (tomorrow == today.plusDays(1)) "明天" else "隔天"}") },
                            onClick = { menu = false; actions.onReschedule(habit, tomorrow) },
                        )
                        DropdownMenuItem(
                            text = { Text("改到其他日期…") },
                            onClick = { menu = false; onPickDateForMove(habit) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("刪除", color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; actions.onDelete(habit) },
                    )
                }
            }
        }
        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun CheckCircle(done: Boolean, color: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (done) color else Color.Transparent, label = "check")
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, color, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, contentDescription = "已完成", tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val zone = ZoneOffset.UTC
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(zone).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) } ?: onDismiss()
            }) { Text("確定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    ) {
        DatePicker(state = state, title = { Text("選擇日期", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

private fun weekdayShort(date: LocalDate) = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.TAIWAN)

/** 項目下方的小字說明，沒有就回傳 null */
private fun noteFor(habit: Habit, data: AppData, date: LocalDate, today: LocalDate): String? {
    if (!habit.isTodo) {
        val streak = currentStreak(habit, data.done[habit.id].orEmpty(), today)
        return if (streak > 0) "🔁 每週重複 · 🔥 連續 $streak 天" else "🔁 每週重複"
    }
    val due = habit.dueDate
    return when {
        data.isDone(habit, date) -> null
        due != null && due.isBefore(date) -> "⚠️ 逾期（原本 ${due.monthValue}/${due.dayOfMonth}）"
        else -> null
    }
}

fun formatDate(date: LocalDate, today: LocalDate): String {
    val base = "${date.monthValue}/${date.dayOfMonth}（${weekdayShort(date)}）"
    return when (date) {
        today -> "今天 $base"
        today.plusDays(1) -> "明天 $base"
        today.plusDays(2) -> "後天 $base"
        else -> base
    }
}

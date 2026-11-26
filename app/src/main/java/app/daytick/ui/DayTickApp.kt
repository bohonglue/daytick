package app.daytick.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.daytick.data.Habit
import app.daytick.data.HabitStore
import app.daytick.update.Updater
import java.time.LocalDate

@Composable
fun DayTickApp(addRequests: Int = 0) {
    val context = LocalContext.current
    val store = remember { HabitStore.get(context) }
    val data by store.data.collectAsState()
    val today by store.today.collectAsState()

    LifecycleResumeEffect(Unit) {
        store.refreshDate()
        onPauseOrDispose { }
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDate = LocalDate.parse(selected)
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var deleting by remember { mutableStateOf<Habit?>(null) }
    // 啟動時自動檢查一次（沒有新版就不會打擾）
    var updateDialog by rememberSaveable { mutableStateOf<String?>("silent") }

    // 換日時，如果原本看的是「今天」，就自動跟著換到新的今天
    var lastToday by remember { mutableStateOf(today) }
    LaunchedEffect(today) {
        if (selectedDate == lastToday) selected = today.toString()
        lastToday = today
    }

    // 從桌面小工具的 ＋ 進來時，直接打開「新增待辦」
    LaunchedEffect(addRequests) {
        if (addRequests > 0) {
            tab = 0
            editing = null
            editorOpen = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text("待辦") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("紀錄") },
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; editorOpen = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新增")
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            0 -> TodayScreen(
                data = data,
                today = today,
                selected = selectedDate,
                onSelect = { selected = it.toString() },
                actions = TodayActions(
                    onToggle = { habit, date -> store.toggle(habit.id, date) },
                    onQuickAdd = { text, date -> store.quickAdd(text, date, today) },
                    onEdit = { editing = it; editorOpen = true },
                    onDelete = { deleting = it },
                    onReschedule = { habit, date -> store.reschedule(habit.id, date, today) },
                ),
                topBarActions = { SettingsMenu(onCheckUpdate = { updateDialog = "manual" }) },
                modifier = modifier,
            )
            else -> HistoryScreen(
                data = data,
                today = today,
                modifier = modifier,
                onToggleDay = { habit, day -> store.toggle(habit.id, day) },
                onEdit = { editing = it; editorOpen = true },
                onClearFinished = { store.clearFinishedTodos(today) },
            )
        }
    }

    if (editorOpen) {
        HabitEditorDialog(
            initial = editing,
            today = today,
            defaultDate = if (tab == 0) selectedDate else today,
            onDismiss = { editorOpen = false },
            onSave = { store.upsert(it); editorOpen = false },
            onDelete = { store.delete(it.id); editorOpen = false },
            onMove = { habit, up -> store.move(habit.id, up) },
        )
    }

    deleting?.let { habit ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("刪除「${habit.name}」？") },
            text = { Text("刪除後無法復原。") },
            confirmButton = {
                TextButton(onClick = { store.delete(habit.id); deleting = null }) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }

    updateDialog?.let { mode ->
        UpdateDialog(silent = mode == "silent", onDismiss = { updateDialog = null })
    }
}

@Composable
private fun SettingsMenu(onCheckUpdate: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.Settings, contentDescription = "設定")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("檢查更新") },
                onClick = { open = false; onCheckUpdate() },
            )
            DropdownMenuItem(
                text = { Text("版本 v${Updater.currentVersionName}") },
                onClick = { open = false },
                enabled = false,
            )
        }
    }
}

package app.daytick.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }

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
                    label = { Text("今天") },
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
                modifier = modifier,
                onToggle = { store.toggle(it.id, today) },
                onEdit = { editing = it; editorOpen = true },
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
            onDismiss = { editorOpen = false },
            onSave = { store.upsert(it); editorOpen = false },
            onDelete = { store.delete(it.id); editorOpen = false },
            onMove = { habit, up -> store.move(habit.id, up) },
        )
    }
}

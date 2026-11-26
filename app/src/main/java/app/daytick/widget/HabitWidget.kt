package app.daytick.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import app.daytick.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import app.daytick.MainActivity
import app.daytick.data.AppData
import app.daytick.data.Habit
import app.daytick.data.HabitStore
import java.time.LocalDate

class HabitWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = HabitStore.get(context)
        store.refreshDate()
        provideContent {
            val data by store.data.collectAsState()
            val today by store.today.collectAsState()
            GlanceTheme {
                WidgetContent(data, today)
            }
        }
    }
}

@Composable
private fun WidgetContent(data: AppData, today: LocalDate) {
    val context = LocalContext.current
    // 未完成的排前面，完成的劃掉排在後面
    val items = data.itemsFor(today, today).sortedBy { data.isDone(it, today) }
    val doneCount = items.count { data.isDone(it, today) }
    val addIntent = Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_ADD, true)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "待辦 ",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GlanceTheme.colors.onSurface),
                )
                Text(
                    text = "$doneCount/${items.size}",
                    style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
            // ＋：打開 App 的新增視窗
            Box(
                modifier = GlanceModifier.size(44.dp).cornerRadius(22.dp).clickable(actionStartActivity(addIntent)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = "新增",
                    modifier = GlanceModifier.size(26.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                )
            }
        }

        if (items.isEmpty()) {
            Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "今天沒有待辦 🎉",
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        } else {
            LazyColumn(GlanceModifier.fillMaxSize().padding(end = 8.dp)) {
                items(items, itemId = { it.id.hashCode().toLong() }) { item ->
                    WidgetItemRow(item, data.isDone(item, today), today)
                }
            }
        }
    }
}

@Composable
private fun WidgetItemRow(item: Habit, done: Boolean, today: LocalDate) {
    val accent = ColorProvider(Color(item.color))
    val overdue = item.isTodo && !done && item.dueDate?.isBefore(today) == true
    Column(GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .clickable(actionRunCallback<ToggleHabitAction>(actionParametersOf(HabitIdKey to item.id))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(GlanceModifier.size(26.dp), contentAlignment = Alignment.Center) {
                if (done) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_check_dot),
                        contentDescription = null,
                        modifier = GlanceModifier.size(26.dp),
                        colorFilter = ColorFilter.tint(accent),
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_check_mark),
                        contentDescription = "已完成",
                        modifier = GlanceModifier.size(26.dp),
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_check_ring),
                        contentDescription = "未完成",
                        modifier = GlanceModifier.size(26.dp),
                        colorFilter = ColorFilter.tint(accent),
                    )
                }
            }
            Spacer(GlanceModifier.width(12.dp))
            Text(
                text = item.name,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 16.sp,
                    color = if (done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (overdue) {
                Text(
                    text = "${item.dueDate!!.monthValue}/${item.dueDate.dayOfMonth}",
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.error),
                )
            }
        }
        // 分隔線
        Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(GlanceTheme.colors.outline))
    }
}

val HabitIdKey = ActionParameters.Key<String>("habit_id")

/** 在桌面小工具上點一下就切換打卡狀態，不必打開 App。 */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HabitIdKey] ?: return
        val store = HabitStore.get(context)
        store.refreshDate()
        store.toggle(habitId, LocalDate.now())
        HabitWidget().updateAll(context)
    }
}

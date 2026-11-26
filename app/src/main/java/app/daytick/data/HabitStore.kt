package app.daytick.data

import android.content.Context
import app.daytick.widget.WidgetSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * 所有資料都存在手機本機（SharedPreferences 裡的 JSON），App 與桌面小工具共用同一份。
 */
class HabitStore private constructor(private val app: Context) {

    private val prefs = app.getSharedPreferences("daytick", Context.MODE_PRIVATE)

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today

    /** 跨日後呼叫，讓畫面與小工具換到新的一天。 */
    fun refreshDate() {
        _today.value = LocalDate.now()
    }

    /** 例行事項：切換某天的打卡；一次性待辦：切換完成 / 未完成（完成日記為 [date]）。 */
    fun toggle(habitId: String, date: LocalDate) = update { data ->
        val habit = data.habits.firstOrNull { it.id == habitId } ?: return@update data
        val key = date.toString()
        val set = data.done[habitId].orEmpty()
        val newSet = when {
            habit.isTodo -> if (set.isEmpty()) setOf(key) else emptySet()
            key in set -> set - key
            else -> set + key
        }
        data.copy(done = data.done + (habitId to newSet))
    }

    /** 快速新增一個待辦到指定日期 */
    fun quickAdd(name: String, date: LocalDate, today: LocalDate) {
        val text = name.trim()
        if (text.isEmpty()) return
        upsert(
            Habit(
                id = newId(),
                name = text,
                emoji = "📝",
                color = 0xFFFF7043,
                kind = Kind.TODO,
                dueDate = date.takeIf { it != today },
            ),
        )
    }

    /** 把待辦改到另一天 */
    fun reschedule(habitId: String, date: LocalDate, today: LocalDate) = update { data ->
        data.copy(habits = data.habits.map { if (it.id == habitId) it.copy(dueDate = date.takeIf { d -> d != today }) else it })
    }

    /** 刪掉所有今天以前就完成的待辦 */
    fun clearFinishedTodos(today: LocalDate) = update { data ->
        val ids = data.finishedTodos(today).map { it.id }.toSet()
        data.copy(habits = data.habits.filterNot { it.id in ids }, done = data.done - ids)
    }

    fun upsert(habit: Habit) = update { data ->
        val exists = data.habits.any { it.id == habit.id }
        val habits = if (exists) {
            data.habits.map { if (it.id == habit.id) habit else it }
        } else {
            val nextOrder = (data.habits.maxOfOrNull { it.order } ?: 0) + 1
            data.habits + habit.copy(order = nextOrder)
        }
        data.copy(habits = habits)
    }

    fun delete(habitId: String) = update { data ->
        data.copy(habits = data.habits.filterNot { it.id == habitId }, done = data.done - habitId)
    }

    /** 在同一個時段內上下移動。 */
    fun move(habitId: String, up: Boolean) = update { data ->
        val habit = data.habits.firstOrNull { it.id == habitId } ?: return@update data
        val group = data.sortedHabits().filter { it.section == habit.section }
        val index = group.indexOfFirst { it.id == habitId }
        val target = if (up) index - 1 else index + 1
        if (target !in group.indices) return@update data
        val reordered = group.toMutableList().apply { add(target, removeAt(index)) }
        val newOrder = reordered.mapIndexed { i, h -> h.id to i }.toMap()
        data.copy(habits = data.habits.map { h -> newOrder[h.id]?.let { h.copy(order = it) } ?: h })
    }

    @Synchronized
    private fun update(transform: (AppData) -> AppData) {
        val newData = transform(_data.value)
        if (newData == _data.value) return
        _data.value = newData
        save(newData)
        WidgetSync.refresh(app)
    }

    private fun load(): AppData {
        val raw = prefs.getString(KEY_DATA, null)
        if (raw == null) {
            val seeded = AppData(habits = sampleHabits())
            save(seeded)
            return seeded
        }
        return runCatching { decode(raw) }.getOrElse { AppData() }
    }

    private fun save(data: AppData) {
        prefs.edit().putString(KEY_DATA, encode(data)).commit()
    }

    companion object {
        private const val KEY_DATA = "data_v1"

        @Volatile
        private var instance: HabitStore? = null

        fun get(context: Context): HabitStore =
            instance ?: synchronized(this) {
                instance ?: HabitStore(context.applicationContext).also { instance = it }
            }

        fun newId(): String = UUID.randomUUID().toString()

        private fun sampleHabits() = listOf(
            Habit(newId(), "起床喝一杯水", "💧", 0xFF29B6F6, Section.MORNING, order = 0),
            Habit(newId(), "運動 30 分鐘", "🏃", 0xFFFF7043, Section.ANYTIME, order = 1),
            Habit(newId(), "睡前閱讀", "📖", 0xFF7E57C2, Section.EVENING, order = 2),
            Habit(newId(), "試試看在桌面小工具上打勾", "📝", 0xFFFF7043, Section.ANYTIME, order = 3, kind = Kind.TODO),
        )

        internal fun encode(data: AppData): String {
            val habits = JSONArray()
            data.habits.forEach { h ->
                habits.put(
                    JSONObject()
                        .put("id", h.id)
                        .put("name", h.name)
                        .put("emoji", h.emoji)
                        .put("color", h.color)
                        .put("section", h.section.name)
                        .put("days", JSONArray(h.days.sorted()))
                        .put("order", h.order)
                        .put("kind", h.kind.name)
                        .put("due", h.dueDate?.toString() ?: JSONObject.NULL)
                )
            }
            val done = JSONObject()
            data.done.forEach { (id, dates) -> done.put(id, JSONArray(dates.sorted())) }
            return JSONObject().put("habits", habits).put("done", done).toString()
        }

        internal fun decode(raw: String): AppData {
            val root = JSONObject(raw)
            val arr = root.optJSONArray("habits") ?: JSONArray()
            val habits = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val daysArr = o.optJSONArray("days")
                Habit(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    emoji = o.optString("emoji", "✅"),
                    color = o.optLong("color", 0xFF4CAF50),
                    section = runCatching { Section.valueOf(o.optString("section")) }.getOrDefault(Section.ANYTIME),
                    days = daysArr?.let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() } ?: Habit.ALL_DAYS,
                    order = o.optInt("order", i),
                    kind = runCatching { Kind.valueOf(o.optString("kind")) }.getOrDefault(Kind.ROUTINE),
                    dueDate = if (o.isNull("due")) null else runCatching { LocalDate.parse(o.getString("due")) }.getOrNull(),
                )
            }
            val doneObj = root.optJSONObject("done") ?: JSONObject()
            val done = doneObj.keys().asSequence().associateWith { id ->
                val a = doneObj.getJSONArray(id)
                (0 until a.length()).map { a.getString(it) }.toSet()
            }
            return AppData(habits, done)
        }
    }
}

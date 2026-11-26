package app.daytick

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.daytick.ui.DayTickApp
import app.daytick.ui.DayTickTheme
import app.daytick.widget.WidgetSync

class MainActivity : ComponentActivity() {

    /** 每次從小工具的 ＋ 進來就 +1，讓畫面打開「新增」視窗 */
    private var addRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WidgetSync.scheduleMidnight(this)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            DayTickTheme {
                DayTickApp(addRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ADD, false) == true) addRequests++
    }

    companion object {
        const val EXTRA_ADD = "app.daytick.extra.ADD"
    }
}

package app.daytick.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.daytick.update.ReleaseInfo
import app.daytick.update.Updater
import kotlinx.coroutines.launch
import java.io.File

private sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: ReleaseInfo) : UpdateState
    data class Downloading(val info: ReleaseInfo, val progress: Float) : UpdateState
    data class Ready(val info: ReleaseInfo, val apk: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * 檢查更新視窗。
 * [silent] = true 時用於 App 啟動自動檢查：沒有新版或失敗就不顯示任何東西。
 */
@Composable
fun UpdateDialog(silent: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }

    LaunchedEffect(Unit) {
        state = try {
            val info = Updater.fetchLatest()
            if (info.versionCode > Updater.currentVersionCode) UpdateState.Available(info) else UpdateState.UpToDate
        } catch (e: Exception) {
            UpdateState.Failed(e.message ?: "無法連線")
        }
    }

    val s = state
    if (silent && (s is UpdateState.Checking || s is UpdateState.UpToDate || s is UpdateState.Failed)) {
        if (s !is UpdateState.Checking) LaunchedEffect(s) { onDismiss() }
        return
    }

    fun startDownload(info: ReleaseInfo) {
        state = UpdateState.Downloading(info, 0f)
        scope.launch {
            state = try {
                val apk = Updater.download(context, info.apkUrl) { p -> state = UpdateState.Downloading(info, p) }
                UpdateState.Ready(info, apk)
            } catch (e: Exception) {
                UpdateState.Failed("下載失敗：${e.message}")
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (s !is UpdateState.Downloading) onDismiss() },
        title = {
            Text(
                when (s) {
                    is UpdateState.Available, is UpdateState.Downloading, is UpdateState.Ready -> "有新版本"
                    UpdateState.UpToDate -> "已經是最新版"
                    is UpdateState.Failed -> "檢查更新失敗"
                    UpdateState.Checking -> "檢查更新中…"
                },
            )
        },
        text = {
            Column {
                Text(
                    "目前版本：v${Updater.currentVersionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                when (s) {
                    UpdateState.Checking -> CircularProgressIndicator()
                    UpdateState.UpToDate -> Text("你用的就是最新版本 👍")
                    is UpdateState.Failed -> Text(s.message)
                    is UpdateState.Available -> ReleaseNotes(s.info)
                    is UpdateState.Downloading -> {
                        ReleaseNotes(s.info)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                        Text("下載中… ${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    }
                    is UpdateState.Ready -> {
                        ReleaseNotes(s.info)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (Updater.canInstall(context)) "下載完成，按「安裝」更新。你的待辦資料會保留。"
                            else "第一次更新需要允許「日日待辦」安裝應用程式：按「前往設定」打開開關，回來後再按「安裝」。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (s) {
                is UpdateState.Available -> TextButton(onClick = { startDownload(s.info) }) { Text("下載並更新") }
                is UpdateState.Ready -> TextButton(onClick = {
                    if (Updater.canInstall(context)) Updater.install(context, s.apk)
                    else Updater.openInstallPermissionSettings(context)
                }) { Text(if (Updater.canInstall(context)) "安裝" else "前往設定") }
                is UpdateState.Downloading -> {}
                else -> TextButton(onClick = onDismiss) { Text("好") }
            }
        },
        dismissButton = {
            if (s is UpdateState.Available || s is UpdateState.Ready) {
                TextButton(onClick = onDismiss) { Text("稍後") }
            }
        },
    )
}

@Composable
private fun ReleaseNotes(info: ReleaseInfo) {
    Text("新版本：v${info.versionName}", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
        Text(info.notes.ifBlank { "（沒有更新說明）" }, style = MaterialTheme.typography.bodyMedium)
    }
}

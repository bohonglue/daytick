package app.daytick.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import app.daytick.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val apkUrl: String,
)

/**
 * App 內更新：只會連到這個專案的 GitHub Releases 查詢與下載新版。
 * 不會上傳任何資料。Android 會檢查新版的簽章，只有用同一把金鑰簽的 APK 才能覆蓋安裝。
 */
object Updater {

    private const val API = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest"

    val currentVersionCode: Int get() = BuildConfig.VERSION_CODE
    val currentVersionName: String get() = BuildConfig.VERSION_NAME

    suspend fun fetchLatest(): ReleaseInfo = withContext(Dispatchers.IO) {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (conn.responseCode != 200) error("GitHub 回應 ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name") // 例如 v1.12
            val assets = json.getJSONArray("assets")
            val apkUrl = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") }
                ?.getString("browser_download_url")
                ?: error("這個版本沒有 APK")
            require(apkUrl.startsWith("https://github.com/${BuildConfig.UPDATE_REPO}/")) { "下載網址不正確" }
            ReleaseInfo(
                versionCode = tag.substringAfterLast('.').toIntOrNull() ?: 0,
                versionName = tag.removePrefix("v"),
                notes = json.optString("body").trim(),
                apkUrl = apkUrl,
            )
        } finally {
            conn.disconnect()
        }
    }

    suspend fun download(context: Context, url: String, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val file = File(dir, "DayTick.apk")
            var target = URL(url)
            var conn: HttpURLConnection
            // GitHub 會轉址到下載伺服器；只接受 https
            var hops = 0
            while (true) {
                conn = (target.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    instanceFollowRedirects = false
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val next = URL(target, conn.getHeaderField("Location"))
                    conn.disconnect()
                    require(next.protocol == "https") { "不安全的下載網址" }
                    require(++hops <= 5) { "轉址太多次" }
                    target = next
                    continue
                }
                if (code != 200) error("下載失敗（$code）")
                break
            }
            try {
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var read = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            read += n
                            if (total > 0) onProgress(read.toFloat() / total)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            file
        }

    /** 是否已允許本 App 安裝更新 */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** 打開「允許安裝不明應用程式」設定頁（只針對本 App） */
    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

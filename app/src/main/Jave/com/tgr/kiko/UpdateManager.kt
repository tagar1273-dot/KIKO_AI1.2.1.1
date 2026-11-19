package com.tgr.kiko

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.concurrent.thread

class UpdateManager(private val activity: Activity) {
    companion object {
        // Replace OWNER and REPO with the GitHub account/repository where update.json and APK are published.
        const val MANIFEST_URL = "https://raw.githubusercontent.com/OWNER/REPO/main/update.json"
    }

    fun check(onMessage: (String) -> Unit) {
        thread {
            try {
                val manifest = readHttps(MANIFEST_URL)
                val json = JSONObject(manifest)
                val latestCode = json.getInt("versionCode")
                val latestName = json.optString("versionName", "نیا ورژن")
                val apkUrl = json.getString("apkUrl")
                val expectedHash = json.getString("sha256").lowercase()
                val currentCode = activity.packageManager.getPackageInfo(activity.packageName, 0).versionCode

                activity.runOnUiThread {
                    if (latestCode <= currentCode) {
                        onMessage("آپ کے KIKO AI کا تازہ ترین ورژن پہلے سے موجود ہے۔")
                    } else {
                        android.app.AlertDialog.Builder(activity)
                            .setTitle("KIKO AI اپڈیٹ دستیاب ہے")
                            .setMessage("نیا ورژن: $latestName\\nکیا آپ اپڈیٹ ڈاؤن لوڈ کرنا چاہتے ہیں؟")
                            .setPositiveButton("ڈاؤن لوڈ") { _, _ -> downloadAndInstall(apkUrl, expectedHash, onMessage) }
                            .setNegativeButton("بعد میں", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                activity.runOnUiThread { onMessage("اپڈیٹ چیک نہیں ہو سکی: ${e.message ?: "نیٹ ورک خرابی"}") }
            }
        }
    }

    private fun downloadAndInstall(apkUrl: String, expectedHash: String, onMessage: (String) -> Unit) {
        thread {
            try {
                require(apkUrl.startsWith("https://")) { "صرف محفوظ HTTPS لنک قبول ہے۔" }
                val conn = URL(apkUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 20000
                conn.readTimeout = 60000
                conn.instanceFollowRedirects = true
                conn.connect()
                require(conn.responseCode in 200..299) { "APK ڈاؤن لوڈ ناکام (${conn.responseCode})" }

                val apk = File(activity.cacheDir, "kiko-update.apk")
                conn.inputStream.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
                conn.disconnect()

                val actualHash = sha256(apk)
                require(actualHash == expectedHash) { "APK کی SHA-256 تصدیق ناکام؛ انسٹال نہیں کیا گیا۔" }

                activity.runOnUiThread {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            !activity.packageManager.canRequestPackageInstalls()) {
                            onMessage("اپڈیٹ انسٹال کرنے کے لیے KIKO AI کو اسکرین پر اجازت دیں۔")
                            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${activity.packageName}"))
                            activity.startActivity(settingsIntent)
                            return@runOnUiThread
                        }
                        val uri = FileProvider.getUriForFile(
                            activity, "${activity.packageName}.provider", apk
                        )
                        val install = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                            data = uri
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        activity.startActivity(install)
                    } catch (e: Exception) {
                        onMessage("انسٹالر نہیں کھل سکا: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                activity.runOnUiThread { onMessage("اپڈیٹ ڈاؤن لوڈ/تصدیق ناکام: ${e.message}") }
            }
        }
    }

    private fun readHttps(url: String): String {
        require(url.startsWith("https://")) { "اپڈیٹ سرور HTTPS ہونا چاہیے۔" }
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        try {
            require(conn.responseCode in 200..299) { "سرور جواب ${conn.responseCode}" }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

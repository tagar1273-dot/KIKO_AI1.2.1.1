package com.tgr.kiko

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var log: TextView
    private lateinit var input: EditText
    private lateinit var prefs: android.content.SharedPreferences
    private var tts: TextToSpeech? = null
    private val history = JSONArray()
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("kiko_settings", MODE_PRIVATE)
        tts = TextToSpeech(this, this)
        buildUi()
        addMessage("KIKO", "السلام علیکم! میں KIKO AI ہوں۔ اپنا سوال لکھیں یا مائیک دبائیں۔ Gemini API key سیٹنگ میں شامل کریں۔")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 14)
            setBackgroundColor(0xFFF4F6FA.toInt())
        }
        val title = TextView(this).apply {
            text = "KIKO AI"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(0xFF17345C.toInt())
            setPadding(0, 8, 0, 12)
        }
        log = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFF17202A.toInt())
            setPadding(12, 12, 12, 12)
            textDirection = android.view.View.TEXT_DIRECTION_LOCALE
        }
        val scroll = ScrollView(this).apply { addView(log) }
        val settings = Button(this).apply {
            text = "Gemini API Key سیٹ کریں"
            setOnClickListener { askApiKey() }
        }
        val update = Button(this).apply {
            text = "KIKO AI اپڈیٹ چیک کریں"
            setOnClickListener {
                UpdateManager(this@MainActivity).check { message ->
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setMessage(message).setPositiveButton("ٹھیک ہے", null).show()
                }
            }
        }
        input = EditText(this).apply {
            hint = "اپنا پیغام لکھیں..."
            textSize = 16f
            minLines = 1
            maxLines = 4
        }
        val send = Button(this).apply {
            text = "پیغام بھیجیں"
            setOnClickListener { val q = input.text.toString().trim(); if (q.isNotEmpty()) { input.setText(""); sendToGemini(q) } }
        }
        val mic = Button(this).apply {
            text = "🎙 آواز سے پوچھیں"
            setOnClickListener { startVoiceInput() }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(send, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(mic, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(title)
        root.addView(settings)
        root.addView(update)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(input)
        root.addView(row)
        setContentView(root)
    }

    private fun askApiKey() {
        val edit = EditText(this).apply {
            hint = "AIza..."
            setText(prefs.getString("gemini_key", ""))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Gemini API Key")
            .setMessage("اپنی Google AI Studio key درج کریں۔ اسے کسی کے ساتھ شیئر نہ کریں۔")
            .setView(edit)
            .setPositiveButton("محفوظ کریں") { _, _ ->
                prefs.edit().putString("gemini_key", edit.text.toString().trim()).apply()
                addMessage("KIKO", "API key اس فون پر محفوظ کر دی گئی۔")
            }
            .setNegativeButton("منسوخ", null)
            .show()
    }

    private fun sendToGemini(question: String) {
        addMessage("آپ", question)
        val key = prefs.getString("gemini_key", "").orEmpty()
        if (key.isBlank()) {
            addMessage("KIKO", "پہلے اوپر Gemini API Key سیٹ کریں۔")
            return
        }
        addMessage("KIKO", "جواب تیار ہو رہا ہے...")
        thread {
            var answer: String
            try {
                val url = URL("$apiUrl?key=${Uri.encode(key)}")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val prompt = "You are KIKO AI, a helpful assistant. Reply in the same language as the user, preferably Urdu when appropriate. Be accurate and safe. User: $question"
                val body = JSONObject()
                    .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                    .put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 1200))
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val raw = stream.bufferedReader().use { it.readText() }
                if (conn.responseCode !in 200..299) {
                    answer = "Gemini API خرابی (${conn.responseCode}): " + JSONObject(raw).optJSONObject("error")?.optString("message", "درخواست ناکام")
                } else {
                    val candidates = JSONObject(raw).optJSONArray("candidates")
                    answer = candidates?.optJSONObject(0)?.optJSONObject("content")
                        ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                        ?: "جواب موصول نہیں ہوا۔ دوبارہ کوشش کریں۔"
                }
                conn.disconnect()
            } catch (e: Exception) {
                answer = "کنکشن میں مسئلہ ہے: ${e.localizedMessage ?: "نامعلوم خرابی"}"
            }
            runOnUiThread {
                // Remove the temporary "جواب تیار ہو رہا ہے..." line.
                val current = log.text.toString()
                val cleaned = current.replace("KIKO: جواب تیار ہو رہا ہے...\n", "")
                log.text = cleaned
                addMessage("KIKO", answer)
                speak(answer)
            }
        }
    }

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10)
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "KIKO سے بات کریں")
            }
            startActivityForResult(intent, 11)
        } catch (_: Exception) {
            addMessage("KIKO", "اس فون میں speech recognition دستیاب نہیں۔")
        }
    }

    @Deprecated("Deprecated by Android API; retained for broad device support")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 11 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) sendToGemini(spoken)
        }
    }

    private fun addMessage(who: String, message: String) {
        log.append("$who: $message\n\n")
    }

    private fun speak(text: String) {
        tts?.speak(text.take(3000), TextToSpeech.QUEUE_FLUSH, null, "kiko_reply")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ur", "PK"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

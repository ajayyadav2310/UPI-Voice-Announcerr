package com.ajay.upivoice

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.*
import android.speech.tts.TextToSpeech
import java.util.Locale

class SettingsActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("upi_settings", Context.MODE_PRIVATE)
    }

    private val bg = Color.rgb(11, 16, 32)
    private val card = Color.rgb(24, 32, 53)
    private val white = Color.WHITE
    private val muted = Color.rgb(160, 174, 195)

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(25))
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        addText(root, "Settings", 27f, white, true)
        addText(root, "Customize your UPI Manager", 13f, muted)
        space(root, 22)

        addText(root, "VOICE SETTINGS", 13f, muted, true)
        space(root, 10)

        addSwitch(
            root,
            "Voice Announcement",
            "Enable payment voice announcements",
            "voice_enabled",
            true
        )

        addSwitch(
            root,
            "Received Announcements",
            "Speak incoming payment notifications",
            "received_enabled",
            true
        )

        addSwitch(
            root,
            "Sent Announcements",
            "Speak outgoing payment notifications",
            "sent_enabled",
            true
        )

        addSwitch(
            root,
            "Silent Mode",
            "Disable voice during silent hours",
            "silent_mode",
            false
        )

        space(root, 18)
        addText(root, "ANNOUNCEMENT LANGUAGE", 13f, muted, true)

        val languages = listOf(
            "Hindi" to "hi",
            "English" to "en",
            "Hinglish" to "both"
        )

        val languageSpinner = Spinner(this)
        languageSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages.map { it.first }
        )

        val savedLanguage = prefs.getString("language", "both")
        languageSpinner.setSelection(
            languages.indexOfFirst { it.second == savedLanguage }
                .coerceAtLeast(0)
        )

        languageSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    prefs.edit()
                        .putString("language", languages[position].second)
                        .apply()
                }
            }

        root.addView(languageSpinner)
        space(root, 18)

        addText(root, "VOICE SPEED", 13f, muted, true)
        addSlider(root, "voice_speed", 5, 20, 10)

        addText(root, "VOICE PITCH", 13f, muted, true)
        addSlider(root, "voice_pitch", 5, 20, 10)

        space(root, 18)

        val testButton = Button(this).apply {
            text = "Test Voice"
            setOnClickListener { testVoice() }
        }
        root.addView(testButton)

        space(root, 10)

        val accessButton = Button(this).apply {
            text = "Notification Access Settings"
            setOnClickListener {
                startActivity(
                    Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                )
            }
        }
        root.addView(accessButton)

        space(root, 18)

        addText(
            root,
            "Privacy: Your payment records are stored locally on this device.",
            12f,
            muted
        )
    }

    private fun addSwitch(
        root: LinearLayout,
        title: String,
        description: String,
        key: String,
        defaultValue: Boolean
    ) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(card)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val label = TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(white)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val toggle = Switch(this).apply {
            isChecked = prefs.getBoolean(key, defaultValue)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
            }
        }

        row.addView(label)
        row.addView(toggle)

        val detail = TextView(this).apply {
            text = description
            textSize = 11f
            setTextColor(muted)
            setPadding(0, dp(5), 0, 0)
        }

        layout.addView(row)
        layout.addView(detail)

        root.addView(
            layout,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(9)
            }
        )
    }

    private fun addSlider(
        root: LinearLayout,
        key: String,
        min: Int,
        max: Int,
        defaultValue: Int
    ) {
        val slider = SeekBar(this).apply {
            this.max = max - min
            progress = (prefs.getInt(key, defaultValue) - min)
                .coerceIn(0, max - min)

            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        prefs.edit().putInt(key, progress + min).apply()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                }
            )
        }

        root.addView(slider)
        space(root, 12)
    }

    private fun testVoice() {
        val language = prefs.getString("language", "both") ?: "both"

        val locale = when (language) {
            "hi" -> Locale("hi", "IN")
            "en" -> Locale.US
            else -> Locale("en", "IN")
        }

        tts?.shutdown()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = locale
                tts?.setSpeechRate(
                    prefs.getInt("voice_speed", 10) / 10f
                )
                tts?.setPitch(
                    prefs.getInt("voice_pitch", 10) / 10f
                )

                tts?.speak(
                    "UPI Manager voice test successful",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "voice_test"
                )
            }
        }
    }

    private fun addText(
        root: LinearLayout,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        root.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        })
    }

    private fun space(root: LinearLayout, size: Int) {
        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(size))
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}

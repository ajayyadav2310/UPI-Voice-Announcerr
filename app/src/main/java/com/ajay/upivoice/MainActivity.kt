package com.ajay.upivoice

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(245, 248, 255))
        }

        val title = TextView(this).apply {
            text = "UPI Voice Announcer"
            textSize = 27f
            setTextColor(Color.rgb(0, 110, 100))
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "\nअब अपने सभी UPI payment notifications सुनें।"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        }

        val enableButton = Button(this).apply {
            text = "Enable Notification Access"
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                )
            }
        }

        val testButton = Button(this).apply {
            text = "Test Voice Announcement"
            setOnClickListener {
                tts?.speak(
                    "एक सौ रुपये Navi पर प्राप्त हुए",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "test"
                )
            }
        }

        val status = TextView(this).apply {
            text = "\nपहले Notification Access चालू करें।"
            textSize = 15f
            gravity = Gravity.CENTER
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(enableButton)
        layout.addView(testButton)
        layout.addView(status)

        setContentView(layout)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

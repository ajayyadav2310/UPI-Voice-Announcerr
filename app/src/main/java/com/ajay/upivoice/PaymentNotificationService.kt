package com.ajay.upivoice

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.os.Bundle
import android.content.Context
import java.util.Locale
import java.security.MessageDigest

class PaymentNotificationService : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var ready = false

    private val prefs by lazy {
        getSharedPreferences("upi_settings", Context.MODE_PRIVATE)
    }

    private val recent = mutableMapOf<String, Long>()
    private val pending = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()

        tts = TextToSpeech(this) { status ->
            ready = status == TextToSpeech.SUCCESS

            if (ready) {
                configureVoice()

                synchronized(pending) {
                    pending.forEach { speak(it) }
                    pending.clear()
                }
            }
        }
    }

    private fun configureVoice() {
        val language = prefs.getString("language", "both") ?: "both"

        val locale = when (language) {
            "hi" -> Locale("hi", "IN")
            "en" -> Locale.US
            else -> Locale("en", "IN")
        }

        tts?.language = locale
        tts?.setSpeechRate(
            prefs.getInt("voice_speed", 10) / 10f
        )
        tts?.setPitch(
            prefs.getInt("voice_pitch", 10) / 10f
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val extras: Bundle = sbn.notification.extras

        val title = extras.getCharSequence("android.title")
            ?.toString().orEmpty()

        val text = extras.getCharSequence("android.text")
            ?.toString().orEmpty()

        val bigText = extras.getCharSequence("android.bigText")
            ?.toString().orEmpty()

        val message = "$title $text $bigText"
        val lower = message.lowercase(Locale.ROOT)

        val receivedWords = listOf(
            "received", "credited", "got", "added", "credit",
            "प्राप्त", "मिला", "जमा", "आए"
        )

        val sentWords = listOf(
            "sent", "paid", "debited", "transferred",
            "भेजा", "भुगतान किया", "डेबिट"
        )

        val direction = when {
            sentWords.any { lower.contains(it) } -> "SENT"
            receivedWords.any { lower.contains(it) } -> "RECEIVED"
            else -> return
        }

        if (direction == "RECEIVED" &&
            !prefs.getBoolean("received_enabled", true)
        ) return

        if (direction == "SENT" &&
            !prefs.getBoolean("sent_enabled", true)
        ) return

        if (!prefs.getBoolean("voice_enabled", true)) return

        if (prefs.getBoolean("silent_mode", false)) return

        val regex = Regex(
            """(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)|([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:rs\.?|inr|rupees|रुपये)""",
            RegexOption.IGNORE_CASE
        )

        val match = regex.find(message) ?: return

        val amountString = match.groupValues
            .drop(1)
            .firstOrNull { it.isNotBlank() }
            ?.replace(",", "") ?: return

        val amount = amountString.toDoubleOrNull() ?: return
        if (amount <= 0) return

        val now = System.currentTimeMillis()
        val rawKey = "${sbn.packageName}|${sbn.key}|${sbn.postTime}|$amount|$direction"

        val key = MessageDigest.getInstance("SHA-256")
            .digest(rawKey.toByteArray())
            .joinToString("") { "%02x".format(it) }

        synchronized(recent) {
            if (now - (recent[key] ?: 0L) < 10000L) return

            recent[key] = now
            recent.entries.removeAll { now - it.value > 60000L }
        }

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            "UPI"
        }

        val saved = TransactionDb(this).saveTransaction(
            dedupeKey = key,
            amount = amount,
            direction = direction,
            counterparty = "",
            appName = appName,
            packageName = sbn.packageName,
            title = title,
            message = message
        )

        if (!saved) return

        val language = prefs.getString("language", "both")

        val announcement = when (language) {
            "hi" -> if (direction == "RECEIVED")
                "$amountString रुपये प्राप्त हुए"
            else "$amountString रुपये भेजे गए"

            "en" -> if (direction == "RECEIVED")
                "Rupees $amountString received on $appName"
            else "Rupees $amountString sent using $appName"

            else -> if (direction == "RECEIVED")
                "$amountString रुपये $appName पर प्राप्त हुए"
            else "$amountString रुपये $appName से भेजे गए"
        }

        synchronized(pending) {
            if (ready) {
                configureVoice()
                speak(announcement)
            } else {
                pending.add(announcement)
            }
        }
    }

    private fun speak(message: String) {
        tts?.speak(
            message,
            TextToSpeech.QUEUE_ADD,
            null,
            System.currentTimeMillis().toString()
        )
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

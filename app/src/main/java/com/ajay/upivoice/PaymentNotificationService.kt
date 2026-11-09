package com.ajay.upivoice

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.app.Notification
import android.content.Context
import android.os.Bundle
import java.util.Locale
import java.security.MessageDigest

class PaymentNotificationService : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsFailed = false

    private val prefs by lazy {
        getSharedPreferences("upi_settings", Context.MODE_PRIVATE)
    }

    private val recent = mutableMapOf<String, Long>()
    private val pendingSpeech = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()

        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            ttsFailed = !ttsReady

            if (ttsReady) {
                configureVoice()

                synchronized(pendingSpeech) {
                    pendingSpeech.forEach { speak(it) }
                    pendingSpeech.clear()
                }
            }
        }
    }

    private fun configureVoice() {
        val language = prefs.getString("language", "both")

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

        if (sbn.packageName == packageName) return

        val notification = sbn.notification
        val extras: Bundle = notification.extras

        val parts = mutableListOf<String>()

        fun add(value: CharSequence?) {
            if (!value.isNullOrBlank()) {
                parts.add(value.toString())
            }
        }

        add(extras.getCharSequence(Notification.EXTRA_TITLE))
        add(extras.getCharSequence(Notification.EXTRA_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        add(notification.tickerText)

        val lines = extras.getCharSequenceArray(
            Notification.EXTRA_TEXT_LINES
        )

        lines?.forEach { add(it) }

        val message = parts.joinToString(" ")
        if (message.isBlank()) return

        val lower = message.lowercase(Locale.ROOT)

        val sentWords = listOf(
            "sent", "you sent", "paid", "you paid",
            "debited", "debit alert", "money sent",
            "payment successful", "transferred to",
            "payment to", "भेजा", "भुगतान किया",
            "डेबिट", "ट्रांसफर किया"
        )

        val receivedWords = listOf(
            "received", "payment received",
            "you've got", "you have got",
            "credited", "credit alert",
            "money received", "received from",
            "credited to", "has been credited",
            "added to your account",
            "प्राप्त", "मिला", "जमा हुआ",
            "पैसे आए", "राशि प्राप्त"
        )

        val direction = when {
            sentWords.any { lower.contains(it) } -> "SENT"
            receivedWords.any { lower.contains(it) } -> "RECEIVED"
            else -> return
        }

        val amountRegex = Regex(
            """(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)|([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:rs\.?|inr|rupees|रुपये)""",
            RegexOption.IGNORE_CASE
        )

        val match = amountRegex.find(message) ?: return

        val amountText = match.groupValues
            .drop(1)
            .firstOrNull { it.isNotBlank() }
            ?.replace(",", "") ?: return

        val amount = amountText.toDoubleOrNull() ?: return
        if (amount <= 0) return

        val now = System.currentTimeMillis()

        val rawKey =
            "${sbn.packageName}|${sbn.key}|${amount}|${direction}"

        val key = MessageDigest.getInstance("SHA-256")
            .digest(rawKey.toByteArray())
            .joinToString("") { "%02x".format(it) }

        synchronized(recent) {
            if (now - (recent[key] ?: 0L) < 10000L) return

            recent[key] = now
            recent.entries.removeAll {
                now - it.value > 60000L
            }
        }

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            "UPI"
        }

        val counterparty = Regex(
            """\bfrom\s+([A-Za-z][A-Za-z .'-]{1,35})""",
            RegexOption.IGNORE_CASE
        ).find(message)?.groupValues?.get(1)?.trim().orEmpty()

        // Save transaction even if voice is disabled.
        val saved = try {
            TransactionDb(this).saveTransaction(
                dedupeKey = key,
                amount = amount,
                direction = direction,
                counterparty = counterparty,
                appName = appName,
                packageName = sbn.packageName,
                title = extras.getCharSequence(
                    Notification.EXTRA_TITLE
                )?.toString().orEmpty(),
                message = message
            )
        } catch (e: Exception) {
            false
        }

        if (!saved) return

        // Voice preferences are checked only after saving.
        if (!prefs.getBoolean("voice_enabled", true)) return

        if (direction == "RECEIVED" &&
            !prefs.getBoolean("received_enabled", true)
        ) return

        if (direction == "SENT" &&
            !prefs.getBoolean("sent_enabled", true)
        ) return

        if (prefs.getBoolean("silent_mode", false)) return

        val language = prefs.getString("language", "both")

        val speech = when (language) {
            "hi" -> if (direction == "RECEIVED") {
                "$amountText रुपये $appName पर प्राप्त हुए"
            } else {
                "$amountText रुपये $appName से भेजे गए"
            }

            "en" -> if (direction == "RECEIVED") {
                "Rupees $amountText received on $appName"
            } else {
                "Rupees $amountText sent using $appName"
            }

            else -> if (direction == "RECEIVED") {
                "$amountText रुपये $appName पर प्राप्त हुए"
            } else {
                "$amountText रुपये $appName से भेजे गए"
            }
        }

        synchronized(pendingSpeech) {
            if (ttsReady) {
                configureVoice()
                speak(speech)
            } else if (!ttsFailed && pendingSpeech.size < 20) {
                pendingSpeech.add(speech)
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

package com.ajay.upivoice

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.os.Bundle
import java.util.Locale

class PaymentNotificationService : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val recentNotifications = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()

        tts = TextToSpeech(this) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale("hi", "IN")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!ready) return

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
            "received", "credited", "got", "added",
            "प्राप्त", "मिला", "जमा", "आए"
        )

        val sentWords = listOf(
            "sent", "paid", "debited", "payment successful",
            "भेजा", "भुगतान किया"
        )

        if (receivedWords.none { lower.contains(it) }) return
        if (sentWords.any { lower.contains(it) }) return

        val amountRegex = Regex(
            """(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)|([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:rs\.?|inr|rupees|रुपये)""",
            RegexOption.IGNORE_CASE
        )

        val match = amountRegex.find(message) ?: return
        val amount = match.groupValues
            .drop(1)
            .firstOrNull { it.isNotBlank() }
            ?.replace(",", "") ?: return

        val now = System.currentTimeMillis()
        val key = "${sbn.key}:$amount"

        synchronized(recentNotifications) {
            if (now - (recentNotifications[key] ?: 0L) < 10000L) return
            recentNotifications[key] = now
            recentNotifications.entries.removeAll {
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

        tts?.speak(
            "$amount रुपये $appName पर प्राप्त हुए",
            TextToSpeech.QUEUE_ADD,
            null,
            key
        )
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}


package com.ajay.upivoice

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.os.Bundle
import java.util.Locale
import java.security.MessageDigest

class PaymentNotificationService : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var ready = false
    private lateinit var database: TransactionDb

    private val recentNotifications = mutableMapOf<String, Long>()
    private val pendingAnnouncements = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()

        database = TransactionDb(this)

        tts = TextToSpeech(this) { status ->
            ready = status == TextToSpeech.SUCCESS

            if (ready) {
                tts?.language = Locale("hi", "IN")

                synchronized(pendingAnnouncements) {
                    pendingAnnouncements.forEach { speak(it) }
                    pendingAnnouncements.clear()
                }
            }
        }
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
            "you paid", "भेजा", "भुगतान किया", "डेबिट"
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

        val amountString = match.groupValues
            .drop(1)
            .firstOrNull { it.isNotBlank() }
            ?.replace(",", "") ?: return

        val amount = amountString.toDoubleOrNull() ?: return
        if (amount <= 0) return

        val now = System.currentTimeMillis()
        val rawKey = "${sbn.packageName}|${sbn.key}|${sbn.postTime}|$amount|$direction"

        val dedupeKey = MessageDigest.getInstance("SHA-256")
            .digest(rawKey.toByteArray())
            .joinToString("") { "%02x".format(it) }

        synchronized(recentNotifications) {
            if (now - (recentNotifications[dedupeKey] ?: 0L) < 10000L) {
                return
            }

            recentNotifications[dedupeKey] = now

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

        val saved = database.saveTransaction(
            dedupeKey = dedupeKey,
            amount = amount,
            direction = direction,
            counterparty = "",
            appName = appName,
            packageName = sbn.packageName,
            title = title,
            message = message
        )

        if (!saved) return

        val announcement = if (direction == "RECEIVED") {
            "$amountString रुपये $appName पर प्राप्त हुए"
        } else {
            "$amountString रुपये $appName से भेजे गए"
        }

        synchronized(pendingAnnouncements) {
            if (ready) {
                speak(announcement)
            } else {
                pendingAnnouncements.add(announcement)
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

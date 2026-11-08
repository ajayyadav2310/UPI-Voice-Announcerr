
package com.ajay.upivoice

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.Secure
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import android.graphics.drawable.GradientDrawable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var database: TransactionDb
    private lateinit var content: LinearLayout

    private val bg = Color.rgb(11, 16, 32)
    private val card = Color.rgb(24, 32, 53)
    private val white = Color.WHITE
    private val green = Color.rgb(48, 210, 145)
    private val red = Color.rgb(255, 105, 115)
    private val blue = Color.rgb(100, 145, 255)
    private val muted = Color.rgb(160, 174, 195)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = TransactionDb(this)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(25), dp(20), dp(30))
        }

        scroll.addView(content)
        setContentView(scroll)

        showDashboard()
    }

    private fun showDashboard() {
        content.removeAllViews()

        addText("UPI MANAGER", 25f, white, true)
        addText("Your offline payment assistant", 13f, muted)
        addSpace(24)

        addText("OVERVIEW", 13f, muted, true)
        addSpace(10)

        val totals = getTotals()

        addStatCard("TOTAL RECEIVED", "₹${format(totals.first)}", green)
        addSpace(12)
        addStatCard("TOTAL SENT", "₹${format(totals.second)}", red)
        addSpace(12)
        addStatCard("TOTAL TRANSACTIONS", totals.third.toString(), blue)

        addSpace(25)
        addText("RECENT TRANSACTIONS", 17f, white, true)
        addSpace(10)

        showRecentTransactions()

        addSpace(22)

        addButton("Refresh Dashboard") {
            showDashboard()
        }

        addSpace(10)

        addButton("Enable Notification Access") {
            startActivity(
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            )
        }

        addSpace(10)

        addText(
            "Your data stays on this device. Payment records are based on app notifications, not independent bank verification.",
            12f,
            muted
        )
    }

    private fun addStatCard(
        heading: String,
        value: String,
        color: Int
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(17), dp(18), dp(17))
            background = rounded(card, 18)
        }

        val title = TextView(this).apply {
            text = heading
            textSize = 12f
            setTextColor(muted)
            setTypeface(null, Typeface.BOLD)
        }

        val amount = TextView(this).apply {
            text = value
            textSize = 27f
            setTextColor(color)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(9), 0, 0)
        }

        box.addView(title)
        box.addView(amount)

        content.addView(
            box,
            LinearLayout.LayoutParams(-1, -2)
        )
    }

    private fun showRecentTransactions() {
        val cursor = database.getRecentTransactions(10)

        if (!cursor.moveToFirst()) {
            addEmptyState()
            cursor.close()
            return
        }

        do {
            val amount = cursor.getDouble(
                cursor.getColumnIndexOrThrow("amount")
            )

            val direction = cursor.getString(
                cursor.getColumnIndexOrThrow("direction")
            )

            val app = cursor.getString(
                cursor.getColumnIndexOrThrow("app_name")
            ) ?: "UPI"

            val time = cursor.getLong(
                cursor.getColumnIndexOrThrow("received_at")
            )

            val name = cursor.getString(
                cursor.getColumnIndexOrThrow("counterparty")
            ).orEmpty()

            val received = direction == "RECEIVED"

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(14), dp(15), dp(14))
                background = rounded(card, 15)
            }

            val heading = TextView(this).apply {
                text = if (received) "Received  +₹${format(amount)}"
                       else "Sent  -₹${format(amount)}"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (received) green else red)
            }

            val details = TextView(this).apply {
                text = buildString {
                    append(app)
                    if (name.isNotBlank()) append(" • $name")
                    append("\n")
                    append(
                        SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                        ).format(Date(time))
                    )
                }
                textSize = 12f
                setTextColor(muted)
                setPadding(0, dp(7), 0, 0)
            }

            item.addView(heading)
            item.addView(details)

            content.addView(
                item,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(10)
                }
            )

        } while (cursor.moveToNext())

        cursor.close()
    }

    private fun getTotals(): Triple<Double, Double, Int> {
        var received = 0.0
        var sent = 0.0
        var count = 0

        val cursor = database.readableDatabase.rawQuery(
            """
            SELECT
                COALESCE(SUM(CASE WHEN direction='RECEIVED' THEN amount ELSE 0 END),0),
                COALESCE(SUM(CASE WHEN direction='SENT' THEN amount ELSE 0 END),0),
                COUNT(*)
            FROM transactions
            """.trimIndent(),
            null
        )

        if (cursor.moveToFirst()) {
            received = cursor.getDouble(0)
            sent = cursor.getDouble(1)
            count = cursor.getInt(2)
        }

        cursor.close()
        return Triple(received, sent, count)
    }

    private fun addEmptyState() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(30), dp(20), dp(30))
            background = rounded(card, 15)
        }

        val title = TextView(this).apply {
            text = "No transactions yet"
            textSize = 17f
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Your real payment notifications will appear here."
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        box.addView(title)
        box.addView(subtitle)

        content.addView(box)
    }

    private fun addText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        val view = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
        content.addView(view)
    }

    private fun addButton(
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            setTextColor(white)
            setBackgroundColor(Color.rgb(49, 92, 190))
            setOnClickListener { action() }
        }

        content.addView(
            button,
            LinearLayout.LayoutParams(-1, dp(52))
        )
    }

    private fun addSpace(size: Int) {
        val space = android.view.View(this)
        content.addView(
            space,
            LinearLayout.LayoutParams(1, dp(size))
        )
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun format(value: Double): String {
        return String.format(Locale.US, "%,.2f", value)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) showDashboard()
    }
}

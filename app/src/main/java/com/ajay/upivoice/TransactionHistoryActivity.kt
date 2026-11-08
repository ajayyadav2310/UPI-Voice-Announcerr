
package com.ajay.upivoice

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.*

class MainActivity : Activity() {

    private lateinit var db: TransactionDb
    private lateinit var root: LinearLayout

    private val bg = Color.rgb(11, 16, 32)
    private val card = Color.rgb(24, 32, 53)
    private val white = Color.WHITE
    private val green = Color.rgb(48, 210, 145)
    private val red = Color.rgb(255, 105, 115)
    private val muted = Color.rgb(160, 174, 195)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = TransactionDb(this)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(25), dp(20), dp(30))
        }

        scroll.addView(root)
        setContentView(scroll)

        showDashboard()
    }

    private fun showDashboard() {
        root.removeAllViews()

        addText("UPI MANAGER", 27f, white, true)
        addText("Your offline payment assistant", 13f, muted)
        space(24)

        addText("PAYMENT OVERVIEW", 14f, muted, true)
        space(10)

        val totals = getTotals()

        addCard("TOTAL RECEIVED", "₹${money(totals.first)}", green)
        space(12)

        addCard("TOTAL SENT", "₹${money(totals.second)}", red)
        space(12)

        addCard("TOTAL TRANSACTIONS", totals.third.toString(), white)

        space(25)
        addText("MANAGE YOUR PAYMENTS", 16f, white, true)
        space(10)

        addMenu(
            "Transaction History",
            "Search and view payments"
        ) {
            open(TransactionHistoryActivity::class.java)
        }

        addMenu(
            "Analytics & Reports",
            "Daily, weekly and monthly statistics"
        ) {
            open(AnalyticsActivity::class.java)
        }

        addMenu(
            "Voice Settings",
            "Language, speed and announcements"
        ) {
            open(SettingsActivity::class.java)
        }

        addMenu(
            "Local Backup",
            "Backup and restore your data"
        ) {
            open(BackupActivity::class.java)
        }

        addMenu(
            "Export Transactions",
            "Export payment history as CSV"
        ) {
            open(ExportActivity::class.java)
        }

        space(15)

        addMenu(
            "Notification Access",
            "Enable payment notification reading"
        ) {
            startActivity(
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            )
        }

        space(20)

        addText(
            "OFFLINE STORAGE • NO CLOUD SYNC",
            12f,
            muted,
            true
        )

        space(6)

        addText(
            "Payment records are based on notifications, not independent bank verification.",
            12f,
            muted
        )
    }

    private fun getTotals(): Triple<Double, Double, Int> {
        val cursor = db.readableDatabase.rawQuery(
            """
            SELECT
            COALESCE(SUM(CASE WHEN direction='RECEIVED' THEN amount ELSE 0 END),0),
            COALESCE(SUM(CASE WHEN direction='SENT' THEN amount ELSE 0 END),0),
            COUNT(*)
            FROM transactions
            """.trimIndent(),
            null
        )

        var received = 0.0
        var sent = 0.0
        var count = 0

        if (cursor.moveToFirst()) {
            received = cursor.getDouble(0)
            sent = cursor.getDouble(1)
            count = cursor.getInt(2)
        }

        cursor.close()

        return Triple(received, sent, count)
    }

    private fun addCard(
        title: String,
        value: String,
        color: Int
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(16), dp(17), dp(16))
            background = rounded(card)
        }

        box.addView(TextView(this).apply {
            text = title
            textSize = 12f
            setTextColor(muted)
            setTypeface(null, Typeface.BOLD)
        })

        box.addView(TextView(this).apply {
            text = value
            textSize = 26f
            setTextColor(color)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        })

        root.addView(box)
    }

    private fun addMenu(
        title: String,
        subtitle: String,
        action: () -> Unit
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(card)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        box.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)
        })

        box.addView(TextView(this).apply {
            text = subtitle
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(5), 0, 0)
        })

        root.addView(
            box,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            }
        )
    }

    private fun addText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        root.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
        })
    }

    private fun space(size: Int) {
        root.addView(
            android.view.View(this),
            LinearLayout.LayoutParams(1, dp(size))
        )
    }

    private fun rounded(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(18).toFloat()
        }
    }

    private fun open(activity: Class<*>) {
        startActivity(Intent(this, activity))
    }

    private fun money(value: Double): String {
        return String.format(
            java.util.Locale.US,
            "%,.2f",
            value
        )
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    override fun onResume() {
        super.onResume()

        if (::root.isInitialized) {
            showDashboard()
        }
    }
}

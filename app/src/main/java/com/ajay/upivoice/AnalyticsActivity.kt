package com.ajay.upivoice

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import java.util.Calendar
import java.util.Locale

class AnalyticsActivity : Activity() {

    private lateinit var db: TransactionDb
    private lateinit var content: LinearLayout

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

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(25))
        }

        scroll.addView(content)
        setContentView(scroll)

        loadAnalytics()
    }

    private fun loadAnalytics() {
        content.removeAllViews()

        addText("Analytics", 27f, white, true)
        addText("Your real payment statistics", 13f, muted)
        addSpace(24)

        addText("ALL TIME", 13f, muted, true)
        addSpace(10)

        val all = getStats(0L)

        addCard("Total Received", "₹${money(all.first)}", green)
        addSpace(10)
        addCard("Total Sent", "₹${money(all.second)}", red)
        addSpace(10)
        addCard("Total Transactions", all.third.toString(), white)

        addSpace(25)

        addText("TIME PERIOD REPORTS", 16f, white, true)
        addSpace(12)

        val day = getStats(startOfDay())
        val week = getStats(startOfWeek())
        val month = getStats(startOfMonth())

        addPeriod("Today", day)
        addPeriod("This Week", week)
        addPeriod("This Month", month)

        addSpace(25)
        addText("APP-WISE SUMMARY", 17f, white, true)
        addSpace(10)

        showAppSummary()

        addSpace(20)

        val refresh = Button(this).apply {
            text = "Refresh Analytics"
            setOnClickListener { loadAnalytics() }
        }
        content.addView(refresh)
    }

    private fun getStats(from: Long): Triple<Double, Double, Int> {
        val cursor = db.readableDatabase.rawQuery(
            """
            SELECT
            COALESCE(SUM(CASE WHEN direction='RECEIVED' THEN amount ELSE 0 END),0),
            COALESCE(SUM(CASE WHEN direction='SENT' THEN amount ELSE 0 END),0),
            COUNT(*)
            FROM transactions
            WHERE received_at >= ?
            """.trimIndent(),
            arrayOf(from.toString())
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

    private fun showAppSummary() {
        val cursor = db.readableDatabase.rawQuery(
            """
            SELECT app_name,
            SUM(CASE WHEN direction='RECEIVED' THEN amount ELSE 0 END),
            SUM(CASE WHEN direction='SENT' THEN amount ELSE 0 END),
            COUNT(*)
            FROM transactions
            GROUP BY app_name
            ORDER BY COUNT(*) DESC
            """.trimIndent(),
            null
        )

        if (!cursor.moveToFirst()) {
            addText("No transaction data available yet.", 13f, muted)
            cursor.close()
            return
        }

        do {
            val app = cursor.getString(0) ?: "UPI"
            val received = cursor.getDouble(1)
            val sent = cursor.getDouble(2)
            val count = cursor.getInt(3)

            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(14), dp(15), dp(14))
                setBackgroundColor(card)
            }

            val title = TextView(this).apply {
                text = app
                textSize = 16f
                setTextColor(white)
                setTypeface(null, Typeface.BOLD)
            }

            val detail = TextView(this).apply {
                text = "Received: ₹${money(received)}\n" +
                    "Sent: ₹${money(sent)}\n" +
                    "Transactions: $count"
                textSize = 13f
                setTextColor(muted)
                setPadding(0, dp(8), 0, 0)
            }

            box.addView(title)
            box.addView(detail)

            content.addView(
                box,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(10)
                }
            )

        } while (cursor.moveToNext())

        cursor.close()
    }

    private fun addPeriod(
        title: String,
        stats: Triple<Double, Double, Int>
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
            setBackgroundColor(card)
        }

        val heading = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)
        }

        val details = TextView(this).apply {
            text = "Received: ₹${money(stats.first)}\n" +
                "Sent: ₹${money(stats.second)}\n" +
                "Transactions: ${stats.third}"
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(8), 0, 0)
        }

        box.addView(heading)
        box.addView(details)

        content.addView(
            box,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            }
        )
    }

    private fun addCard(title: String, value: String, color: Int) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            setBackgroundColor(card)
        }

        val heading = TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(muted)
        }

        val amount = TextView(this).apply {
            text = value
            textSize = 24f
            setTextColor(color)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(7), 0, 0)
        }

        box.addView(heading)
        box.addView(amount)
        content.addView(box)
    }

    private fun addText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        content.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        })
    }

    private fun addSpace(size: Int) {
        content.addView(
            android.view.View(this),
            LinearLayout.LayoutParams(1, dp(size))
        )
    }

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun money(value: Double) =
        String.format(Locale.US, "%,.2f", value)

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()
}

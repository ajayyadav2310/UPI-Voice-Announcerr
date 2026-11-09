
package com.ajay.upivoice

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val bg = Color.rgb(10, 15, 27)
    private val card = Color.rgb(21, 29, 45)
    private val green = Color.rgb(42, 211, 145)
    private val blue = Color.rgb(76, 155, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(151, 164, 184)

    private lateinit var root: LinearLayout
    private lateinit var receivedText: TextView
    private lateinit var sentText: TextView
    private lateinit var countText: TextView
    private lateinit var netText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildDashboard()
    }

    private fun buildDashboard() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
        }

        scroll.addView(content)

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        heading.addView(label("UPI MANAGER", 12, green, true))
        heading.addView(label("Dashboard", 27, white, true))
        heading.addView(label(
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date()),
            12, muted
        ))

        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))

        val status = label("● LOCAL", 11, green, true)
        status.setPadding(dp(12), dp(9), dp(12), dp(9))
        status.background = rounded(card, 30)
        header.addView(status)

        content.addView(header)
        space(content, 24)

        // Main balance-style summary card
        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(20, 91, 83), Color.rgb(24, 54, 83))
            ).apply { cornerRadius = dp(22).toFloat() }
        }

        summary.addView(label("TOTAL RECEIVED", 12, Color.LTGRAY, true))

        receivedText = label("₹0.00", 34, white, true)
        summary.addView(receivedText)

        space(summary, 18)

        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val sentBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        sentBox.addView(label("TOTAL SENT", 11, Color.LTGRAY))
        sentText = label("₹0.00", 19, white, true)
        sentBox.addView(sentText)

        val netBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        netBox.addView(label("NET FLOW", 11, Color.LTGRAY))
        netText = label("₹0.00", 19, white, true)
        netBox.addView(netText)

        line.addView(sentBox, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(netBox, LinearLayout.LayoutParams(0, -2, 1f))
        summary.addView(line)

        content.addView(summary)
        space(content, 22)

        // Transaction count
        countText = label("0 Transactions recorded", 14, muted, true)
        content.addView(countText)
        space(content, 12)

        // Quick actions
        content.addView(label("QUICK ACTIONS", 13, muted, true))
        space(content, 12)

        val actionRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actionRow1.addView(actionCard("▤", "History", "View transactions", blue) {
            open(TransactionHistoryActivity::class.java)
        }, weightedParams())

        actionRow1.addView(actionCard("◈", "Analytics", "View insights", green) {
            open(AnalyticsActivity::class.java)
        }, weightedParams())

        content.addView(actionRow1)
        space(content, 12)

        val actionRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actionRow2.addView(actionCard("♫", "Voice Studio", "Voice settings", Color.rgb(190, 137, 255)) {
            open(SettingsActivity::class.java)
        }, weightedParams())

        actionRow2.addView(actionCard("⇧", "Export", "Export CSV", Color.rgb(255, 178, 90)) {
            open(ExportActivity::class.java)
        }, weightedParams())

        content.addView(actionRow2)
        space(content, 12)

        val actionRow3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actionRow3.addView(actionCard("▣", "Backup", "Local backup", Color.rgb(255, 130, 150)) {
            open(BackupActivity::class.java)
        }, weightedParams())

        actionRow3.addView(actionCard("⚙", "Settings", "App preferences", muted) {
            open(SettingsActivity::class.java)
        }, weightedParams())

        content.addView(actionRow3)
        space(content, 22)

        // Notification access
        val access = TextView(this).apply {
            text = "  ♧   Notification Access     ›"
            textSize = 15f
            setTextColor(white)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(card, 16)
            setOnClickListener {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
        }

        content.addView(access, LinearLayout.LayoutParams(-1, dp(58)))
        space(content, 18)

        content.addView(label(
            "Your transactions are stored locally on this device.",
            11, muted
        ))

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bottomNavigation())

        setContentView(root)
        refreshStats()
    }

    private fun actionCard(
        symbol: String,
        title: String,
        subtitle: String,
        accent: Int,
        click: () -> Unit
    ): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(16), dp(10), dp(16))
            background = rounded(card, 18)
            setOnClickListener { click() }
        }

        box.addView(label(symbol, 25, accent, true))
        space(box, 10)
        box.addView(label(title, 15, white, true))
        box.addView(label(subtitle, 11, muted))

        return box
    }

    private fun bottomNavigation(): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(10), dp(5), dp(10))
            setBackgroundColor(Color.rgb(17, 24, 38))
        }

        nav.addView(navItem("⌂", "Home", true) {})
        nav.addView(navItem("▤", "History") {
            open(TransactionHistoryActivity::class.java)
        })
        nav.addView(navItem("◈", "Insights") {
            open(AnalyticsActivity::class.java)
        })
        nav.addView(navItem("♫", "Voice") {
            open(SettingsActivity::class.java)
        })
        nav.addView(navItem("⚙", "Settings") {
            open(SettingsActivity::class.java)
        })

        return nav
    }

    private fun navItem(
        symbol: String,
        title: String,
        selected: Boolean = false,
        click: () -> Unit
    ): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(4), dp(5), dp(4))
            setOnClickListener { click() }
        }

        item.addView(label(symbol, 20, if (selected) green else muted, true))
        item.addView(label(title, 10, if (selected) green else muted))
        return item.apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
    }

    private fun refreshStats() {
        try {
            val db = TransactionDb(this)
            val cursor = db.getRecentTransactions(10000)

            var received = 0.0
            var sent = 0.0
            var count = 0

            cursor.use {
                val amountIndex = it.getColumnIndex("amount")
                val directionIndex = it.getColumnIndex("direction")

                while (it.moveToNext()) {
                    if (amountIndex < 0 || directionIndex < 0) continue

                    val amount = it.getDouble(amountIndex)
                    val direction = it.getString(directionIndex) ?: ""

                    when (direction.uppercase(Locale.ROOT)) {
                        "RECEIVED" -> received += amount
                        "SENT" -> sent += amount
                    }
                    count++
                }
            }

            receivedText.text = money(received)
            sentText.text = money(sent)
            netText.text = money(received - sent)
            countText.text = "$count Transactions recorded"

        } catch (e: Exception) {
            countText.text = "Unable to load transaction data"
        }
    }

    private fun money(value: Double): String {
        return "₹" + String.format(Locale.US, "%,.2f", value)
    }

    private fun open(target: Class<*>) {
        startActivity(Intent(this, target))
    }

    private fun label(
        value: String,
        size: Int,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun space(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun weightedParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, dp(116), 1f).apply {
            marginEnd = dp(8)
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        if (::receivedText.isInitialized) refreshStats()
    }
}

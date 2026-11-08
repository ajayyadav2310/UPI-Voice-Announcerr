
package com.ajay.upivoice

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionHistoryActivity : Activity() {

    private lateinit var db: TransactionDb
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private var filter = "ALL"

    private val bg = Color.rgb(11, 16, 32)
    private val card = Color.rgb(24, 32, 53)
    private val white = Color.WHITE
    private val green = Color.rgb(48, 210, 145)
    private val red = Color.rgb(255, 105, 115)
    private val muted = Color.rgb(160, 174, 195)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = TransactionDb(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(15))
            setBackgroundColor(bg)
        }

        root.addView(TextView(this).apply {
            text = "Transaction History"
            textSize = 24f
            setTextColor(white)
        })

        search = EditText(this).apply {
            hint = "Search transactions..."
            setHintTextColor(muted)
            setTextColor(white)
            setSingleLine(true)
        }
        root.addView(search)

        val filters = LinearLayout(this)
        listOf("ALL", "RECEIVED", "SENT").forEach { value ->
            filters.addView(Button(this).apply {
                text = value
                setOnClickListener {
                    filter = value
                    loadTransactions()
                }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        root.addView(filters)

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(20))
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)

        search.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                loadTransactions()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        loadTransactions()
    }

    private fun loadTransactions() {
        list.removeAllViews()

        val cursor = db.getRecentTransactions(1000)
        val query = search.text.toString().lowercase(Locale.ROOT)
        var found = 0

        if (cursor.moveToFirst()) {
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
                val name = cursor.getString(
                    cursor.getColumnIndexOrThrow("counterparty")
                ).orEmpty()
                val time = cursor.getLong(
                    cursor.getColumnIndexOrThrow("received_at")
                )

                if (filter != "ALL" && direction != filter) continue

                if (!"$app $name $amount $direction"
                        .lowercase(Locale.ROOT).contains(query)) continue

                found++

                val received = direction == "RECEIVED"

                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(15), dp(15), dp(15), dp(15))
                    setBackgroundColor(card)
                }

                item.addView(TextView(this).apply {
                    text = "${if (received) "+ " else "- "}₹" +
                        String.format(Locale.US, "%,.2f", amount)
                    textSize = 19f
                    setTextColor(if (received) green else red)
                })

                item.addView(TextView(this).apply {
                    text = "$app${if (name.isNotBlank()) " • $name" else ""}\n" +
                        SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                        ).format(Date(time))
                    textSize = 13f
                    setTextColor(muted)
                    setPadding(0, dp(7), 0, 0)
                })

                list.addView(item, LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(10)
                })

            } while (cursor.moveToNext())
        }

        cursor.close()

        if (found == 0) {
            list.addView(TextView(this).apply {
                text = "No transactions found"
                setTextColor(muted)
                gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, dp(40))
            })
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

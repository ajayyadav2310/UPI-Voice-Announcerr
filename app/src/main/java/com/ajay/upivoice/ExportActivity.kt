package com.ajay.upivoice

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.widget.*
import java.io.OutputStreamWriter

class ExportActivity : Activity() {

    private val bg = Color.rgb(11, 16, 32)
    private val white = Color.WHITE
    private val muted = Color.rgb(160, 174, 195)
    private val createFileRequest = 301

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 30, 22, 22)
            setBackgroundColor(bg)
        }

        root.addView(TextView(this).apply {
            text = "Export Transactions"
            textSize = 25f
            setTextColor(white)
        })

        root.addView(TextView(this).apply {
            text = "Export your locally saved payment records as a CSV file."
            textSize = 14f
            setTextColor(muted)
            setPadding(0, 12, 0, 20)
        })

        root.addView(Button(this).apply {
            text = "Export CSV"
            setOnClickListener { createCsvFile() }
        })

        setContentView(root)
    }

    private fun createCsvFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "UPI_Transactions.csv")
        }

        startActivityForResult(intent, createFileRequest)
    }

    @Deprecated("Deprecated Android result API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != createFileRequest ||
            resultCode != RESULT_OK
        ) return

        val uri = data?.data ?: return
        exportCsv(uri)
    }

    private fun exportCsv(uri: Uri) {
        try {
            val cursor = TransactionDb(this).getRecentTransactions(100000)

            contentResolver.openOutputStream(uri)?.use { output ->
                OutputStreamWriter(output, Charsets.UTF_8).use { writer ->

                    writer.write(
                        "Amount,Direction,Counterparty,App,Package,Date,Title,Notification\n"
                    )

                    if (cursor.moveToFirst()) {
                        do {
                            val amount = cursor.getDouble(
                                cursor.getColumnIndexOrThrow("amount")
                            )

                            val direction = cursor.getString(
                                cursor.getColumnIndexOrThrow("direction")
                            )

                            val name = cursor.getString(
                                cursor.getColumnIndexOrThrow("counterparty")
                            ).orEmpty()

                            val app = cursor.getString(
                                cursor.getColumnIndexOrThrow("app_name")
                            ).orEmpty()

                            val packageName = cursor.getString(
                                cursor.getColumnIndexOrThrow("package_name")
                            ).orEmpty()

                            val time = cursor.getLong(
                                cursor.getColumnIndexOrThrow("received_at")
                            )

                            val title = cursor.getString(
                                cursor.getColumnIndexOrThrow("notification_title")
                            ).orEmpty()

                            val message = cursor.getString(
                                cursor.getColumnIndexOrThrow("notification_text")
                            ).orEmpty()

                            val date = java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                java.util.Locale.US
                            ).format(java.util.Date(time))

                            val values = listOf(
                                amount.toString(),
                                direction,
                                name,
                                app,
                                packageName,
                                date,
                                title,
                                message
                            )

                            writer.write(
                                values.joinToString(",") { csv(it) } + "\n"
                            )

                        } while (cursor.moveToNext())
                    }

                    cursor.close()
                    writer.flush()
                }
            } ?: throw Exception("Cannot create export file")

            Toast.makeText(
                this,
                "CSV exported successfully",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Export failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun csv(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}

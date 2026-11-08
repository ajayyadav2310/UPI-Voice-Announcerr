package com.ajay.upivoice

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.widget.*
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupActivity : Activity() {

    private val bg = Color.rgb(11, 16, 32)
    private val white = Color.WHITE
    private val muted = Color.rgb(160, 174, 195)

    private val createBackupCode = 101
    private val restoreBackupCode = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 30, 22, 22)
            setBackgroundColor(bg)
        }

        val title = TextView(this).apply {
            text = "Local Backup"
            textSize = 26f
            setTextColor(white)
        }

        val description = TextView(this).apply {
            text = "Create or restore your transaction database backup. Files remain under your control."
            textSize = 14f
            setTextColor(muted)
        }

        val backup = Button(this).apply {
            text = "Create Backup"
            setOnClickListener { createBackup() }
        }

        val restore = Button(this).apply {
            text = "Restore Backup"
            setOnClickListener { chooseBackup() }
        }

        root.addView(title)
        root.addView(description)
        root.addView(backup)
        root.addView(restore)

        setContentView(root)
    }

    private fun createBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(
                Intent.EXTRA_TITLE,
                "UPI_Manager_Backup.db"
            )
        }

        startActivityForResult(intent, createBackupCode)
    }

    private fun chooseBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, restoreBackupCode)
    }

    @Deprecated("Deprecated Android result API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) return

        val uri = data?.data ?: return

        when (requestCode) {
            createBackupCode -> exportDatabase(uri)
            restoreBackupCode -> importDatabase(uri)
        }
    }

    private fun exportDatabase(uri: Uri) {
        try {
            val dbFile = getDatabasePath("upi_transactions.db")

            if (!dbFile.exists()) {
                toast("No transaction database found")
                return
            }

            contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            }

            toast("Backup created successfully")
        } catch (e: Exception) {
            toast("Backup failed: ${e.message}")
        }
    }

    private fun importDatabase(uri: Uri) {
        try {
            val dbFile = getDatabasePath("upi_transactions.db")
            val tempFile = File(filesDir, "restore_temp.db")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Cannot open backup file")

            val checkDb = SQLiteDatabase.openDatabase(
                tempFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            val valid = checkDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'",
                null
            ).use { it.moveToFirst() }

            checkDb.close()

            if (!valid) throw Exception("Invalid UPI Manager backup")

            getSharedPreferences("upi_settings", MODE_PRIVATE)
                .edit()
                .putBoolean("restore_in_progress", true)
                .apply()

            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()

            getSharedPreferences("upi_settings", MODE_PRIVATE)
                .edit()
                .putBoolean("restore_in_progress", false)
                .apply()

            toast("Backup restored. Restart the app.")
        } catch (e: Exception) {
            File(filesDir, "restore_temp.db").delete()
            toast("Restore failed: ${e.message}")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

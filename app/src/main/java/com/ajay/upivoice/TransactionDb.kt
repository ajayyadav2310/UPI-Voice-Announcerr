package com.ajay.upivoice

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TransactionDb(context: Context) :
    SQLiteOpenHelper(context, "upi_transactions.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dedupe_key TEXT UNIQUE,
                amount REAL NOT NULL,
                direction TEXT NOT NULL,
                counterparty TEXT,
                app_name TEXT,
                package_name TEXT,
                notification_title TEXT,
                notification_text TEXT,
                received_at INTEGER NOT NULL,
                verification_status TEXT DEFAULT 'NOTIFICATION_ONLY'
            )
        """.trimIndent())
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Future database upgrades
    }

    fun saveTransaction(
        dedupeKey: String,
        amount: Double,
        direction: String,
        counterparty: String,
        appName: String,
        packageName: String,
        title: String,
        message: String
    ): Boolean {
        val values = ContentValues().apply {
            put("dedupe_key", dedupeKey)
            put("amount", amount)
            put("direction", direction)
            put("counterparty", counterparty)
            put("app_name", appName)
            put("package_name", packageName)
            put("notification_title", title)
            put("notification_text", message)
            put("received_at", System.currentTimeMillis())
            put("verification_status", "NOTIFICATION_ONLY")
        }

        val result = writableDatabase.insertWithOnConflict(
            "transactions",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        return result != -1L
    }

    fun getRecentTransactions(limit: Int = 100) =
        readableDatabase.rawQuery(
            "SELECT * FROM transactions ORDER BY received_at DESC LIMIT ?",
            arrayOf(limit.toString())
        )
    }

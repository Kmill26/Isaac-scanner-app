package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val itemQuality: Int,
    val confidence: Float,
    val verdict: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runTitle: String,
    val character: String,
    val itemsCsv: String, // comma separated item names
    val synergiesCount: Int,
    val winStatus: Boolean = false,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

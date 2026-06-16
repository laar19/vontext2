package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey val jobId: String,
    val status: String, // PENDING, PROCESSING, COMPLETED, FAILED
    val videoPath: String,
    val videoFilename: String,
    val hasAudio: Boolean,
    val videoDuration: Float,
    val additionalNotes: String?,
    val progress: Int,
    val progressMessage: String,
    val errorMessage: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val pdfPath: String?,
    val zipPath: String?,
    val frameInterval: Int, // 0 = Auto
    val whisperMode: String, // LOCAL_SMALL, REMOTE
    val transcriptionText: String? = null
)

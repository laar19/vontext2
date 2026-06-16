package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    fun getJobById(jobId: String): Flow<Job?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: Job)

    @Update
    suspend fun updateJob(job: Job)

    @Query("DELETE FROM jobs WHERE jobId = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("DELETE FROM jobs")
    suspend fun clearAllJobs()
}

package com.example.data

import kotlinx.coroutines.flow.Flow

class JobRepository(private val jobDao: JobDao) {
    val allJobs: Flow<List<Job>> = jobDao.getAllJobs()

    fun getJobById(jobId: String): Flow<Job?> = jobDao.getJobById(jobId)

    suspend fun getJobByIdSync(jobId: String): Job? = jobDao.getJobByIdSync(jobId)

    suspend fun insertJob(job: Job) = jobDao.insertJob(job)

    suspend fun updateJob(job: Job) = jobDao.updateJob(job)

    suspend fun deleteJob(jobId: String) = jobDao.deleteJob(jobId)

    suspend fun clearAllJobs() = jobDao.clearAllJobs()
}

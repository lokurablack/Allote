package com.example.allote.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class ResumenData(
    val hectareas: Double,
    val costoPorHectarea: Double,
    val totalSinIVA: Double,
    val totalConIVA: Double,
    val aplicaIVA: Boolean
)

@Singleton
class AdministracionResumenRepository @Inject constructor(
    private val jobDao: JobDao,
    private val administracionDao: AdministracionDao
) {
    fun getResumenDataStream(jobId: Int): Flow<ResumenData?> {
        return jobDao.getByIdStream(jobId).combine(administracionDao.getByJobIdStream(jobId)) { job, adm ->
            if (job != null && adm != null) {
                ResumenData(
                    hectareas = job.surface ?: 0.0,
                    costoPorHectarea = adm.costoPorHectarea,
                    totalSinIVA = adm.totalSinIVA,
                    totalConIVA = adm.totalConIVA,
                    aplicaIVA = adm.aplicaIVA
                )
            } else {
                null
            }
        }
    }
}
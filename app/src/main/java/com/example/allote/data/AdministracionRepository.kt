package com.example.allote.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdministracionRepository @Inject constructor(
    private val jobDao: JobDao,
    private val administracionDao: AdministracionDao,
    private val movimientoContableDao: MovimientoContableDao) {
        fun getJobStream(jobId: Int): Flow<Job?> = jobDao.getByIdStream(jobId)
        fun getAdministracionStream(jobId: Int): Flow<AdministracionTrabajo?> = administracionDao.getByJobIdStream(jobId)
        suspend fun getMovimientoContable(jobId: Int): MovimientoContable? = movimientoContableDao.getMovimientoByJobId(jobId)
        suspend fun saveAdministracionData(administracion: AdministracionTrabajo) { administracionDao.insert(administracion) }
        suspend fun saveMovimientoContable(movimiento: MovimientoContable) {
            movimientoContableDao.upsert(movimiento)
        }
    }
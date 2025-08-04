package com.example.allote.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainDashboardRepository @Inject constructor(
    private val jobDao: JobDao,
    private val clientDao: ClientDao,
    private val movimientoContableDao: MovimientoContableDao,
    private val exchangeRateRepository: ExchangeRateRepository
) {
    fun getAllJobsStream(): Flow<List<Job>> = jobDao.getAllStream()
    fun getAllClientsStream(): Flow<List<Client>> = clientDao.getAllStream()
    fun getAllMovimientosStream(): Flow<List<MovimientoContable>> = movimientoContableDao.getAllMovimientosStream()
    suspend fun getDolarRates(): DolarApiResponse? {
        return exchangeRateRepository.getDolarApiResponse()
    }
}
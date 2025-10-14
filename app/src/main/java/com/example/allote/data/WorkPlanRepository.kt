package com.example.allote.data

import com.example.allote.service.FlightPlanningService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkPlanRepository @Inject constructor(
    private val workPlanDao: WorkPlanDao,
    private val flightSegmentDao: FlightSegmentDao,
    private val jobDao: JobDao,
    private val loteDao: LoteDao,
    private val jobParametrosDao: JobParametrosDao
) {

    private val planningService = FlightPlanningService()

    fun getWorkPlansForJob(jobId: Int): Flow<List<WorkPlan>> {
        return workPlanDao.getWorkPlansForJob(jobId)
    }

    fun getLatestWorkPlanForLote(jobId: Int, loteId: Int): Flow<WorkPlan?> {
        return workPlanDao.getLatestWorkPlanForLote(jobId, loteId)
    }

    fun getLatestWorkPlanForJob(jobId: Int): Flow<WorkPlan?> {
        return workPlanDao.getLatestWorkPlanForJob(jobId)
    }

    fun getFlightSegmentsForPlan(workPlanId: Int): Flow<List<FlightSegment>> {
        return flightSegmentDao.getFlightSegmentsForPlan(workPlanId)
    }

    suspend fun createWorkPlan(input: FlightPlanningService.PlanningInput): WorkPlan {
        val result = planningService.calculateOptimalPlan(input)
        val planId = workPlanDao.insert(result.workPlan)
        val segmentsWithPlanId = result.flightSegments.map { segment ->
            segment.copy(workPlanId = planId.toInt())
        }
        flightSegmentDao.insertAll(segmentsWithPlanId)
        return result.workPlan.copy(id = planId.toInt())
    }

    suspend fun updateWorkPlan(workPlan: WorkPlan) {
        workPlanDao.update(workPlan)
    }

    suspend fun deleteWorkPlan(workPlan: WorkPlan) {
        flightSegmentDao.deleteAllForPlan(workPlan.id)
        workPlanDao.delete(workPlan)
    }

    suspend fun deleteAllPlansForJob(jobId: Int) {
        workPlanDao.deleteAllForJob(jobId)
    }

    suspend fun getJobInfo(jobId: Int): Job? {
        return jobDao.getJobByIdSync(jobId)
    }

    suspend fun getLoteInfo(loteId: Int): Lote? {
        return loteDao.getLoteByIdSync(loteId)
    }

    suspend fun getJobParameters(jobId: Int): JobParametros? {
        return jobParametrosDao.getByJobId(jobId)
    }

    suspend fun recalculateWorkPlan(
        existingPlan: WorkPlan,
        newInput: FlightPlanningService.PlanningInput
    ): WorkPlan {
        flightSegmentDao.deleteAllForPlan(existingPlan.id)
        val result = planningService.calculateOptimalPlan(newInput)
        val updatedPlan = result.workPlan.copy(
            id = existingPlan.id,
            fechaCreacion = existingPlan.fechaCreacion,
            fechaModificacion = System.currentTimeMillis()
        )
        workPlanDao.update(updatedPlan)
        val segmentsWithPlanId = result.flightSegments.map { segment ->
            segment.copy(workPlanId = existingPlan.id)
        }
        flightSegmentDao.insertAll(segmentsWithPlanId)
        return updatedPlan
    }
}

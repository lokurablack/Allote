package com.example.allote.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightSegmentDao {
    @Query("SELECT * FROM flight_segments WHERE workPlanId = :workPlanId ORDER BY ordenVuelo ASC")
    fun getFlightSegmentsForPlan(workPlanId: Int): Flow<List<FlightSegment>>

    @Query("SELECT * FROM flight_segments WHERE workPlanId = :workPlanId ORDER BY ordenVuelo ASC")
    suspend fun getFlightSegmentsForPlanSync(workPlanId: Int): List<FlightSegment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: FlightSegment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<FlightSegment>)

    @Update
    suspend fun update(segment: FlightSegment)

    @Delete
    suspend fun delete(segment: FlightSegment)

    @Query("DELETE FROM flight_segments WHERE workPlanId = :workPlanId")
    suspend fun deleteAllForPlan(workPlanId: Int)
}

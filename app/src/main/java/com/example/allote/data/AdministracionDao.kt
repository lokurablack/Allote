package com.example.allote.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AdministracionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(adm: AdministracionTrabajo)

    @Query("SELECT * FROM AdministracionTrabajo WHERE jobId = :jobId LIMIT 1")
    suspend fun getByJobId(jobId: Int): AdministracionTrabajo?

    @Query("SELECT * FROM AdministracionTrabajo")
    fun getAllAdministracionTrabajosStream(): Flow<List<AdministracionTrabajo>>

    @Query("SELECT * FROM documento_trabajo WHERE jobId = :jobId")
    suspend fun getAllDocumentsForJob(jobId: Int): List<DocumentoTrabajo>

    @Insert
    suspend fun insertDocumento(documento: DocumentoTrabajo)

    @Query("SELECT * FROM AdministracionTrabajo WHERE jobId = :jobId LIMIT 1")
    fun getByJobIdStream(jobId: Int): Flow<AdministracionTrabajo?>

    @Delete
    suspend fun deleteDocumento(documento: DocumentoTrabajo)

    // (Opcional) para borrar por URI directamente:
    @Query("DELETE FROM documento_trabajo WHERE documentUri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("SELECT * FROM documento_trabajo WHERE jobId = :jobId")
    fun getAllDocumentsForJobStream(jobId: Int): Flow<List<DocumentoTrabajo>>
}

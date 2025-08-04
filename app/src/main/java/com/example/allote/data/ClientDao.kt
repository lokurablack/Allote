package com.example.allote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE ASC")
    fun getAllStream(): Flow<List<Client>>

    @Insert
    suspend fun insert(client: Client)

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()

    @Query("SELECT * FROM clients WHERE id = :clientId LIMIT 1")
    suspend fun getById(clientId: Int): Client?

    @Query("SELECT * FROM clients WHERE id = :clientId")
    fun getByIdStream(clientId: Int): Flow<Client?>

    @Query("SELECT * FROM clients WHERE id = :clientId")
    fun getStream(clientId: Int): Flow<Client?> // Asegúrate de que devuelva un Flow nulable (Flow<Client?>)
}

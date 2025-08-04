package com.example.allote.data

import kotlinx.coroutines.flow.Flow

class ClientsRepository(private val clientDao: ClientDao, private val jobDao: JobDao) {

    // Esta función ahora devuelve un Flow. Room puede hacer esto automáticamente.
    // La UI se actualizará sola cada vez que los clientes cambien.
    fun getAllClientsStream(): Flow<List<Client>> {
        return clientDao.getAllStream() // Nota: Necesitas modificar tu DAO para devolver Flow<List<Client>>
    }

    suspend fun insertClient(client: Client) {
        clientDao.insert(client)
    }

    suspend fun updateClient(client: Client) {
        clientDao.update(client)
    }

    // Aquí centralizamos la lógica de negocio de eliminar un cliente.
    // Si mañana tienes que borrar un cliente desde otro sitio, solo llamas a esta función.
    suspend fun deleteClientAndJobs(client: Client) {
        jobDao.deleteJobsByClientId(client.id)
        clientDao.delete(client)
    }
}
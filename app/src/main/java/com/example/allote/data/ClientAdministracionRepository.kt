package com.example.allote.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientAdministracionRepository @Inject constructor(
    private val clientDao: ClientDao
) {
    // Obtenemos un cliente específico por su ID como un Flow
    fun getClientStream(clientId: Int): Flow<Client?> {
        return clientDao.getByIdStream(clientId)
    }
}
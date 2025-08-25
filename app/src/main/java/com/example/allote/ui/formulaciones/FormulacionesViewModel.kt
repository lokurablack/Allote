package com.example.allote.ui.formulaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.Formulacion
import com.example.allote.data.FormulacionesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormulacionesViewModel @Inject constructor(private val repository: FormulacionesRepository) : ViewModel() {

    private val _formulaciones = MutableStateFlow<List<Formulacion>>(emptyList())
    val formulaciones: StateFlow<List<Formulacion>> = _formulaciones.asStateFlow()

    private val toDeleteInDb = mutableListOf<Formulacion>()
    private var hasUnsavedChanges = false

    private fun reorderAndSetState(newList: List<Formulacion>) {
        val reorderedList = newList.mapIndexed { index, formulacion ->
            formulacion.copy(ordenMezcla = index + 1)
        }
        _formulaciones.value = reorderedList
    }

    fun onMove(fromIndex: Int, toIndex: Int) {
        val currentList = _formulaciones.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val movedItem = currentList.removeAt(fromIndex)
            currentList.add(toIndex, movedItem)
            reorderAndSetState(currentList)
            hasUnsavedChanges = true
        }
    }

    fun addFormulacion(nombre: String, tipoUnidad: String) {
        val currentList = _formulaciones.value
        val newFormulacion = Formulacion(
            id = 0,
            nombre = nombre,
            tipoUnidad = tipoUnidad,
            ordenMezcla = currentList.size + 1
        )
        _formulaciones.value = currentList + newFormulacion
        hasUnsavedChanges = true
    }

    fun updateFormulacion(id: Int, newName: String, newTipo: String) {
        val currentList = _formulaciones.value.map {
            if (it.id == id) it.copy(nombre = newName, tipoUnidad = newTipo) else it
        }
        _formulaciones.value = currentList
        hasUnsavedChanges = true
    }

    fun deleteFormulacion(formulacion: Formulacion) {
        if (formulacion.id != 0) {
            toDeleteInDb.add(formulacion)
        }
        val newList = _formulaciones.value.filterNot { it == formulacion }
        reorderAndSetState(newList)
        hasUnsavedChanges = true
    }

    fun saveChanges(onComplete: () -> Unit) {
        viewModelScope.launch {
            if (toDeleteInDb.isNotEmpty()) {
                repository.deleteAll(toDeleteInDb)
                toDeleteInDb.clear()
            }
            repository.saveAll(_formulaciones.value)
            hasUnsavedChanges = false
            onComplete()
        }
    }

    // Nuevo método para auto-guardado
    fun autoSaveIfNeeded(onComplete: (() -> Unit)? = null) {
        if (hasUnsavedChanges) {
            viewModelScope.launch {
                if (toDeleteInDb.isNotEmpty()) {
                    repository.deleteAll(toDeleteInDb)
                    toDeleteInDb.clear()
                }
                repository.saveAll(_formulaciones.value)
                hasUnsavedChanges = false
                onComplete?.invoke()
            }
        }
    }

    suspend fun isFormulacionInUse(formulacion: Formulacion): Boolean {
        if (formulacion.id == 0) return false
        return repository.getProductCountForFormulacion(formulacion.id) > 0
    }
}
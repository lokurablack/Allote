package com.example.allote.ui.components

import com.example.allote.data.MovimientoContable

sealed class DialogState {
    object None : DialogState()
    object Add : DialogState()
    data class Options(val movimiento: MovimientoContable) : DialogState()
    data class Details(val movimiento: MovimientoContable) : DialogState()
    data class Edit(val movimiento: MovimientoContable) : DialogState()
    data class Delete(val movimiento: MovimientoContable) : DialogState()
}
package com.example.allote.ui.administraciongeneral

import java.util.Calendar

// Define todos los tipos de filtro que queremos ofrecer
enum class TipoFiltroFecha(val displayName: String) {
    MES_ACTUAL("Mes actual"),
    MES_ANTERIOR("Mes anterior"),
    ULTIMOS_SEIS_MESES("Últimos 6 meses"),
    ANO_EN_CURSO("Año en curso"),
    ANO_ANTERIOR("Año anterior"),
    RANGO_PERSONALIZADO("Rango personalizado")
}

// Una clase de datos para mantener el estado del filtro
data class FiltroFecha(
    val tipo: TipoFiltroFecha = TipoFiltroFecha.MES_ACTUAL,
    val fechaInicio: Long? = null,
    val fechaFin: Long? = null
)

// Función de utilidad para calcular los rangos de fechas
fun getRangoDeFechas(filtro: FiltroFecha): Pair<Long, Long> {
    val cal = Calendar.getInstance()

    when (filtro.tipo) {
        TipoFiltroFecha.MES_ACTUAL -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            setInicioDelDia(cal)
            val inicio = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            setFinDelDia(cal)
            val fin = cal.timeInMillis
            return Pair(inicio, fin)
        }
        TipoFiltroFecha.MES_ANTERIOR -> {
            cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            setInicioDelDia(cal)
            val inicio = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            setFinDelDia(cal)
            val fin = cal.timeInMillis
            return Pair(inicio, fin)
        }
        TipoFiltroFecha.ULTIMOS_SEIS_MESES -> {
            val fin = cal.timeInMillis
            cal.add(Calendar.MONTH, -6)
            setInicioDelDia(cal)
            val inicio = cal.timeInMillis
            return Pair(inicio, fin)
        }
        TipoFiltroFecha.ANO_EN_CURSO -> {
            cal.set(Calendar.DAY_OF_YEAR, 1)
            setInicioDelDia(cal)
            val inicio = cal.timeInMillis
            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
            setFinDelDia(cal)
            val fin = cal.timeInMillis
            return Pair(inicio, fin)
        }
        TipoFiltroFecha.ANO_ANTERIOR -> {
            cal.add(Calendar.YEAR, -1)
            cal.set(Calendar.DAY_OF_YEAR, 1)
            setInicioDelDia(cal)
            val inicio = cal.timeInMillis
            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
            setFinDelDia(cal)
            val fin = cal.timeInMillis
            return Pair(inicio, fin)
        }
        TipoFiltroFecha.RANGO_PERSONALIZADO -> {
            // Para el rango personalizado, las fechas ya vienen en el objeto filtro
            return Pair(filtro.fechaInicio ?: 0, filtro.fechaFin ?: Long.MAX_VALUE)
        }
    }
}

private fun setInicioDelDia(cal: Calendar) {
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
}

private fun setFinDelDia(cal: Calendar) {
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
}
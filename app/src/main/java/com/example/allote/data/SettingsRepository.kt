package com.example.allote.data

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class CurrencySettings(
    val displayCurrency: String,
    val exchangeRate: Double
)

data class Prices(
    val liquida: String,
    val solida: String,
    val mixta: String,
    val varias: String
)

@Singleton
class SettingsRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        const val KEY_CURRENCY = "display_currency"
        const val KEY_EXCHANGE_RATE = "exchange_rate_usd_ars"
        const val KEY_PRECIO_LIQUIDA = "precio_liquida"
        const val KEY_PRECIO_SOLIDA = "precio_solida"
        const val KEY_PRECIO_MIXTA = "precio_mixta"
        const val KEY_PRECIO_VARIAS = "precio_varias"
    }

    fun getPricesFlow(): Flow<Prices> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PRECIO_LIQUIDA || key == KEY_PRECIO_SOLIDA || key == KEY_PRECIO_MIXTA || key == KEY_PRECIO_VARIAS) {
                trySend(getCurrentPrices())
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getCurrentPrices())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun getCurrentPrices(): Prices {
        return Prices(
            liquida = sharedPreferences.getString(KEY_PRECIO_LIQUIDA, "") ?: "",
            solida = sharedPreferences.getString(KEY_PRECIO_SOLIDA, "") ?: "",
            mixta = sharedPreferences.getString(KEY_PRECIO_MIXTA, "") ?: "",
            varias = sharedPreferences.getString(KEY_PRECIO_VARIAS, "") ?: ""
        )
    }

    fun getCurrencySettingsFlow(): Flow<CurrencySettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CURRENCY || key == KEY_EXCHANGE_RATE) {
                trySend(getCurrentCurrencySettings())
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getCurrentCurrencySettings())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun getCurrentCurrencySettings(): CurrencySettings {
        val currency = sharedPreferences.getString(KEY_CURRENCY, "USD") ?: "USD"
        val rateString = sharedPreferences.getString(KEY_EXCHANGE_RATE, "1.0") ?: "1.0"
        return CurrencySettings(
            displayCurrency = currency,
            exchangeRate = rateString.toDoubleOrNull() ?: 1.0
        )
    }

    fun saveDisplayCurrency(currency: String) {
        sharedPreferences.edit { putString(KEY_CURRENCY, currency) }
    }

    fun saveExchangeRate(rate: String) {
        sharedPreferences.edit { putString(KEY_EXCHANGE_RATE, rate) }
    }

    fun savePrecioLiquida(value: String) = sharedPreferences.edit { putString(KEY_PRECIO_LIQUIDA, value) }
    fun savePrecioSolida(value: String) = sharedPreferences.edit { putString(KEY_PRECIO_SOLIDA, value) }
    fun savePrecioMixta(value: String) = sharedPreferences.edit { putString(KEY_PRECIO_MIXTA, value) }
    fun savePrecioVarias(value: String) = sharedPreferences.edit { putString(KEY_PRECIO_VARIAS, value) }
    fun saveTankCapacity(value: String) = sharedPreferences.edit { putString("tank_capacity", value) }
    fun getTankCapacity(): String = sharedPreferences.getString("tank_capacity", "") ?: ""

    fun getDefaultPrice(tipoAplicacion: String): Double {
        val key = when (tipoAplicacion.lowercase()) {
            "aplicacion liquida" -> KEY_PRECIO_LIQUIDA
            "aplicacion solida" -> KEY_PRECIO_SOLIDA
            "aplicacion mixta" -> KEY_PRECIO_MIXTA
            "aplicaciones varias" -> KEY_PRECIO_VARIAS
            else -> null
        }
        return key?.let { sharedPreferences.getString(it, "0.0")?.toDoubleOrNull() ?: 0.0 } ?: 0.0
    }
}
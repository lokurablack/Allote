package com.example.allote.data

import com.google.gson.annotations.SerializedName

data class DolarApiResponse(
    @SerializedName("oficial")
    val oficial: DolarValue,

    @SerializedName("blue")
    val blue: DolarValue,

    @SerializedName("last_update")
    val lastUpdate: String
)

data class DolarValue(
    @SerializedName("value_sell")
    val valueSell: Double,

    @SerializedName("value_buy")
    val valueBuy: Double
)
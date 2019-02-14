package com.example.gamgam.currencyconverter.model
import com.google.gson.annotations.SerializedName



data class Currency(
    @SerializedName("results")
    val results: Map<String, Information>
)


data class Information(
    @SerializedName("currencyName")
    val currencyName: String,
    @SerializedName("currencySymbol")
    val currencySymbol: String,
    @SerializedName("id")
    val id: String
)
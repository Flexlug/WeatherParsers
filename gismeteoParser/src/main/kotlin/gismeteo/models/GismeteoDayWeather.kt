package gismeteo.models

import java.time.LocalDate

data class GismeteoDayWeather (
    val date: LocalDate,
    val maxTemp: Int,
    val minTemp: Int,
    val windSpeed: Int,
    val precipitation: Float
)
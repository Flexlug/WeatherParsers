package rp5.models

import java.time.LocalDate

data class Rp5DayWeather (
    val date: LocalDate,
    val hours: List<Int>,
    val temperature: List<Int>,
    val humidity: List<Int>,
    val pressure: List<Int>,
    val wind: List<Int>,
    val windDirection: List<String>
)
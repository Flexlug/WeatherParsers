package gismeteo.models

class GismeteoCityWeather(
    val name: String,
    val weather: List<GismeteoDayWeather>
)
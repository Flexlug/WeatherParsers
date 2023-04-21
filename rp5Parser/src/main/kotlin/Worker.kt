import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import rp5.Rp5Parser
import rp5.models.Rp5CityWeather
import java.lang.Thread.sleep
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.hours

class Worker(val path: String) {
    private val EXECUTE_PARSE_DELAY = 60000L
    private val EACH_CITY_PARSE_DELAY = 2000L

    fun infiniteLoop() {
        // parse cities initially
        parseCities()

        val delay = 3.hours

        var lastParseDate = Clock.System.now()
        while (true) {
            val currentDateTime = Clock.System.now()

            if ((currentDateTime - lastParseDate) > delay) {
                parseCities()
                lastParseDate = currentDateTime
            } else {
                val nextParseTime = lastParseDate + delay - currentDateTime
                println("Next parse is after $nextParseTime")
                Thread.sleep(EXECUTE_PARSE_DELAY)
            }
        }
    }

    fun parseCities(singleCity: Boolean = false) {
        val parser = Rp5Parser()
        val popularCitiesUrls = parser.getPopularCites()

        println("got ${popularCitiesUrls.count()} urls")

        val cityWeathers = mutableListOf<Rp5CityWeather>()

        var cityProgress = popularCitiesUrls.count()
        for (city in popularCitiesUrls) {
            sleep(EACH_CITY_PARSE_DELAY)
            try {
                val cityForecast = parser.getCityInfoForWeek(city)
                cityWeathers.add(cityForecast)
            } catch (e: IllegalArgumentException) {
                println("Parse error: ${e.message}")
            } catch (e: Exception) {
                println("Unexpected error: ${e.message}")
            }

            println("Got weather for ${city.name}, ${--cityProgress} remains")

            if (singleCity) {
                break
            }
        }

        _writeToCsv(cityWeathers)
    }


    private fun _writeToCsv(cities: List<Rp5CityWeather>) {
        val currTime = Clock.System.now().toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
        val strDate = currTime.date.toString()
        val strTime = currTime.time
            .toString()
            .replace(':', '_')

        println("Writing weather to csv")

        val csvPath = Path(path)
        if (!csvPath.exists()) {
            csvPath.createDirectory()
        }

        val csvFilePath = csvPath.resolve("$strDate-$strTime.csv")

        println(csvFilePath.toAbsolutePath())

        csvWriter().open(csvFilePath.toAbsolutePath().toString()) {
            for (city in cities) {
                for (weather in city.weather) {
                    val hoursCount = weather.hours.count()
                    for (weatherHour in 0 until hoursCount) {
                        writeRow(
                            city.name,
                            weather.date,
                            weather.hours[weatherHour],
                            weather.temperature[weatherHour],
                            weather.humidity[weatherHour],
                            weather.pressure[weatherHour],
                            weather.wind[weatherHour],
                            weather.windDirection[weatherHour],
                        )
                    }
                }
            }
        }
    }
}
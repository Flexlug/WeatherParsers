import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import gismeteo.GismeteoParser
import gismeteo.models.GismeteoCityWeather
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class Worker(private val path: String) {
    private val EXECUTE_PARSE_DELAY = 60000L
    private val EACH_CITY_PARSE_DELAY = 2000L

    fun infiniteLoop() {

        // parse them initially
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
        val parser = GismeteoParser()
        val popularCitiesUrls = parser.getPopularCites()

        println("got ${popularCitiesUrls.count()} urls")

        val cityWeathers = mutableListOf<GismeteoCityWeather>()

        var cityProgress = popularCitiesUrls.count()
        for (city in popularCitiesUrls) {
            Thread.sleep(EACH_CITY_PARSE_DELAY)
            try {
                val weather = parser.getCityInfoFor2Weeks(city)
                cityWeathers.add(weather)
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

        writeToCsv(cityWeathers)
    }

    fun parseSpecificCity(cityName: String) {
        val parser = GismeteoParser()
        val specifiedCity = parser.getPopularCites()
                .find {
                    it.name == cityName
                }

        if (specifiedCity == null) {
            println("Couldn't find specified city: $cityName")
            return
        }

        val cityWeathers = mutableListOf<GismeteoCityWeather>()
        try {
            val weather = parser.getCityInfoFor2Weeks(specifiedCity)
            cityWeathers.add(weather)
        } catch (e: IllegalArgumentException) {
            println("Parse error: ${e.message}")
        } catch (e: Exception) {
            println("Unexpected error: ${e.message}")
        }

        writeToCsv(cityWeathers)
    }

    fun writeToCsv(cities: List<GismeteoCityWeather>) {
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
                    writeRow(
                            city.name,
                            weather.date,
                            weather.maxTemp,
                            weather.minTemp,
                            weather.windSpeed,
                            weather.precipitation,
                    )
                }
            }
        }
    }
}
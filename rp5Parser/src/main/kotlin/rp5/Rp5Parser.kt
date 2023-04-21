package rp5

import com.xenomachina.argparser.InvalidArgumentException
import rp5.models.Rp5CityInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import rp5.models.Rp5CityWeather
import rp5.models.Rp5DayWeather
import java.io.File
import java.time.LocalDate

class Rp5Parser {
    private val POPULAR_CITIES_TABLE_URL = "https://rp5.ru/Погода_в_Московской_области"
    private val BASE_LINK = "https://rp5.ru"

    fun getPopularCites(): List<Rp5CityInfo> {
        val xpathQuery = "//div[@class='countryMap']/div/h3/a[@class='href20']"

        val popularCitiesDocument = Jsoup.connect(POPULAR_CITIES_TABLE_URL).get()
        val popularCitiesUrls = (popularCitiesDocument.selectXpath(xpathQuery) ?: listOf())
            .map {
                Rp5CityInfo(
                    it.text(),
                    BASE_LINK + it.attr("href")
                )
            }

        return popularCitiesUrls
    }

    fun getCityInfoForWeek(city: Rp5CityInfo): Rp5CityWeather {
        val cityInfoDocument = Jsoup.connect(city.url).get()

        val cityForecastData = mutableMapOf<String, List<*>>().apply {
            set(LOCAL_TIME_ID, _getLocalTimes(cityInfoDocument))
            set(FACT_TEMPERATURES_ID, _getFactTemperatures(cityInfoDocument))
            set(HUMIDITIES_ID, _getHumidities(cityInfoDocument))
            set(PRESSURES_ID, _getPressures(cityInfoDocument))
            set(WIND_SPEEDS_ID, _getWindSpeeds(cityInfoDocument))
            set(WIND_DIRECTIONS_ID, _getWindDirections(cityInfoDocument))
        }

        // Мы можем получить полную прогностическую информацию только на завтрашний день
        // На сегодняшний же день мы можем получить только частичную информацию
        val entriesCountEachDay = _getEntriesCountEachDay(cityInfoDocument)

        // Валидация
        // Количество элементов в каждой строке должно быть одинаковым
        val totalEntriesCount = entriesCountEachDay.sum()
        if (cityForecastData.any {
                if (it.value.count() != totalEntriesCount) {
                    println("${it.key} has ${it.value.count()} values, but $totalEntriesCount expected!")
                    return@any true
                }
                return@any false
            }) {
            throw InvalidArgumentException("Some of fields does not contain the same amount of elements")
        }

        val forecastbyDays = mutableListOf<Rp5DayWeather>()

        var entriesPassed = 0
        for (day in 0 until entriesCountEachDay.count()) {
            val entriesCount = entriesCountEachDay[day]

            val dayForecast = Rp5DayWeather(
                LocalDate.now().plusDays(day.toLong()),
                (cityForecastData[LOCAL_TIME_ID] as List<Int>).drop(entriesPassed).take(entriesCount),
                (cityForecastData[FACT_TEMPERATURES_ID] as List<Int>).drop(entriesPassed).take(entriesCount),
                (cityForecastData[HUMIDITIES_ID] as List<Int>).drop(entriesPassed).take(entriesCount),
                (cityForecastData[PRESSURES_ID] as List<Int>).drop(entriesPassed).take(entriesCount),
                (cityForecastData[WIND_SPEEDS_ID] as List<Int>).drop(entriesPassed).take(entriesCount),
                (cityForecastData[WIND_DIRECTIONS_ID] as List<String>).drop(entriesPassed).take(entriesCount)
            )
            forecastbyDays.add(dayForecast)

            entriesPassed += entriesCount
        }

        return Rp5CityWeather(city.name, forecastbyDays)
    }

    private fun _getLocalTimes(city: Document): List<Int> {
        val xPathQuery = "//table[@id='forecastTable_1_3']/tbody/tr[@class='forecastTime']" +
                "/td[@class='n underlineRow' " +
                "or @class='n2 underlineRow' " +
                "or @class='d underlineRow' " +
                "or @class='d2 underlineRow']"

        return city.selectXpath(xPathQuery)
            .map {
                it.text().toInt()
            }
    }

    private fun _getFactTemperatures(city: Document): List<Int> {
        val xpathQuery = "//table[@id='forecastTable_1_3']/tbody" +
                "/tr[.//a[@class='t_temperature']]/td[contains(@class, 'n underlineRow toplineRow') " +
                "or contains(@class, 'n2 underlineRow toplineRow') " +
                "or contains(@class, 'd underlineRow toplineRow') " +
                "or contains(@class, 'd2 underlineRow toplineRow')]" +
                "/div[@class='t_0']/b"
        val temperatureElements = city.selectXpath(xpathQuery)

        return _parseTemperatureElements(temperatureElements)
    }

    private fun _getHumidities(city: Document): List<Int> {
        val xpathQuery = "//table[@id='forecastTable_1_3']/tbody/tr[.//a[contains(@title, 'влажность')]]" +
                "/td[contains(@class, 'n underlineRow') " +
                "or contains(@class, 'n2 underlineRow') " +
                "or contains(@class, 'd underlineRow') " +
                "or contains(@class, 'd2 underlineRow')]"

        return city.selectXpath(xpathQuery).map {
            it.text().toInt()
        }
    }

    private fun _getWindSpeeds(city: Document): List<Int> {
        val xPathQuery = "//table[@id='forecastTable_1_3']/tbody/tr[.//a[@class='t_wind_velocity']]" +
                "/td[contains(@class, 'wn') or contains(@class, 'wd')]/div[contains(@class, 'wv_0')]"

        val xPathQueryEmpty = "//table[@id='forecastTable_1_3']/tbody/tr[.//a[@class='t_wind_velocity']]" +
                "/td[(contains(@class, 'wn') or contains(@class, 'wd')) and (count(*)=0)]"


        val windElements = mutableListOf<Element>().apply {
            addAll(city.selectXpath(xPathQuery))
            addAll(city.selectXpath(xPathQueryEmpty))
        }

        return windElements.map {
            it.text().toInt()
        }
    }

    private fun _getWindDirections(city: Document): List<String> {
        val xPathQuery = "//table[@id='forecastTable_1_3']/tbody/tr[.//td[contains(text(), 'направление')]]" +
                "/td[contains(@class, 'grayLittle')]"

        return city.selectXpath(xPathQuery).map {
            it.text()
        }
    }

    private fun _getPressures(city: Document): List<Int> {
        val xpathQueryWithB = "//table[@id='forecastTable_1_3']/tbody/tr[.//a[@class='t_pressure']]" +
                "/td[contains(@class, 'n underlineRow') " +
                "or contains(@class, 'n2 underlineRow') " +
                "or contains(@class, 'd underlineRow') " +
                "or contains(@class, 'd2 underlineRow')]" +
                "/div[@class='p_0']/b"

        val xpathQueryWithoutB = "//table[@id='forecastTable_1_3']/tbody/tr[.//a[@class='t_pressure']]" +
                "/td[contains(@class, 'n underlineRow') " +
                "or contains(@class, 'n2 underlineRow') " +
                "or contains(@class, 'd underlineRow') " +
                "or contains(@class, 'd2 underlineRow')]" +
                "/div[@class='p_0' and count(b)=0]"

        val pressureElements = mutableListOf<Element>().apply{
            addAll(city.selectXpath(xpathQueryWithB))
            addAll(city.selectXpath(xpathQueryWithoutB))
        }

        return pressureElements.map {
            it.text().toInt()
        }
    }

    private fun _parseTemperatureElements(elements: Elements): List<Int> = elements
        .map {
            it.text()
        }
        .map {
            when (it[0]) {
                '-' -> it.replace("<span class=\"otstup\"></span>", "").toInt() * -1
                '+' -> it.replace("<span class=\"otstup\"></span>", "").toInt()
                '0' -> 0
                else -> throw IllegalArgumentException("This is not temperature - $it")
            }
        }

    private fun _getEntriesCountEachDay(city: Document): List<Int> {
        val xPathQuery = "//table[@id='forecastTable_1_3']/tbody/tr[@class='forecastDate']/td"

        val entries = city.selectXpath(xPathQuery).map {
            it.attr("colspan").toInt()
        }.toMutableList()

        // Первый и последний элемент включают также столбцы с инфорацией по строкам
        entries[0]--
        entries[entries.count() - 1]--

        return entries
    }

//    private fun _getPrecitipationValues(city: Document): List<Float> {
//        val xpathQuery = "//div[@data-row='precipitation-bars']/div[@class='row-item']/div[@class='item-unit' or @class='item-unit unit-blue']"
//        val precipitationElements = city.selectXpath(xpathQuery)
//        val precipitationValues = precipitationElements.map {
//            // replace нужен т.к. разделителем целой и дробной части на сайте является запятая
//            it.text().replace(',', '.').toFloat()
//        }
//
//        return precipitationValues
//    }
//
//
//    private fun _getFeelsLikeTemperatures(city: Document): List<Int> {
//        val xpathQuery = "//table[@id='forecastTable_1_3']/tbody/tr[7]/td[contains(@class, 'n underlineRow toplineRow') " +
//                "or contains(@class, 'n2 underlineRow toplineRow') " +
//                "or contains(@class, 'd2 underlineRow toplineRow')]/div[@class='t_0']/b"
//        val temperatureElements = city.selectXpath(xpathQuery)
//
//        return _parseTemperatureElements(temperatureElements)
//    }

//    private fun _getFogChance(city: Document): List<Int> {
//        val xPathQuery = "//table[@id='forecastTable_1_3']/tbody/tr[.//a[contains(@title, 'туман')]]" +
//                "/td[@class='n' or @class='n2' or @class='d2']/div"
//        val fogRegex = Regex("(\\d*)%")
//
//        return city.selectXpath(xPathQuery)
//            .map {
//                val fogAttr = if (it.hasAttr("onmouseover")) it.attr("onmouseover") else return@map 0
//
//                val fogGroupValues = fogRegex.find(fogAttr)?.groupValues
//                    ?: throw IllegalArgumentException("Couldn't find fog chance element via regex. Text: $fogAttr")
//
//                if (fogGroupValues.isEmpty()) {
//                    throw IllegalArgumentException("Couldn't find cloudiness element via regex. GroupCollection is empty. Text: $fogAttr")
//                }
//
//                // Нулевой элемент - это match, а не group
//                fogGroupValues[1].toInt()
//            }
//    }

    companion object {
        val LOCAL_TIME_ID = "LOCAL_TIME"
        val FACT_TEMPERATURES_ID = "FACT_TEMPERATURES"
        val HUMIDITIES_ID = "HUMIDIITES"
        val PRESSURES_ID = "PRESSURES"
        val WIND_SPEEDS_ID = "WIND_SPEEDS"
        val WIND_DIRECTIONS_ID = "WIND_DIRECTIONS"
    }
}
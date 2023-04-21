package gismeteo

import gismeteo.models.GismeteoCityInfo
import gismeteo.models.GismeteoCityWeather
import gismeteo.models.GismeteoDayWeather
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.time.LocalDate

class GismeteoParser {
    private val POPULAR_CITIES_TABLE_URL = "https://www.gismeteo.ru/catalog/russia/moscow-oblast/"
    private val BASE_LINK = "https://www.gismeteo.ru"

    fun getPopularCites(): List<GismeteoCityInfo> {
        val popularCitiesDocument = Jsoup.connect(POPULAR_CITIES_TABLE_URL).get()
        val popularCitiesUrls = (popularCitiesDocument.selectXpath("//a[@class='link-item link-popular']") ?: listOf())
            .map {
                GismeteoCityInfo(
                    it.text(),
                    BASE_LINK + it.attr("href")
                )
            }

        return popularCitiesUrls
    }

    fun getCityInfoFor2Weeks(city: GismeteoCityInfo): GismeteoCityWeather {
        val cityLink = city.url + "2-weeks"
        val cityInfoDocument = Jsoup.connect(cityLink).get()

        val maxTemperatures = _getMaxTemperatures(cityInfoDocument)
        val minTemperatures = _getMinTemperatures(cityInfoDocument)
        val windSpeeds = _getWindSpeeds(cityInfoDocument)
        val precipitationValues = _getPrecitipationValues(cityInfoDocument)

        // Валидация
        // Должно быть 14 значений на каждый день по каждому из полей
        if (maxTemperatures.count() != 14)
            throw IllegalArgumentException("Couldn't get max temperatures array length for 2 weeks. Got length: ${maxTemperatures.count()}")

        if (minTemperatures.count() != 14)
            throw IllegalArgumentException("Couldn't get min temperatures array length for 2 weeks. Got length: ${minTemperatures.count()}")

        if (windSpeeds.count() != 14)
            throw IllegalArgumentException("Couldn't get wind speeds array length for 2 weeks. Got length: ${windSpeeds.count()}")

        if (precipitationValues.count() != 14)
            throw IllegalArgumentException("Couldn't get precipitation values array length for 2 weeks. Got length: ${precipitationValues.count()}")

        val days = mutableListOf<GismeteoDayWeather>()
        for (i in 0 until 14) {
            days.add(
                GismeteoDayWeather(
                    LocalDate.now().plusDays(i.toLong()),
                    maxTemperatures[i],
                    minTemperatures[i],
                    windSpeeds[i],
                    precipitationValues[i]
                )
            )
        }

        return GismeteoCityWeather(city.name, days)
    }

    private fun _getMinTemperatures(city: Document): List<Int> {
        val xpathQuery = "//div[@data-row='temperature-air']/div/div[@class='values']/div/div[@class='mint']/span[@class='unit unit_temperature_c']"
        val temperatureElements = city.selectXpath(xpathQuery)

        return _parseTemperatureElements(temperatureElements)
    }

    private fun _getMaxTemperatures(city: Document): List<Int> {
        val xpathQuery = "//div[@data-row='temperature-air']/div/div[@class='values']/div/div[@class='maxt']/span[@class='unit unit_temperature_c']"
        val temperatureElements = city.selectXpath(xpathQuery)

        return _parseTemperatureElements(temperatureElements)
    }

    private fun _parseTemperatureElements(elements: Elements): List<Int> = elements
        .map {
            it.text()
        }
        .filter {
            // По какой-то причине здесь всплывает этот элемент: <span class="unit unit_temperature_c">°C</span>
            it != "°C"
        }
        .map {
            when (it[0]) {
                '−' -> it.substring(1).toInt() * -1
                '+' -> it.substring(1).toInt()
                '0' -> 0
                else -> throw IllegalArgumentException("This is not temperature - $it")
            }
        }

    private fun _getWindSpeeds(city: Document): List<Int> {
        val xPathItemsQuery = "//div[@data-row='wind-gust']/div[@class='row-item']"
        val xPathSpanQuery = "//span[@class='wind-unit unit unit_wind_m_s']"
        val windValues = city.selectXpath(xPathItemsQuery)
            .map {
                if (it.childrenSize() == 3) {
                    it.selectXpath(xPathSpanQuery)
                        .first()
                        ?.text()
                        ?.toInt() ?: throw IllegalArgumentException("Couldn't parse wind row item")
                } else {
                    0
                }
            }

        return windValues
    }

    private fun _getPrecitipationValues(city: Document): List<Float> {
        val xpathQuery = "//div[@data-row='precipitation-bars']/div[@class='row-item']/div[@class='item-unit' or @class='item-unit unit-blue']"
        val precipitationElements = city.selectXpath(xpathQuery)
        val precipitationValues = precipitationElements.map {
            // replace нужен т.к. разделителем целой и дробной части на сайте является запятая
            it.text().replace(',', '.').toFloat()
        }

        return precipitationValues
    }
}
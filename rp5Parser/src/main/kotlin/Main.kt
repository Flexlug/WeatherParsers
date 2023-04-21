import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.xenomachina.argparser.ArgParser
import rp5.models.Rp5CityWeather
import rp5.Rp5Parser
import java.lang.Thread.sleep
import java.time.LocalDate
import java.time.LocalTime

fun main(args: Array<String>) {

    ArgParser(args).parseInto(::MainArgs).run {
        val worker = Worker(path)

        if (singleCity) {
            println("--single flag provided. Only one successfully parsed city will be written to .csv")
            worker.parseCities(singleCity = true)
            return
        }

        if (inf) {
            println("--inf flag provided. This program will run infinitely until stopped manually")
            worker.infiniteLoop()
            return
        }

        worker.parseCities()
    }
}
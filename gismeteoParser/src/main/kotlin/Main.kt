import com.xenomachina.argparser.ArgParser

fun main(args: Array<String>) {
    ArgParser(args).parseInto(::MainArgs).run {
        val worker = Worker(path)

        if (city.isNotBlank()) {
            worker.parseSpecificCity(city)
            return
        }

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

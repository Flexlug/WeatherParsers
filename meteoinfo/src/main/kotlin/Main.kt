import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.datetime.Clock
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import java.time.LocalTime
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.hours

val url = "https://meteoinfo.ru/hmc-output/observ/obs_arch.php"

fun String.utf8(): String = URLEncoder.encode(this, "UTF-8")

fun DateTimeUnix(time:String):Long {
    val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
    val currentDate = sdf.parse(time)
    println(currentDate)

    val unix = currentDate.time
    return unix
}

data class Xitoto(
    val datata: MutableList<String>,
    val timing: String)


fun readXml(): Xitoto {
    val xmlFile = File("items.xml")

    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
    doc.documentElement.normalize()
    val bookList: NodeList = doc.getElementsByTagName("table")
    val line = mutableListOf<String>()
    var l1: String = ""

    for (i in 0..bookList.length - 1) {
        var bookNode: Node = bookList.item(i)

        if (bookNode.getNodeType() === Node.ELEMENT_NODE) {

            val elem = bookNode as Element


            val mMap = mutableMapOf<String, String>()


            for (j in 0..elem.attributes.length - 1) {
                mMap.putIfAbsent(elem.attributes.item(j).nodeName, elem.attributes.item(j).nodeValue)
            }


            for (i in 1..4) {
                l1 = "${elem.getElementsByTagName("tr").item(0).textContent}"

                val l2 = "${elem.getElementsByTagName("tr").item(i).textContent}".split(' ')

                line.add(l2.last())

            }


        }
    }

    return Xitoto(line, l1)
}


fun Parser_Site(id_city: String,dt:String) {
    val params = mapOf("lang" to "ru-RU", "id_city" to "${id_city}", "dt" to "${dt}", "has_db" to "1", "dop" to "0")
    val urlParams = params.map { (k, v) -> "${(k.utf8())}=${v.utf8()}" }
        .joinToString("&")
    val client = HttpClient.newBuilder().build();
    val request = HttpRequest.newBuilder()
        .uri(URI.create("${url}?${urlParams}"))
        .build();
    val response = client.send(request, HttpResponse.BodyHandlers.ofString());
    var response_body = response.body()
    // response_body.replace("\"\\","\"")
    response_body = response_body.replace("\\\"", "\"")
    response_body = response_body.replace("&nbsp", "\\")
    response_body = response_body.replace("<nobr>", "")
    response_body = response_body.replace("&deg", "")
    response_body = response_body.replace("<br>", "")
    var i = response_body.indexOf("<a", 0)
    response_body = response_body.substring(i)
    response_body = response_body.dropLast(9)
    response_body = "<html>" + response_body + "</html>"
    File("items.xml").writeText(response_body)
}



fun TableRead(NumColumn: Integer) {
    val ValueXpath = "/html/body/table/tbody/tr[${NumColumn}]/td[2]"
    val NameXpath = "/html/body/table/tbody/tr[${NumColumn}]/td[1]"
}

fun writeToCsv(doc: MutableList<MutableList<String>>) {
    val csvPath = Path("data")
    if (!csvPath.exists()) {
        csvPath.createDirectory()
    }

    val csvFilePath = csvPath.resolve("test.csv")
    println("Writing weather to csv")

    csvWriter {
        charset = "UTF-8"
        delimiter = ','
    }.writeAll(doc, csvFilePath.toAbsolutePath().toString(), append = true)

}

fun worker() {
    val doc = mutableListOf<MutableList<String>>()
    println(DateTimeUnix("18/04/2023 00:00:00"))

    val ListId = listOf("1647", "1659")
    val TimeList = listOf("0")
    val City = mutableListOf("Vladimir", "Moscow")
    for (id in ListId) {
        Parser_Site(id, "0")
        val result = readXml()
        doc.add(result.datata)
        println(result.timing)
    }

    doc[0].add(City[0])
    doc[1].add(City[1])

    for (i in 0..1) {
        doc[i][0] = doc[i][0].replace("рт.ст.", "")
        doc[i][1] = doc[i][1].replace(";C", "")
        doc[i][2] = doc[i][2].replace("%", "")
        doc[i][3] = doc[i][3].replace("ветра", "")
        doc[i][3] = doc[i][3].replace("м/с", "")

    }
    println(doc)
    writeToCsv(doc)
}


val EXECUTE_PARSE_DELAY = 60000L

fun main(args: Array<String>) {
    worker()
    val delay = 3.hours

    var lastParseDate = Clock.System.now()
    while (true) {
        val currentDateTime = Clock.System.now()

        if ((currentDateTime - lastParseDate) > delay) {
            worker()
            lastParseDate = currentDateTime
        } else {
            val nextParseTime = lastParseDate + delay - currentDateTime
            println("Next parse is after $nextParseTime")
            Thread.sleep(EXECUTE_PARSE_DELAY)
        }
    }
}
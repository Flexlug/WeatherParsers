import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default

class MainArgs(parser: ArgParser) {
    val city by parser.storing(
        "-c", "--city",
        help = "Find and parse specific city"
    ).default<String>("")

    val singleCity by parser.flagging(
        "-S", "--single",
        help = "Parse only first town"
    )

    val path by parser.storing(
        "-P", "--path",
        help = "Path for .csv"
    ).default<String>("data")

    val inf by parser.flagging(
        "-I", "--inf",
        help = "Starts program in infinite loop. Parses all cities with hardcoded delay - 1 hour"
    )
}
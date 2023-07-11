package dslmaker

import org.antlr.v4.kotlinruntime.ANTLRInputStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import piacenti.dslmaker.antlr.generated.JSONLexer
import piacenti.dslmaker.antlr.generated.JSONParser
import piacenti.dslmaker.GenericParser
import piacenti.dslmaker.GenericParser2
import piacenti.dslmaker.dsl.JSONGrammar
import piacenti.dslmaker.dsl.XMLGrammar
import piacenti.dslmaker.dsl.XMLToken
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class LongParsingTest {
    private val grammar = XMLGrammar(XMLToken.START)
    private val parser = GenericParser(grammar)
    private val parser2 = GenericParser2(grammar)
    private val parser3 = GenericParser2(JSONGrammar())
    private val longXml = long

    @Test
    fun test_long_parsing() {
        parser.parse(longXml, true)
    }

    @Test
    fun test_long_parsing2() {
        parser2.parse(longXml, true)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun test_long_json_parsing2() {
        (0..10).forEach {
            println(measureTimedValue {
                parser3.parse(longer, true)
            }.duration.inWholeMilliseconds)
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun antlr_longer_parsing2() {
        (0..10).forEach {
            println(measureTimedValue {
                val jsonLexer = JSONLexer(ANTLRInputStream(longer))
                val jsonParser = JSONParser(CommonTokenStream(jsonLexer))
                jsonParser.json()
            }.duration.inWholeMilliseconds)
        }
    }
}
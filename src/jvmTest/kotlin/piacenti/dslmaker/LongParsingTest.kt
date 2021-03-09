package piacenti.dslmaker

import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.junit.Ignore
import org.junit.Test
import piacenti.dslmaker.antlr.generated.*
import piacenti.dslmaker.dsl.JSONGrammar
import piacenti.dslmaker.dsl.XMLGrammar
import piacenti.dslmaker.dsl.XMLToken.START
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


class LongParsingTest {
    private val grammar = XMLGrammar(START)
    private val parser = GenericParser(grammar)
    private val parser2 = GenericParser2(grammar)
    private val longXml = File("src/jvmTest/resources/large.xml").bufferedReader().readText()
    private val longerXml = File("src/jvmTest/resources/larger.xml").bufferedReader().readText()
    private val longerJSON = File("src/jvmTest/resources/generated.json").bufferedReader().readText()

    @Test
    fun test_long_parsing() {
        parser.parse(longXml, true)
    }

    @Test
    fun test_long_parsing2() {
        parser2.parse(longXml, true)
    }

    @Ignore
    @Test
    fun test_longer_parsing() {
        parser.parse(longerXml, true)
    }

    @Test
    fun test_longer_parsing2() {
        parser2.parse(longerXml, true)
    }

    @Ignore
    @Test
    fun test_longer_parsing3() {
        GenericParser2(JSONGrammar()).let { p ->
            (0..3).forEach { p.parse(longerJSON, true) }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    @Ignore
    fun antlr_longer_parsing3() {
        (0..10).forEach {
            println(measureTimedValue {
                val jsonLexer = JSONLexer(CharStreams.fromString(longerJSON))
                val jsonParser = JSONParser(CommonTokenStream(jsonLexer))
                jsonParser.json()
            }.duration.inMilliseconds)
        }
    }
    @Test
    fun mini(){
        val lexer=MiniLexer(CharStreams.fromString("stuff foo and bar thing or another one andocrino and and or or or or\n"))
        val tokenStream=CommonTokenStream(lexer)
        println(lexer.allTokens.joinToString("\n") { it.toString() })
        lexer.reset()
        val parser=MiniParser(tokenStream)
        val tree = parser.main()
        println(parser.numberOfSyntaxErrors)
        println(tree.toStringTree(parser))
    }

}
package piacenti.dslmaker.interfaces

import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.junit.Test
import piacenti.dslmaker.antlr.generated.XMLLexer
import piacenti.dslmaker.antlr.generated.XMLParser


internal class AntlrTreePrinterTest {

    @Test
    fun treeToString() {
        val lexer = XMLLexer(CharStreams.fromString("""
            <element>
<element2>
<element3>value</element3>
</element2>
</element>
        """.trimIndent()))
        val tokenStream = CommonTokenStream(lexer)
        println(lexer.allTokens.joinToString("\n") { it.toString() })
        lexer.reset()
        val parser = XMLParser(tokenStream)
        val tree = parser.document()
        println(parser.numberOfSyntaxErrors)
        println(tree.toStringTree(parser))
        println(tree.treeToString(parser))
    }
}
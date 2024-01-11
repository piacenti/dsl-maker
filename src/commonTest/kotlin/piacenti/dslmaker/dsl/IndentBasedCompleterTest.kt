package piacenti.dslmaker.dsl

import org.antlr.v4.kotlinruntime.ANTLRInputStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker
import piacenti.dslmaker.antlr.generated.IndentBasedLexer
import piacenti.dslmaker.antlr.generated.IndentBasedParser
import piacenti.dslmaker.antlr.generated.IndentBasedParser.Rules
import piacenti.dslmaker.antlr.generated.IndentBasedParser.Tokens
import piacenti.dslmaker.antlr.generated.IndentBasedParserBaseListener
import piacenti.dslmaker.dsl.antlr.AntlrAutoCompleter
import piacenti.dslmaker.dsl.antlr.AntlrCompletionData
import piacenti.dslmaker.validateEquals
import kotlin.test.Test
import org.antlr.v4.kotlinruntime.tree.ParseTree

enum class DataType {
    JSON, XML, UNKNOWN
}

class DataTypeResolver : IndentBasedParserBaseListener() {
    var type: DataType = DataType.UNKNOWN
    override fun enterDataPath(ctx: IndentBasedParser.DataPathContext) {
        val currentType = type
        type = if (ctx.text.replace(" ", "").startsWith("$"))
            DataType.JSON
        else
            DataType.XML
        if (currentType != DataType.UNKNOWN && currentType != type) {
            error("You cannot mix xpath and jsonpath in your query")
        }
    }

    override fun exitMain(ctx: IndentBasedParser.MainContext) {
        ctx.dataType = type
    }
}

class IndentBasedCompleterAntlr : AntlrAutoCompleter<IndentBasedParser>() {
    private lateinit var dataType: DataType
    override fun createParser(tokenStream: CommonTokenStream): IndentBasedParser {
        return IndentBasedParser(tokenStream)
    }

    override fun runStartRule(parser: IndentBasedParser): IndentBasedParser.MainContext {
        val main = parser.main()
        ParseTreeWalker().walk(DataTypeResolver(), main)
        dataType = main.dataType
        return main
    }

    override fun createLexer(text: String): Lexer {
        val indentBasedLexer = IndentBasedLexer(ANTLRInputStream(text))
        indentBasedLexer.dedentAtEnd = false
        return indentBasedLexer
    }

    override fun processSuggestions(antlrCompletionData: AntlrCompletionData, parseTree: ParseTree, completionFactory: (text: String, type: String) -> Completion): Set<Completion> {
        val result = mutableSetOf<Completion>()
        val ruleIds = antlrCompletionData.rules.map { it.id }
        val tokenIds = antlrCompletionData.tokens.map { it.id }
        result.addAll(ruleIds.mapNotNull {
            when (it) {
                Rules.alias.id -> {
                    listOf(completionFactory("as some header name", "alias"))
                }
                Rules.dataPath.id -> {
                    val jsonCompletion = completionFactory("$.someFiled.child[?(@.name=='something')].grandchild", "jsonpath")
                    val xmlCompletion = completionFactory("//someElement[name/text()='some value']/childElement", "xpath")
                    when (dataType) {
                        DataType.JSON -> listOf(jsonCompletion)
                        DataType.XML -> {
                            listOf(xmlCompletion)
                        }
                        else -> {
                            listOf(jsonCompletion, xmlCompletion)
                        }
                    }

                }
                Rules.print.id -> {
                    listOf(completionFactory("print\n", "action"))
                }
                Rules.names.id -> {
                    if (dataType == DataType.XML)
                        listOf(completionFactory("name\n", "action"))
                    else null
                }
                else -> null
            }
        }.flatten())
        result.addAll(tokenIds.mapNotNull {
            when (it) {
                Tokens.NEW_LINE.id -> {
                    completionFactory("\n", "new line")
                }
                Tokens.INDENT.id -> completionFactory("\t", "indent")
                Tokens.DEDENT.id -> completionFactory("", "dedent")
                else -> null
            }
        })

        return result
    }
}

class IndentBasedCompleterTest {
    private val script = """
//calendars
  id as calendar id
    print
    
  months
    id as month id
      print
    monthname
      print
    monthnumber
      print
    
    """.trimIndent()
    private val completer = IndentBasedCompleterAntlr()

    @Test
    fun complete_at_beginning() {
        val result = completer.suggestCompletion(script, 0, 0)
        result.size validateEquals 3
        result[0].type validateEquals "new line"
        result[1].type validateEquals "xpath"
        result[2].type validateEquals "jsonpath"
    }

    @Test
    fun complete_after_path() {
        val result = completer.suggestCompletion(script, 11, 11)
        result.size validateEquals 2
        result[0].type validateEquals "new line"
        result[1].type validateEquals "alias"
    }

    @Test
    fun complete_after_path_line_break() {
        val result = completer.suggestCompletion(script, 12, 12)
        result.size validateEquals 1
        result[0].type validateEquals "indent"
    }

    @Test
    fun complete_after_indent() {
        val result = completer.suggestCompletion(script, 14, 14)
        result.size validateEquals 5
        result[0].type validateEquals "dedent"
        result[1].type validateEquals "new line"
        result[2].type validateEquals "xpath"
        result[3].type validateEquals "action"
        result[4].type validateEquals "action"
    }


    @Test
    fun complete_after_inner_block_action() {
        val result = completer.suggestCompletion(script, 46, 46)
        result.size validateEquals 4
        result[0].type validateEquals "indent"
        result[1].type validateEquals "dedent"
        result[2].type validateEquals "new line"
        result[3].type validateEquals "xpath"
    }
}
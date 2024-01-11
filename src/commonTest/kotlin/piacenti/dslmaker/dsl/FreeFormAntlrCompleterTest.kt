package piacenti.dslmaker.dsl


import org.antlr.v4.kotlinruntime.ANTLRInputStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.misc.IntervalSet
import piacenti.dslmaker.antlr.generated.FreeFormLexer
import piacenti.dslmaker.antlr.generated.FreeFormParser
import piacenti.dslmaker.antlr.generated.FreeFormParser.Rules
import piacenti.dslmaker.antlr.generated.FreeFormParser.Tokens
import piacenti.dslmaker.dsl.antlr.AntlrAutoCompleter
import piacenti.dslmaker.dsl.antlr.AntlrCompletionData
import piacenti.dslmaker.validateEquals
import kotlin.test.Test
import org.antlr.v4.kotlinruntime.tree.ParseTree

fun IntervalSet.toList(): MutableList<Int> {
    val result = mutableListOf<Int>()
    for (i in 0 until this.size()) {
        result.add(this[i])
    }
    return result
}


class FreeFormCompleter2 : AntlrAutoCompleter<FreeFormParser>() {
    override fun createParser(tokenStream: CommonTokenStream): FreeFormParser {
        return FreeFormParser(tokenStream)
    }

    override fun runStartRule(parser: FreeFormParser): FreeFormParser.MainContext {
        return parser.main()
    }

    override fun createLexer(text: String): Lexer {
        return FreeFormLexer(ANTLRInputStream(text))
    }

    override fun processSuggestions(antlrCompletionData: AntlrCompletionData, parseTree: ParseTree, completionFactory: (text: String, type: String) -> Completion): Set<Completion> {
        val result = mutableSetOf<Completion>()
        val ruleIds = antlrCompletionData.rules.map { it.id }
        val tokenIds = antlrCompletionData.tokens.map { it.id }
        result.addAll(ruleIds.mapNotNull { ruleId ->
            when (ruleId) {
                Rules.value.id -> {
                    listOf("free form text", "'single quoted text'", "\"double quoted text\":").map { completionFactory(it, "value") }
                }
                Rules.type.id -> listOf("codes:", "verbalizations:", "objects:").map { completionFactory(it, "keyword") }
                Rules.criteria.id -> {
                    listOf(completionFactory("criteria\n", "keyword"))
                }
                Rules.and.id -> listOf(completionFactory(" and ", "logical operator"))
                Rules.or.id -> listOf(completionFactory(" or ", "logical operator"))
                else -> null
            }
        }.flatten())
        result.addAll(tokenIds.mapNotNull {
            when (it) {
                Tokens.NEW_LINE.id -> {
                    if (!ruleIds.contains(Rules.criteria.id))
                        completionFactory("\n", "new line")
                    else null
                }
                Tokens.OPEN.id -> completionFactory(" ( ", "start of logical block")
                Tokens.CLOSE.id -> completionFactory(" ) ", "end of logical block")
                else -> null
            }
        })
        return result
    }
}

class FreeFormCompleterTest {
    private val text: String = """
criteria
    codes: boingons thing and foo and ( 12345 or 52345 or  62345 or 72345 or  82345 or  92345 or  02345) 
            """.trimIndent()

    @Test
    fun test_completion_from_beginning() {
        val result = FreeFormCompleter2().suggestCompletion(text, 0, 0)
        result.size validateEquals 1
        result.first().completionText validateEquals """criteria
"""
    }
    @Test
    fun test_completion_after_criteria() {
        val result = FreeFormCompleter2().suggestCompletion(text, 8, 8)
        result.size validateEquals 1
        result.first().completionText validateEquals "\n"

    }
    @Test
    fun test_completion_type() {
        val result = FreeFormCompleter2().suggestCompletion(text, 12, 12)
        result.size validateEquals 3
        result[2].completionText validateEquals "codes:"

    }
    @Test
    fun test_completion_partway_typed_token() {
        val result =  FreeFormCompleter2().suggestCompletion(text, 12, 17)
        result.size validateEquals 3
        result[0].completionText validateEquals "codes:"
    }

    @Test
    fun test_completion_middle_of_type() {
        val result = FreeFormCompleter2().suggestCompletion(text, 15, 15)
        result.size validateEquals 3
        result[2].completionText validateEquals "codes:"
    }

    @Test
    fun test_completion_value_after_colon() {
        val result = FreeFormCompleter2().suggestCompletion(text, 19, 19)
        result.size validateEquals 4
        result.filter {
            it.type == "value"
        }.count() validateEquals 3
        result.filter {
            it.type == "start of logical block" && it.completionText == " ( "
        }.count() validateEquals 1
    }

    @Test
    fun test_completion_value_after_spaces_after_colon() {
        val result = FreeFormCompleter2().suggestCompletion(text, 20, 20)
        result.size validateEquals 4
        result.filter {
            it.type == "value"
        }.count() validateEquals 3
        result.filter {
            it.type == "start of logical block" && it.completionText == " ( "
        }.count() validateEquals 1
    }

    @Test
    fun test_completion_logical_operator_at_end_of_value() {
        val result = FreeFormCompleter2().suggestCompletion(text, 28, 28)
        result.size validateEquals 3
        result.forEach {
            (it.type in listOf("logical operator", "new line")) validateEquals true
        }
    }

    @Test
    fun test_completion_logical_operator_after_space_after_value() {
        val result = FreeFormCompleter2().suggestCompletion(text, 29, 29)
        result.size validateEquals 3
        result.forEach {
            (it.type in listOf("logical operator", "new line")) validateEquals true
        }
    }

    @Test
    fun test_completion_logical_operator_inside_known_block() {
        var result = FreeFormCompleter2().suggestCompletion(text, 41, 41)
        result.size validateEquals 2
        result.last().apply {
            type validateEquals "logical operator"
            completionText validateEquals " and "
        }
        result = FreeFormCompleter2().suggestCompletion(text, 54, 54)
        result.size validateEquals 1
        result.first().apply {
            type validateEquals "logical operator"
            completionText validateEquals " or "
        }
    }

    @Test
    fun test_completion_close_parenthesis_should_be_suggested_after_at_least_two_terms_in_parenthesised_block() {
        val result = FreeFormCompleter2().suggestCompletion(text, 64, 64)
        result.size validateEquals 2
        result.first().apply {
            type validateEquals "end of logical block"
            completionText validateEquals " ) "
        }
        result.last().apply {
            type validateEquals "logical operator"
            completionText validateEquals " or "
        }
    }

    @Test
    fun test_completion_logical_operator_right_after_logical_operator() {
        val result = FreeFormCompleter2().suggestCompletion(text, 38, 38)
        result.size validateEquals 4
        result.filter {
            it.type == "start of logical block" &&
                    it.completionText == " ( "
        }.count() validateEquals 1
        result.filter {
            it.type == "value"
        }.count() validateEquals 3
    }
    @Test
    fun test_completion_start_of_replace_as_text_index() {
        val result = FreeFormCompleter2().suggestCompletion("""
criteria 
    codes: some and thing and (stuff or thing)
criteria
    objects: thing  more stuff or things
    obje
        """.trimIndent(), 115, 115)
        result.size validateEquals 4
        result.first().startOfReplace validateEquals 111
    }
    @Test
    fun test_completion_start_of_replace_as_text_index_from_new_line() {
        val result = FreeFormCompleter2().suggestCompletion("""
criteria 
    codes: some and thing and (stuff or thing)
criteria
    objects: thing  more stuff or things
""".trimStart(), 107, 107)
        result.size validateEquals 4
        result.first().startOfReplace validateEquals 107
    }
}
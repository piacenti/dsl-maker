package piacenti.dslmaker.dsl

import org.antlr.v4.kotlinruntime.ANTLRInputStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import piacenti.dslmaker.DirectAndIndirectLeftRecursiveExpressionGrammar
import piacenti.dslmaker.ExpressionInventory
import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.dsl.XMLToken.ATTRIBUTE
import piacenti.dslmaker.dsl.XMLToken.ELEMENT_END
import piacenti.dslmaker.dsl.XMLToken.ELEMENT_START
import piacenti.dslmaker.dsl.XMLToken.NAME
import piacenti.dslmaker.dsl.XMLToken.alphanumericSequence
import piacenti.dslmaker.dsl.XMLToken.greaterThan
import piacenti.dslmaker.dsl.XMLToken.lessThan
import piacenti.dslmaker.dsl.XMLToken.value
import piacenti.dslmaker.dsl.antlr.AntlrCompletionSuggester
import piacenti.dslmaker.structures.ASTNode
import piacenti.dslmaker.validateEquals
import kotlin.test.Test
import piacenti.dslmaker.antlr.generated.JSONLexer
import piacenti.dslmaker.antlr.generated.JSONParser

class XMLCompleter : GrammarBasedAutoCompleter(XMLGrammar()) {
    override fun stepToCompletions(step: ProductionStep, completionFactory: (text: String, type: String) -> Completion): List<Completion> {
        val result = mutableListOf<Completion>()
        when (step) {
            lessThan, ELEMENT_START, ELEMENT_END -> result.add(completionFactory("<", "less than"))
            greaterThan -> result.add(completionFactory(">", "less than"))
            NAME, ATTRIBUTE, alphanumericSequence -> result.add(completionFactory("alphanumeric", "alphanumeric sequence"))
            value -> result.add(completionFactory("some element value", "value"))
        }
        return result
    }
}

class ExpressionCompleter : GrammarBasedAutoCompleter(DirectAndIndirectLeftRecursiveExpressionGrammar(ExpressionInventory.EXPRESSION)) {
    override fun stepToCompletions(step: ProductionStep, completionFactory: (text: String, type: String) -> Completion): List<Completion> {
        val result = mutableListOf<Completion>()
        ExpressionInventory.apply {
            when (step) {
                NUMBER -> result.add(completionFactory("0-10", "number"))
                MULTIPLICATION, ADDITION, SUBTRACTION, DIVISION -> result.add(completionFactory("+,-,*,/", "operator"))
                OPEN_PARENTHESIS -> result.add(completionFactory("(", "start of expression grouping"))
                CLOSE_PARENTHESIS -> result.add(completionFactory(")", "end of expression grouping"))
            }
        }
        return result
    }
}

class AntlrCompleter : AutoCompleter {
    @OptIn(ExperimentalStdlibApi::class)
    override fun suggestCompletion(text: String, startOfCompletionCaretPosition: Int, currentCaretPosition: Int, astBasedAction: ((result: MutableSet<Completion>, ast: ASTNode) -> Unit)?): List<Completion> {
        val evalText = text.substring(0, startOfCompletionCaretPosition)
        val lexer = JSONLexer(ANTLRInputStream(evalText))
        val tokenStream = CommonTokenStream(lexer)
        val parser = JSONParser(tokenStream)
         parser.json()
        val curry = { completionText: String, description: String -> Completion(completionText, text, startOfCompletionCaretPosition, currentCaretPosition,description) }
        val result = mutableListOf<Completion>()
        val suggestCompletions = AntlrCompletionSuggester().suggestCompletions(parser)
        result.addAll(suggestCompletions.tokens.map { it.id }.mapNotNull {
            when (it) {
                JSONParser.Tokens.STRING -> curry("\"some value\"", "value")
                else -> null
            }
        })
        result.addAll(suggestCompletions.rules.map { it.id }.mapNotNull {
            when (it) {
                JSONParser.Rules.Arr -> curry("[]", "array")
                JSONParser.Rules.Obj -> curry("{}", "object")
                else -> null
            }
        })
        return result
    }
}

class CompleterTest {
    @Test
    fun antlr_completer() {
        val pos = 13
        val suggestCompletion = AntlrCompleter().suggestCompletion("""{"something":"another"}""", pos, pos)
        suggestCompletion.size validateEquals 3
        suggestCompletion[0].type validateEquals "value"
        suggestCompletion[1].type validateEquals "object"
        suggestCompletion[2].type validateEquals "array"
    }

    @Test
    fun complete_on_failure() {
        XMLCompleter().suggestCompletion("""
<element>
    <element>value</element>
</element>
        """.trimIndent().trim(), 9, 9).let {
            it.first().type validateEquals "value"
            it.last().type validateEquals "less than"
        }
    }

    @Test
    fun complete_on_success_for_recursive_grammars() {
        ExpressionCompleter().suggestCompletion("5+6-7*9/10", 1, 1).let {
            it.first().type validateEquals "operator"
        }
    }

}
package piacenti.dslmaker.dsl

import kotlinx.serialization.Serializable
import piacenti.dslmaker.GenericParser2
import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.errors.ParserException
import piacenti.dslmaker.structures.ASTNode
import piacenti.dslmaker.structures.Grammar

@Serializable
data class Range<T>(val value: T?, val start: Int, val end: Int)

@Serializable
data class Completion(val completionText: String, val text: String, val startOfReplace: Int, val caretPosition: Int, val type: String? = null, val completionCustomAction: ((Range<String>, Completion) -> Unit)? = null)

interface AutoCompleter {
    fun suggestCompletion(text: String,
                          startOfCompletionCaretPosition: Int,
                          currentCaretPosition: Int = startOfCompletionCaretPosition,
                          astBasedAction: ((result: MutableSet<Completion>, ast: ASTNode) -> Unit)? = null): List<Completion>
}

abstract class GrammarBasedAutoCompleter(val grammar: Grammar<*>) : AutoCompleter {
    private val parser = GenericParser2(grammar)
    abstract fun stepToCompletions(step:ProductionStep, completionFactory:(text:String,type:String)->Completion):List<Completion>
    private fun generateCompletions(text: String, startOfCompletionCaretPosition: Int, currentCaretPosition: Int, steps: Set<ProductionStep>): List<Completion> {

        val result = mutableListOf<Completion>()
        val curry: (String, String) -> Completion = { completionText, completionType ->
            Completion(completionText, text, startOfCompletionCaretPosition, currentCaretPosition, completionType)
        }
        steps.forEach { step ->
            result.addAll(stepToCompletions(step, curry))
        }
        return result
    }
    override fun suggestCompletion(text: String,
                                   startOfCompletionCaretPosition: Int,
                                   currentCaretPosition: Int,
                                   astBasedAction: ((result: MutableSet<Completion>, ast: ASTNode) -> Unit)?): List<Completion> {
        val result = mutableSetOf<Completion>()
        try {
            val evalText = text.substring(0, startOfCompletionCaretPosition)
            val ast = parser.parse(evalText, true)
            if (astBasedAction != null) {
                ast.forEachRecursive({
                    astBasedAction(result, it)
                })
            }
        } catch (ex: ParserException) {
            //while writing we do not want to see lots of parse errors
        }
        if (result.isEmpty())
            result.addAll(generateCompletions(text, startOfCompletionCaretPosition, currentCaretPosition, parser.expectedTokenNotMatched))
        if (result.isEmpty())
            result.addAll(generateCompletions(text, startOfCompletionCaretPosition, currentCaretPosition, parser.possibleValidTokensIfContinuingParsing))
        return CompletionRanker().rank(result)
    }

}

class CompletionRanker {
    fun rank(completions: Set<Completion>): List<Completion> {
        return completions.sortedWith(compareBy {
            val substring = it.text.substring(it.startOfReplace, it.caretPosition)
            when {
                substring.isNotBlank() -> substring.similarity(it.completionText)
                else -> 1
            }
        }).reversed()
    }
}

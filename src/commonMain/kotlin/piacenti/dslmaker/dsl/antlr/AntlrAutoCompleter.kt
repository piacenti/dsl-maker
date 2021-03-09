package piacenti.dslmaker.dsl.antlr

import kotlin.math.min
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.tree.ParseTree
import piacenti.dslmaker.dsl.AutoCompleter
import piacenti.dslmaker.dsl.Completion
import piacenti.dslmaker.dsl.CompletionRanker
import piacenti.dslmaker.structures.ASTNode

abstract class AntlrAutoCompleter<T : Parser> : AutoCompleter {
    var parsedWithoutErrors: Boolean = true
    protected abstract fun createParser(tokenStream: CommonTokenStream): T
    protected abstract fun runStartRule(parser: T): ParseTree
    protected abstract fun createLexer(text: String): Lexer
    protected abstract fun processSuggestions(antlrCompletionData: AntlrCompletionData,
                                              completionFactory: (text: String, type: String) -> Completion): Set<Completion>

    override fun suggestCompletion(text: String, startOfCompletionCaretPosition: Int, currentCaretPosition: Int, astBasedAction: ((result: MutableSet<Completion>, ast: ASTNode) -> Unit)?): List<Completion> {
        val evalText = text.substring(0, currentCaretPosition)
        val lexer = createLexer(evalText)
        val tokenStream = CommonTokenStream(lexer)
        val parser = createParser(tokenStream)
        parsedWithoutErrors = true
        runStartRule(parser)
        if (parser.numberOfSyntaxErrors > 0) {
            parsedWithoutErrors = false
        }
        val suggestCompletions = AntlrCompletionSuggester().suggestCompletions(parser)
        //we add plus one here so that we don't replace the last character of the last match since it is inclusive
        val startOfReplace = min(suggestCompletions.highestTextIndexReached + 1, currentCaretPosition)
        val curry = { completionText: String, type: String -> Completion(completionText, text, startOfReplace, currentCaretPosition, type) }
        val result = processSuggestions(suggestCompletions, curry)
        return CompletionRanker().rank(result.toSet())
    }
}
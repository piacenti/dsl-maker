package piacenti.dslmaker.dsl.antlr

import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer

data class ErrorData(
    val message: String, val invocationStack: List<String>, val invocationStackIds: List<Int>,
    val expectedTokens: List<String>, val expectedTokenIds: List<Int>
)

class CaptureErrorListener : BaseErrorListener() {
    val errors = mutableListOf<ErrorData>()

    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?
    ) {
        super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e)
        val parser = recognizer as? Parser
        val ruleInvocationStack = parser?.ruleInvocationStack?.reversed() ?: emptyList()
        val ruleInvocationStackIds = ruleInvocationStack.mapNotNull { parser?.ruleIndexMap?.get(it) }
        val expectedTokenIds: List<Int> = parser?.expectedTokens?.toList() ?: emptyList()
        val expectedTokens =
            expectedTokenIds.mapNotNull { id -> parser?.tokenTypeMap?.entries?.find { it.value == id }?.key }
        errors.add(
            ErrorData(
                "line $line:$charPositionInLine $msg",
                ruleInvocationStack,
                ruleInvocationStackIds,
                expectedTokens,
                expectedTokenIds
            )
        )
    }
}

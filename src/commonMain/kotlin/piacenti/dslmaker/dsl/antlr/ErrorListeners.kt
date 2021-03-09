package piacenti.dslmaker.dsl.antlr

import org.antlr.v4.kotlinruntime.*

class CaptureErrorListener : BaseErrorListener() {
    val errors = mutableListOf<String>()

    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
        super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e)
        errors.add("line $line:$charPositionInLine $msg")
    }
}

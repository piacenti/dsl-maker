package piacenti.dslmaker.dsl.antlr

import org.antlr.v4.kotlinruntime.*

class BailoutErrorStrategy : DefaultErrorStrategy() {
    override fun recover(recognizer: Parser, e: RecognitionException) {
        throw e
    }

    override fun recoverInline(recognizer: Parser): Token {
        throw RuntimeException(InputMismatchException(recognizer))
    }

    override fun sync(recognizer: Parser) {
        //don't sync
    }
}
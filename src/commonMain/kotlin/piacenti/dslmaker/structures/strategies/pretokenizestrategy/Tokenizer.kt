package piacenti.dslmaker.structures.strategies.pretokenizestrategy

import piacenti.dslmaker.*
import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.errors.ParserException
import piacenti.dslmaker.getCurrentTimeInMilliSeconds
import piacenti.dslmaker.structures.Grammar

/**
 * @param <T>
 * @author Piacenti
</T> */
class Tokenizer(val grammar: Grammar<*>) {
    private val contextStack = mutableListOf<String>()
    private val tokenInfos = grammar.container.tokens
    companion object{
        fun matchTokenRegex(token: ProductionStep, text: String, indexOffset: Int): RegexMatch? {
            val result = regexMatchFromOffset(text, "(?s)^\\s*(?:" + token.regexDefinition + ")", indexOffset)
            return result
        }
    }
    fun tokenize(textInput: String): List<Token> {
        val startTime = getCurrentTimeInMilliSeconds()
        val tokens = mutableListOf<Token>()
        var text = textInput
        var currentStart = 0
        while (currentStart<textInput.length) {
            var match = false
            for (token in getContextTokens()) {
                if (token.regexDefinition != "") {
                    val m = matchTokenRegex(token, text,currentStart)
                    if (m != null) {
                        match = true
                        val end = m.groups.first().endIndex
                        val matchedText = m.groups.first().text
                        val value = matchedText!!.trim()
                        if (token.validateToken == null || token.validateToken.invoke(value)) {
                            tokens.add(Token(token,
                                    value, matchedText, currentStart,   end))
                            currentStart = end
                            if (token.removeContext != null)
                                contextStack.remove(token.removeContext)
                            if (token.addContext != null)
                                contextStack.add(token.addContext)
                            break
                        }
                    }
                }
            }
            if (!match) {
                if (text.trim() == "") {
                    break
                }
                throw ParserException(
                        "Unexpected character in input at index $currentStart, remaining textInput: $text")
            }
        }
        LOG.debug("tokenization done in " + (getCurrentTimeInMilliSeconds() - startTime).toString()+"ms")
        return tokens
    }

    private fun getContextTokens(): List<ProductionStep> {
        return tokenInfos.filter { it.context == null || it.context in contextStack }
    }
}
package piacenti.dslmaker

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.errors.ParserException
import piacenti.dslmaker.interfaces.GrammarInventory
import piacenti.dslmaker.interfaces.Parser
import piacenti.dslmaker.structures.ASTNode
import piacenti.dslmaker.structures.Grammar
import piacenti.dslmaker.structures.derivationgraph.LeftRecursionRemover

class GenericParser2<T : GrammarInventory>(grammar: Grammar<T>): Parser {
    private val leftRecursionRemover = LeftRecursionRemover()
    var grammar: Grammar<T> = leftRecursionRemover.removeLeftRecursion(grammar)
        set(grammar) {
            field = grammar
            leftRecursionRemover.removeLeftRecursion(field)
        }

    private var matcher: ExpressionMatcher2? = null

    override val highestIndexParsed: Int
        get() = matcher!!.trackData.highestSuccessfulIndex

    override val possibleValidTokensIfContinuingParsing: Set<ProductionStep>
        get() = matcher!!.trackData.possibleValidTokensIfContinuingParsing.toSet()

    override val expectedTokenNotMatched: Set<ProductionStep>
        get() = matcher!!.trackData.expectedTokenNotMatched.toSet()


    private fun getCustomEnumInstance(): List<ProductionStep> {
        return grammar.container.tokens + grammar.container.productions

    }

    /**
     * @param text
     * @param matchEntireText
     * @return
     * @throws ParserException
     */
    override fun parse(text: String, matchEntireText: Boolean , tokenizeFirst: Boolean): ASTNode {
        matcher = ExpressionMatcher2(tokenizeFirst)
        return matcher!!.match(grammar, text, matchEntireText)
    }


    @Suppress("MemberVisibilityCanBePrivate")
    fun parseFind(text: String, regexDelimiter: String?): List<ASTNode> {
        matcher = ExpressionMatcher2()
        return matcher!!.find(grammar, text, regexDelimiter)

    }

    fun parseFind(text: String): List<ASTNode> {
        return parseFind(text, null)
    }
}
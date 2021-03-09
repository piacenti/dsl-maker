package piacenti.dslmaker

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.errors.ParserException
import piacenti.dslmaker.interfaces.GrammarInventory
import piacenti.dslmaker.interfaces.Parser
import piacenti.dslmaker.structures.Grammar
import piacenti.dslmaker.structures.derivationgraph.DerivationGraph
import piacenti.dslmaker.structures.strategies.pretokenizestrategy.PreTokenizeExpressionMatcher
import piacenti.dslmaker.structures.strategies.pretokenizestrategy.Token
import piacenti.dslmaker.structures.strategies.pretokenizestrategy.Tokenizer

class GenericParser<T : GrammarInventory>(grammar: Grammar<T>):Parser {

    var grammar: Grammar<T> = grammar
        set(grammar) {
            field = grammar
            derivationTree = DerivationGraph(field)
        }
    @Suppress("MemberVisibilityCanBePrivate")
    var derivationTree: DerivationGraph = DerivationGraph(grammar)
        private set



    private var matcher: ExpressionMatcher? = null

    override val highestIndexParsed: Int
        get() = matcher!!.highestSuccessfulIndex

    override val possibleValidTokensIfContinuingParsing: Set<ProductionStep>
        get() = matcher!!.possibleValidTokensIfContinuingParsing

    override val expectedTokenNotMatched: Set<ProductionStep>
        get() = matcher!!.expectedTokenNotMatched


    private fun getCustomEnumInstance(): List<ProductionStep> {
        return grammar.container.tokens + grammar.container.productions

    }

    /**
     * @param text
     * @param matchEntireText
     * @return
     * @throws ParserException
     */
    override fun parse(text: String, matchEntireText: Boolean , tokenizeFirst: Boolean): GenericParser<T> {
        if (tokenizeFirst) {
            preTokenizeParse(derivationTree, grammar, text, matchEntireText)
        } else {
            matcher = ExpressionMatcher()
            matcher!!.match(derivationTree, grammar, text, matchEntireText)
        }
        return this
    }

    private fun preTokenizeParse(derivationTree: DerivationGraph?, grammar: Grammar<T>?, text: String,
                                 matchEntireText: Boolean): GenericParser<T> {
        val tokens = getTokens(text)
        val expressionMatcher = PreTokenizeExpressionMatcher()
        expressionMatcher.match(derivationTree!!, grammar!!, text, tokens, matchEntireText)
        return this
    }

    private fun getTokens(text: String): List<Token> {
        val tokenDefinitions = getCustomEnumInstance()
        val tokenizer = Tokenizer(grammar)
        return tokenizer.tokenize(text)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun parseFind(text: String, regexDelimiter: String? = null): GenericParser<T> {
        matcher = ExpressionMatcher()
        matcher!!.find(derivationTree, grammar, text, regexDelimiter)
        return this
    }

    fun parseFind(text: String): GenericParser<T> {
        parseFind(text, null)
        return this
    }
}
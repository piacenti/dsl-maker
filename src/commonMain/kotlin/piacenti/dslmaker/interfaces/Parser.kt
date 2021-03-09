package piacenti.dslmaker.interfaces

import piacenti.dslmaker.abstraction.ProductionStep

interface Parser {
    //TODO bring both version 1 and 2 of parsers under a common interface
    fun parse(text: String, matchEntireText: Boolean = false, tokenizeFirst: Boolean = false):Any?
    val highestIndexParsed: Int
    val possibleValidTokensIfContinuingParsing: Set<ProductionStep>
    val expectedTokenNotMatched: Set<ProductionStep>
}
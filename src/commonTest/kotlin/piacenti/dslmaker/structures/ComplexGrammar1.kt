package piacenti.dslmaker.structures

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.interfaces.GrammarInventory


object ComplexGrammarInventory1 : GrammarInventory {
    override val tokens = mutableListOf<ProductionStep>()
    override val productions = mutableListOf<ProductionStep>()

    val AND_LEAF = createToken("AND_LEAF", "&&")
    val OR_LEAF = createToken("OR_LEAF", "\\|\\|")
    val COMMA_LEAF = createToken("COMMA_LEAF", ",")
    val OPEN_PARENTHESIS_LEAF = createToken("OPEN_PARENTHESIS_LEAF", "\\(")
    val CLOSE_PARENTHESIS_LEAF = createToken("CLOSE_PARENTHESIS_LEAF", "\\)")

    val IDENTIFIER_LEAF = createToken("IDENTIFIER_LEAF", "(\\w|\\.|_)+")
    val STRING_LEAF = createToken("STRING_LEAF", "\"([^\"\\\\]|\\\\.)*?\"")
    val EMPTY_LEAF = createToken("EMPTY_LEAF", "")


    val START = createProduction("START")
    val OPEN_PARENTHESIS = createProduction("OPEN_PARENTHESIS")
    val CLOSE_PARENTHESIS = createProduction("CLOSE_PARENTHESIS")
    val IDENTIFIER = createProduction("IDENTIFIER")

    val PARAMETERS = createProduction("PARAMETERS")
    val PARAMETER = createProduction("PARAMETER")
    val METHOD = createProduction("METHOD")
    val METHOD_NAME = createProduction("METHOD_NAME")
    val FIELD_NAME = createProduction("FIELD_NAME")
    val AND_GROUP = createProduction("AND_GROUP")
    val OR_GROUP = createProduction("OR_GROUP")

}

class ComplexGrammar1(root: ProductionStep, container: ComplexGrammarInventory1=ComplexGrammarInventory1) : Grammar<ComplexGrammarInventory1>(root, container) {

    init {
        container.apply {
            p(START, e(AND_GROUP), e(OR_GROUP))
            p(METHOD, e(METHOD_NAME, OPEN_PARENTHESIS, PARAMETERS, CLOSE_PARENTHESIS))
            p(PARAMETERS, e(PARAMETER, COMMA_LEAF, PARAMETERS), e(PARAMETER), e(EMPTY_LEAF))
            p(PARAMETER, e(METHOD), e(FIELD_NAME))
            p(IDENTIFIER, e(IDENTIFIER_LEAF), e(STRING_LEAF))
            p(AND_GROUP, e(OPEN_PARENTHESIS, PARAMETER, CLOSE_PARENTHESIS, AND_LEAF, AND_GROUP),
                    e(OPEN_PARENTHESIS, AND_GROUP, CLOSE_PARENTHESIS), e(OPEN_PARENTHESIS, OR_GROUP,
                    CLOSE_PARENTHESIS), e(OPEN_PARENTHESIS, PARAMETER, CLOSE_PARENTHESIS))
            p(OR_GROUP, e(OPEN_PARENTHESIS, PARAMETER, CLOSE_PARENTHESIS, OR_LEAF, OR_GROUP),
                    e(OPEN_PARENTHESIS, OR_GROUP, CLOSE_PARENTHESIS), e(OPEN_PARENTHESIS, AND_GROUP,
                    CLOSE_PARENTHESIS), e(OPEN_PARENTHESIS, PARAMETER, CLOSE_PARENTHESIS))
            p(OPEN_PARENTHESIS, e(OPEN_PARENTHESIS_LEAF))
            p(CLOSE_PARENTHESIS, e(CLOSE_PARENTHESIS_LEAF))
            p(METHOD_NAME, e(IDENTIFIER))
            p(FIELD_NAME, e(IDENTIFIER))
        }
    }
}
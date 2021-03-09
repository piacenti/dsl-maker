package piacenti.dslmaker.structures

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.interfaces.GrammarInventory

object ComplexGrammarInventory2 : GrammarInventory {
    override val tokens = mutableListOf<ProductionStep>()
    override val productions = mutableListOf<ProductionStep>()

    val EVALUATE_LEAF = createToken("EVALUATE_LEAF", "evaluate")
    val COMMA_LEAF = createToken("COMMA_LEAF", ",")
    val OPEN_PARENTHESIS_LEAF = createToken("OPEN_PARENTHESIS_LEAF", "\\(")
    val CLOSE_PARENTHESIS_LEAF = createToken("CLOSE_PARENTHESIS_LEAF", "\\)")
    val IDENTIFIER_LEAF = createToken("IDENTIFIER_LEAF", "(\\w|\\.|_|\\$|\\?)+")
    val STRING_LEAF = createToken("STRING_LEAF", "\"([^\"\\\\]|\\\\.)*?\"")
    val OPERATOR_LEAF = createToken("OPERATOR_LEAF", ">=|<=|==|>|<|!=|in|from|\\+|-|&&|\\|\\|")
    val NEGATION_OPERATOR_LEAF = createToken("NEGATION_OPERATOR_LEAF", "!")
    val BRACKET_LEAF = createToken("BRACKET_LEAF", "\\[|\\]")
    val PERIOD_LEAF = createToken("PERIOD_LEAF", "\\.")
    val COLON_LEAF = createToken("COLON_LEAF", ":")
    val SEMI_COLON_LEAF = createToken("SEMI_COLON_LEAF", ";")
    val EMPTY_LEAF = createToken("EMPTY_LEAF", "")


    val START = createProduction("START")
    val OPEN_PARENTHESIS = createProduction("OPEN_PARENTHESIS")
    val CLOSE_PARENTHESIS = createProduction("CLOSE_PARENTHESIS")
    val OPERATOR = createProduction("OPERATOR")
    val NEGATION_OPERATOR_OPTIONAL = createProduction("NEGATION_OPERATOR_OPTIONAL")
    val IDENTIFIER = createProduction("IDENTIFIER")

    val METHOD = createProduction("METHOD")
    val METHOD_NAME = createProduction("METHOD_NAME")
    val FIELD_NAME = createProduction("FIELD_NAME")

    val EXPRESSION = createProduction("EXPRESSION")
    val ARGUMENT_LIST = createProduction("ARGUMENT_LIST")
    val RANGE = createProduction("RANGE")
    val BRACKET = createProduction("BRACKET")
    val METHOD_CHAIN = createProduction("METHOD_CHAIN")
    val CAST = createProduction("CAST")
    val PROPERTY = createProduction("PROPERTY")
    val STATEMENT = createProduction("STATEMENT")
    val STATEMENTS = createProduction("STATEMENTS")
    val ASSIGNMENT = createProduction("ASSIGNMENT")
    val EVALUATE = createProduction("EVALUATE")

}

class ComplexGrammar2(root: ProductionStep, container: ComplexGrammarInventory2=ComplexGrammarInventory2) : Grammar<ComplexGrammarInventory2>(root, container) {

    init {
        container.apply {
            p(START, e(STATEMENTS))
            p(STATEMENTS, e(STATEMENT, STATEMENTS), e(STATEMENT))
            p(STATEMENT, e(ASSIGNMENT, SEMI_COLON_LEAF), e(EVALUATE, SEMI_COLON_LEAF), e(EXPRESSION, SEMI_COLON_LEAF))
            p(ASSIGNMENT, e(IDENTIFIER, COLON_LEAF, EXPRESSION))
            p(EXPRESSION, e(CAST, EXPRESSION),
                    e(NEGATION_OPERATOR_OPTIONAL, OPEN_PARENTHESIS, EXPRESSION, OPERATOR, EXPRESSION,
                            CLOSE_PARENTHESIS),
                    e(OPEN_PARENTHESIS, EXPRESSION,
                            OPERATOR, EXPRESSION, CLOSE_PARENTHESIS), e(EXPRESSION, OPERATOR, EXPRESSION),
                    e(METHOD_CHAIN),
                    e(NEGATION_OPERATOR_OPTIONAL, OPEN_PARENTHESIS, EXPRESSION,
                            CLOSE_PARENTHESIS), e(OPEN_PARENTHESIS, EXPRESSION, CLOSE_PARENTHESIS),
                    e(NEGATION_OPERATOR_OPTIONAL, FIELD_NAME), e(FIELD_NAME), e(RANGE))
            p(METHOD, e(METHOD_NAME, OPEN_PARENTHESIS, ARGUMENT_LIST, CLOSE_PARENTHESIS), e(METHOD_NAME, EXPRESSION))
            p(METHOD_CHAIN, e(METHOD, PERIOD_LEAF, METHOD_CHAIN), e(METHOD))
            p(ARGUMENT_LIST, e(EXPRESSION, COMMA_LEAF, ARGUMENT_LIST), e(EXPRESSION), e(EMPTY_LEAF))
            p(IDENTIFIER, e(IDENTIFIER_LEAF), e(STRING_LEAF))
            p(OPEN_PARENTHESIS, e(OPEN_PARENTHESIS_LEAF))
            p(CLOSE_PARENTHESIS, e(CLOSE_PARENTHESIS_LEAF))
            p(METHOD_NAME, e(IDENTIFIER))
            p(FIELD_NAME, e(IDENTIFIER))
            p(OPERATOR, e(OPERATOR_LEAF))
            p(NEGATION_OPERATOR_OPTIONAL, e(NEGATION_OPERATOR_LEAF))
            p(RANGE, e(BRACKET, EXPRESSION, COMMA_LEAF, EXPRESSION, BRACKET))
            p(BRACKET, e(BRACKET_LEAF))
            p(CAST, e(OPEN_PARENTHESIS, IDENTIFIER, CLOSE_PARENTHESIS))
            p(EVALUATE, e(EVALUATE_LEAF, OPEN_PARENTHESIS, ASSIGNMENT, CLOSE_PARENTHESIS),
                    e(EVALUATE_LEAF, OPEN_PARENTHESIS, EXPRESSION, CLOSE_PARENTHESIS))
        }
    }
}
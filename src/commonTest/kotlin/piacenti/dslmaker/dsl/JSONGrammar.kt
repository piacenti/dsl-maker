package piacenti.dslmaker.dsl

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.interfaces.GrammarInventory
import piacenti.dslmaker.structures.Grammar

object JSONToken : GrammarInventory {
    override val productions: MutableList<ProductionStep> = mutableListOf()
    override val tokens: MutableList<ProductionStep> = mutableListOf()

    val openBrace = createToken("openBrace", "\\{")
    val closeBrace = createToken("closeBrace", "\\}")
    val openBracket = createToken("openBracket", "\\[")
    val closeBracked = createToken("closeBracked", "\\]")
    val quotedSequence = createToken("quotedSequence", "\"(?:.*?(?<!\\\\))\"")
    val number = createToken("number", "-?\\d+(?:\\.\\d+)*")
    val boolean = createToken("boolean", "true|false")
    val nullValue = createToken("nullValue", "null")
    val colon = createToken("colon", ":")
    val comma = createToken("comma", ",")
    val empty = createToken("empty", "")

    val OBJECT = createProduction("OBJECT")
    val ARRAY = createProduction("ARRAY")
    val KEY_VALUE_PAIR = createProduction("KEY_VALUE_PAIR")
    val KEY_VALUE_PAIRS = createProduction("KEY_VALUE_PAIRS")
    val START = createProduction("START")
    val VALUE = createProduction("VALUE")
    val VALUES = createProduction("VALUES")
}

class JSONGrammar(root: ProductionStep = JSONToken.START) : Grammar<JSONToken>(root, JSONToken) {
    init {
        JSONToken.apply {
            p(START, e(OBJECT), e(ARRAY))
            p(KEY_VALUE_PAIR, e(quotedSequence, colon, VALUE))
            p(KEY_VALUE_PAIRS, e(KEY_VALUE_PAIR, comma, KEY_VALUE_PAIRS), e(KEY_VALUE_PAIR))
            p(ARRAY, e(openBracket, VALUES, closeBracked))
            p(VALUES, e(VALUE, comma, VALUES), e(VALUE))
            p(VALUE, e(ARRAY), e(OBJECT), e(quotedSequence), e(number), e(boolean), e(nullValue))
            p(OBJECT, e(openBrace, KEY_VALUE_PAIRS, closeBrace))
        }
    }
}
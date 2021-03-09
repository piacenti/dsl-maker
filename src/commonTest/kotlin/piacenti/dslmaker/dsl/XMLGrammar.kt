package piacenti.dslmaker.dsl

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.interfaces.GrammarInventory
import piacenti.dslmaker.structures.Grammar

object XMLToken : GrammarInventory {
    override val productions: MutableList<ProductionStep> = mutableListOf()
    override val tokens: MutableList<ProductionStep> = mutableListOf()

    val value = createToken("value", "<!\\[CDATA\\[.+?\\]\\]>|[^<]+", context = "value", validateToken = {it.isNotBlank()})
    val valueEncoded = createToken("valueEncoded", "&lt;!\\[CDATA\\[.+?\\]\\]&gt;|(?:.(?!lt;))+", context = "valueEncoded")
    val lessThan = createToken("lessThan", "<", removeContext = "value", addContext = "element")
    val lessThanThanEncoded = createToken("lessThanThanEncoded", "&lt;", removeContext = "valueEncoded", addContext = "element")
    val greaterThan = createToken("greaterThan", ">", addContext = "value", removeContext = "element")
    val greaterThanEncoded = createToken("greaterThanEncoded", "&gt;", addContext = "valueEncoded", removeContext = "element")

    val slash = createToken("slash", "/", context = "element")
    val colon = createToken("colon", ":", context = "element")
    val questionMark = createToken("questionMark", "\\?", context = "element")
    val equalsMark = createToken("equalsMark", "=", context = "element")
    val alphanumericSequence = createToken("alphanumericSequence", "\\w+", context = "element")
    val quotedSequence = createToken("quotedSequence", "\"(?:.*?(?<!\\\\))\"", context = "element")
    val empty = createToken("empty", "")

    val ELEMENT = createProduction("ELEMENT")
    val ELEMENTS = createProduction("ELEMENTS")
    val HEADER_ELEMENT = createProduction("HEADER_ELEMENT")
    val HEADER_ELEMENT_ENCODED = createProduction("HEADER_ELEMENT_ENCODED")
    val ELEMENT_START = createProduction("ELEMENT_START")
    val ELEMENT_END = createProduction("ELEMENT_END")
    val ELEMENT_START_ENCODED = createProduction("ELEMENT_START_ENCODED")
    val ELEMENT_END_ENCODED = createProduction("ELEMENT_END_ENCODED")
    val ATTRIBUTE = createProduction("ATTRIBUTE")
    val ATTRIBUTES = createProduction("ATTRIBUTES")
    val START = createProduction("START")
    val NAME = createProduction("NAME")
    val SIBLING_ELEMENTS = createProduction("SIBLING_ELEMENTS")
    val OPTIONAL_VALUE = createProduction("OPTIONAL_VALUE")
    val OPTIONAL_ENCODED_VALUE = createProduction("OPTIONAL_ENCODED_VALUE")
}

class XMLGrammar(root: ProductionStep = XMLToken.START) : Grammar<XMLToken>(root, XMLToken) {
    init {
        XMLToken.apply {
            p(START, e(ELEMENTS))
            p(ELEMENTS, e(SIBLING_ELEMENTS))
            p(SIBLING_ELEMENTS, e(ELEMENT, SIBLING_ELEMENTS), e(ELEMENT))
            p(ELEMENT, e(HEADER_ELEMENT, ELEMENT),
                    e(ELEMENT_START, ELEMENTS, ELEMENT_END),
                    e(ELEMENT_START, OPTIONAL_VALUE, ELEMENT_END),
                    //parses html encoded xml
                    e(HEADER_ELEMENT_ENCODED, ELEMENT),
                    e(ELEMENT_START_ENCODED, ELEMENTS, ELEMENT_END_ENCODED),
                    e(ELEMENT_START_ENCODED, OPTIONAL_ENCODED_VALUE, ELEMENT_END_ENCODED))
            p(HEADER_ELEMENT, e(lessThan, questionMark, alphanumericSequence, ATTRIBUTES, questionMark, greaterThan))
            p(HEADER_ELEMENT_ENCODED, e(lessThanThanEncoded, questionMark, alphanumericSequence, ATTRIBUTES, questionMark, greaterThanEncoded))
            p(ELEMENT_START, e(lessThan, NAME, ATTRIBUTES, greaterThan))
            p(ELEMENT_END, e(lessThan, slash, NAME, greaterThan))
            p(ELEMENT_START_ENCODED, e(lessThanThanEncoded, NAME, ATTRIBUTES, greaterThanEncoded))
            p(ELEMENT_END_ENCODED, e(lessThanThanEncoded, slash, NAME, greaterThanEncoded))
            p(ATTRIBUTES, e(ATTRIBUTE, ATTRIBUTES), e(ATTRIBUTE), e(empty))
            p(ATTRIBUTE, e(NAME, equalsMark, quotedSequence))
            p(NAME, e(alphanumericSequence, colon, alphanumericSequence), e(alphanumericSequence))
            p(OPTIONAL_VALUE, e(value), e(empty))
            p(OPTIONAL_ENCODED_VALUE, e(valueEncoded), e(empty))
        }
    }
}
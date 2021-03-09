package piacenti.dslmaker

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.errors.ParserException
import piacenti.dslmaker.interfaces.GrammarInventory
import piacenti.dslmaker.structures.Expression
import piacenti.dslmaker.structures.Grammar
import piacenti.dslmaker.structures.Production
import kotlin.test.*

infix fun Any?.validateEquals(other: Any) {
    assertEquals(other, this)
}

object NumberExpressionTokens : GrammarInventory {
    override val tokens = mutableListOf<ProductionStep>()
    override val productions = mutableListOf<ProductionStep>()
    val number = createToken("number", "\\d+")
    val operation = createToken("operation", "\\+|-|\\*|/")
    val OpernPar = createToken("OpernPar", "\\(")
    val ClosePar = createToken("ClosePar", "\\)")
    val NUMBER = createProduction("NUMBER")
    val EXPRESSION = createProduction("EXPRESSION")
    val OPERATION = createProduction("OPERATION")
    val OP = createProduction("OP")
    val CP = createProduction("CP")

}

class NumberExpressionGrammar(root: ProductionStep,
                              numberExpressionsTokens: NumberExpressionTokens = NumberExpressionTokens) : Grammar<NumberExpressionTokens>(
        root, numberExpressionsTokens) {
    init {
        productions[NumberExpressionTokens.OP] = Production(null,
                Expression(NumberExpressionTokens.OpernPar))
        productions[NumberExpressionTokens.CP] = Production(null, Expression(NumberExpressionTokens.ClosePar))
        productions[NumberExpressionTokens.NUMBER] = Production(null,
                Expression(NumberExpressionTokens.number))
        productions[NumberExpressionTokens.OPERATION] = Production(null,
                Expression(NumberExpressionTokens.operation))
        productions[NumberExpressionTokens.EXPRESSION] = Production(null,
                Expression(NumberExpressionTokens.OpernPar,
                        NumberExpressionTokens.EXPRESSION, NumberExpressionTokens.ClosePar),
                Expression(NumberExpressionTokens.NUMBER,
                        NumberExpressionTokens.OPERATION, NumberExpressionTokens.EXPRESSION),
                Expression(NumberExpressionTokens.NUMBER))
    }
}

/**
 * Created by Piacenti on 4/12/2016.*/
class SimpleGenericParserTest {
    private var result = ""
    lateinit var grammar: NumberExpressionGrammar
    lateinit var parser: GenericParser<NumberExpressionTokens>
    private lateinit var parser2: GenericParser2<NumberExpressionTokens>

    @BeforeTest
    fun setup() {
        grammar = NumberExpressionGrammar(NumberExpressionTokens.EXPRESSION).apply {
            productions[NumberExpressionTokens.EXPRESSION]!!.action = { data ->
                result = data.match
            }
        }
        parser = GenericParser(grammar)
        parser2 = GenericParser2(grammar)
        result = ""
    }

    @Test
    fun should_parse_if_entire_text_match() {
        parser.parse("5+7-2")
        result validateEquals "5+7-2"
        parser.parse("7-2*5")
        result validateEquals "7-2*5"
    }
    @Test
    fun should_parse_full_usage(){
        val result=parser2.parse("(4+2)", true)
        result.toDOTGraph()
        result.treeToString() validateEquals """
            EXPRESSION: (4+2)
            ├── OpernPar: (
            ├── EXPRESSION: 4+2
            │   ├── NUMBER: 4
            │   │   └── number: 4
            │   ├── OPERATION: +
            │   │   └── operation: +
            │   └── EXPRESSION: 2
            │       └── NUMBER: 2
            │           └── number: 2
            └── ClosePar: )
        """.trimIndent().trim()
    }
    @Test
    fun should_parse_if_entire_text_match2() {
        parser2.parse("7-2*5")
        val result = parser2.parse("5+7-2")
        result.treeToString() validateEquals """
        EXPRESSION: 5+7-2
        ├── NUMBER: 5
        │   └── number: 5
        ├── OPERATION: +
        │   └── operation: +
        └── EXPRESSION: 7-2
            ├── NUMBER: 7
            │   └── number: 7
            ├── OPERATION: -
            │   └── operation: -
            └── EXPRESSION: 2
                └── NUMBER: 2
                    └── number: 2
        """.trimIndent().trim()
    }
    @Ignore
    @Test
            /*TODO need to address this problem. Might be able to modify the expression algorithm to instead of passing data
             to each method call, to hold a separate stack variables that will contain the arguments potentially increasing dramatically
             how deep you can go with the algorithm may also need to externalize any variable defined outside the loop closure
             since they are captured by it. This means no variables are defined outside of those methods and no parameters are passed
             and instead we manage everything with global class variables
             Other options may also be needed since the tests bellow prove that you can only
             go as deep as about 100000. Default java stack is of 400k. Using while loops to simulate
             the recursion may be an option although a difficult one

             -Xss6128 increases the thread stack size so that this test passes
             but not optimal since it changes default stack size for every new thread
             which increases memory usage overall. It also doesn't solve the problem for javascript*/
    fun should_not_cause_stack_overflow() {
        val text = "5+7" + ("-2+7" * 1000)
        parser.parse(text, true)
    }

    @Test
    fun should_not_cause_stack_overflow2() {
        val text = "5+7" + ("-2+7" * 1000)
        parser2.parse(text, true)
    }


    @Test
    fun should_fail_if_not_all_match_and_flag_says_it_should() {
        parser.parse("5+7-2!@#$%")
        result validateEquals "5+7-2"
        assertFailsWith<ParserException> { parser.parse("5+7-2!@#$%", true) }
    }

    @Test
    fun should_fail_if_not_all_match_and_flag_says_it_should2() {
        val result=parser2.parse("5+7-2!@#$%")
        result.text validateEquals "5+7-2"
        assertFailsWith<ParserException> { parser.parse("5+7-2!@#$%", true) }
    }

    @Test
    fun should_be_able_to_find_in_text() {
        parser.parseFind("!@#$%5+7-2")
        result validateEquals "5+7-2"
    }
    @Test
    fun should_be_able_to_find_in_text2() {
        val result=parser2.parseFind("!@#$%5+7-2")
        result.first().text validateEquals "5+7-2"
    }

    @Test
    fun parseFind_should_be_performant() {
        val start = getCurrentTimeInMilliSeconds()
        for (i in 0..500) {
            parser.parseFind("!@#$%5+7-2")
        }
        val end = getCurrentTimeInMilliSeconds()
        val diff = end - start
        assertTrue(diff / 1000 < 2.0)
        println(diff)
    }
    @Test
    fun parseFind_should_be_performant2() {
        val start = getCurrentTimeInMilliSeconds()
        for (i in 0..500) {
            parser2.parseFind("!@#$%5+7-2")
        }
        val end = getCurrentTimeInMilliSeconds()
        val diff = end - start
        assertTrue(diff / 1000 < 2.0)
        println(diff)
    }
    @Test
    fun should_be_performant() {
        val start = getCurrentTimeInMilliSeconds()
        for (i in 0..300) {
            parser.parse("7-2*5", true)
        }
        val end = getCurrentTimeInMilliSeconds()
        val diff = end - start
        assertTrue(diff / 1000 < 2.0)
        println(diff)
    }
    @Test
    fun should_be_performant_2() {
        val start = getCurrentTimeInMilliSeconds()
        for (i in 0..300) {
            parser2.parse("7-2*5", true)
        }
        val end = getCurrentTimeInMilliSeconds()
        val diff = end - start
        assertTrue(diff / 1000 < 2.0)
        println(diff)
    }

    @Test
    fun global_regex_should_work() {
        val globalDotRegexString = "(?s)\"(.+?)\"".genericRegex()
        val result = globalDotRegexString.find(""" stuff "
             
             more"
        """)
        assertNotEquals(null, result)
    }
}


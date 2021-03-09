package piacenti.dslmaker

import piacenti.dslmaker.ExpressionInventory.EXPRESSION
import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.interfaces.GrammarInventory
import piacenti.dslmaker.structures.Grammar
import piacenti.dslmaker.structures.Production
import kotlin.test.Test
import kotlin.test.assertEquals


/**
 * @author Piacenti
 */
object ExpressionInventory : GrammarInventory {
    override val tokens = mutableListOf<ProductionStep>()
    override val productions = mutableListOf<ProductionStep>()

    internal val NUMBER = createToken("NUMBER", "\\d+")
    internal val ADDITION = createToken("ADDITION", "\\+")
    internal val SUBTRACTION = createToken("SUBTRACTION", "-")
    internal val DIVISION = createToken("DIVISION", "/")
    internal val MULTIPLICATION = createToken("MULTIPLICATION", "\\*")
    internal val OPEN_PARENTHESIS = createToken("OPEN_PARENTHESIS", "\\(")
    internal val CLOSE_PARENTHESIS = createToken("CLOSE_PARENTHESIS", "\\)")
    internal val EXPRESSION = createProduction("EXPRESSION")
    internal val PRECEDENCE = createProduction("PRECEDENCE")
    internal val OPERATOR = createProduction("OPERATOR")
}

class ObviousLeftRecursiveExpressionGrammar internal constructor(root: ProductionStep,
                                                                 simpleToken: ExpressionInventory = ExpressionInventory) : Grammar<ExpressionInventory>(
        root, simpleToken) {
    init {
        ExpressionInventory.apply {
            p(EXPRESSION, e(EXPRESSION, OPERATOR, EXPRESSION),
                    e(OPEN_PARENTHESIS, EXPRESSION, CLOSE_PARENTHESIS),
                    e(NUMBER))
            p(OPERATOR, e(ADDITION), e(SUBTRACTION), e(DIVISION),
                    e(MULTIPLICATION))
        }
    }
}

class IndirectLeftRecursiveExpressionGrammar internal constructor(root: ProductionStep,
                                                                  simpleToken: ExpressionInventory = ExpressionInventory) : Grammar<ExpressionInventory>(
        root, simpleToken) {
    init {
        ExpressionInventory.apply {
            p(EXPRESSION, e(PRECEDENCE, EXPRESSION),
                    e(OPEN_PARENTHESIS, EXPRESSION, CLOSE_PARENTHESIS),
                    e(NUMBER))
            p(PRECEDENCE, e(EXPRESSION, OPERATOR))
            p(OPERATOR, e(ADDITION), e(SUBTRACTION), e(DIVISION),
                    e(MULTIPLICATION))
        }
    }
}

class DirectAndIndirectLeftRecursiveExpressionGrammar internal constructor(root: ProductionStep,
                                                                           simpleToken: ExpressionInventory = ExpressionInventory) : Grammar<ExpressionInventory>(
        root, simpleToken) {
    init {
        ExpressionInventory.apply {
            p(EXPRESSION, e(PRECEDENCE, EXPRESSION),
                    e(OPEN_PARENTHESIS, EXPRESSION, CLOSE_PARENTHESIS),
                    e(EXPRESSION, OPERATOR),
                    e(NUMBER))
            p(PRECEDENCE, e(EXPRESSION, OPERATOR))
            p(OPERATOR, e(ADDITION), e(SUBTRACTION), e(DIVISION),
                    e(MULTIPLICATION))
        }
    }
}

class RecursiveGrammarGenericParserTest {


    /**
     *
     */
    @Test
    fun handleObviousLeftRecursionTest() {
        val grammar = ObviousLeftRecursiveExpressionGrammar(EXPRESSION)
        val test = GenericParser(grammar)
        val result = StringBuilder()
        captureAst(result, grammar.productions)
        test.parse("2+3*(4+2)", true)
        assertEquals("""EXPRESSION -> (\() EXPRESSION (\)) EXPRESSION' | (\d+) EXPRESSION'
EXPRESSION' -> OPERATOR EXPRESSION EXPRESSION' | ()
OPERATOR -> (\+) | (-) | (/) | (\*)
""", grammar.toString())
        assertEquals(
                "ASTNode(type=EXPRESSION, text='2+3*(4+2)')ASTNode(type=(\\d+), text='2')ASTNode(type=EXPRESSION', text='+3*(4+2)')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=EXPRESSION, text='3*(4+2)')ASTNode(type=(\\d+), text='3')ASTNode(type=EXPRESSION', text='*(4+2)')ASTNode(type=OPERATOR, text='*')ASTNode(type=(\\*), text='*')ASTNode(type=EXPRESSION, text='(4+2)')ASTNode(type=(\\(), text='(')ASTNode(type=EXPRESSION, text='4+2')ASTNode(type=(\\d+), text='4')ASTNode(type=EXPRESSION', text='+2')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=EXPRESSION, text='2')ASTNode(type=(\\d+), text='2')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')ASTNode(type=(\\)), text=')')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')",
                result.toString())


    }

    @Test
    fun handleObviousLeftRecursionTest2() {
        val grammar = ObviousLeftRecursiveExpressionGrammar(EXPRESSION)
        val test = GenericParser2(grammar)
        val result = StringBuilder()
        val ast = test.parse("2+3*(4+2)", true)
        ast.forEachRecursive({
            result.append(it)
        })
        assertEquals("""EXPRESSION -> (\() EXPRESSION (\)) EXPRESSION' | (\d+) EXPRESSION'
EXPRESSION' -> OPERATOR EXPRESSION EXPRESSION' | ()
OPERATOR -> (\+) | (-) | (/) | (\*)
""", grammar.toString())
        assertEquals(
                "ASTNode(type=EXPRESSION, text='2+3*(4+2)')ASTNode(type=(\\d+), text='2')ASTNode(type=EXPRESSION', text='+3*(4+2)')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=EXPRESSION, text='3*(4+2)')ASTNode(type=(\\d+), text='3')ASTNode(type=EXPRESSION', text='*(4+2)')ASTNode(type=OPERATOR, text='*')ASTNode(type=(\\*), text='*')ASTNode(type=EXPRESSION, text='(4+2)')ASTNode(type=(\\(), text='(')ASTNode(type=EXPRESSION, text='4+2')ASTNode(type=(\\d+), text='4')ASTNode(type=EXPRESSION', text='+2')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=EXPRESSION, text='2')ASTNode(type=(\\d+), text='2')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')ASTNode(type=(\\)), text=')')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')",
                result.toString())


    }

    private fun captureAst(result: StringBuilder, productions: Map<ProductionStep, Production>) {
        productions.getValue(EXPRESSION).action = { data ->
            if (data.astNode.parent == null) {
                data.astNode.forEachRecursive({ node ->
                    result.append(node)
                })
            }
        }
    }


    @Test
    fun handleIndirectLeftRecursionTest() {
        val grammar = IndirectLeftRecursiveExpressionGrammar(EXPRESSION)
        val test = GenericParser(grammar)
        val result = StringBuilder()
        captureAst(result, grammar.productions)
        test.parse("2+3*(4+2)", true)
        assertEquals(
                "ASTNode(type=EXPRESSION, text='2+3*(4+2)')ASTNode(type=PRECEDENCE, text='2+')ASTNode(type=(\\d+), text='2')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=PRECEDENCE', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION, text='3*(4+2)')ASTNode(type=PRECEDENCE, text='3*')ASTNode(type=(\\d+), text='3')ASTNode(type=OPERATOR, text='*')ASTNode(type=(\\*), text='*')ASTNode(type=PRECEDENCE', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION, text='(4+2)')ASTNode(type=(\\(), text='(')ASTNode(type=EXPRESSION, text='4+2')ASTNode(type=PRECEDENCE, text='4+')ASTNode(type=(\\d+), text='4')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=PRECEDENCE', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION, text='2')ASTNode(type=(\\d+), text='2')ASTNode(type=(\\)), text=')')",
                result.toString())
    }

    @Test
    fun handleIndirectLeftRecursionTest2() {
        val grammar = IndirectLeftRecursiveExpressionGrammar(EXPRESSION)
        val test = GenericParser2(grammar)
        val result = StringBuilder()
        val ast = test.parse("2+3*(4+2)", true)
        ast.forEachRecursive({
            result.append(it)
        })
        assertEquals(
                "ASTNode(type=EXPRESSION, text='2+3*(4+2)')ASTNode(type=PRECEDENCE, text='2+')ASTNode(type=(\\d+), text='2')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=PRECEDENCE', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION, text='3*(4+2)')ASTNode(type=PRECEDENCE, text='3*')ASTNode(type=(\\d+), text='3')ASTNode(type=OPERATOR, text='*')ASTNode(type=(\\*), text='*')ASTNode(type=PRECEDENCE', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION, text='(4+2)')ASTNode(type=(\\(), text='(')ASTNode(type=EXPRESSION, text='4+2')ASTNode(type=PRECEDENCE, text='4+')ASTNode(type=(\\d+), text='4')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=PRECEDENCE', text='')ASTNode(type=(), text='')ASTNode(type=EXPRESSION, text='2')ASTNode(type=(\\d+), text='2')ASTNode(type=(\\)), text=')')",
                result.toString())
        ast.treeToString() validateEquals """
        EXPRESSION: 2+3*(4+2)
        ├── PRECEDENCE: 2+
        │   ├── NUMBER: 2
        │   ├── OPERATOR: +
        │   │   └── ADDITION: +
        │   └── PRECEDENCE': 
        │       └── 
        └── EXPRESSION: 3*(4+2)
            ├── PRECEDENCE: 3*
            │   ├── NUMBER: 3
            │   ├── OPERATOR: *
            │   │   └── MULTIPLICATION: *
            │   └── PRECEDENCE': 
            │       └── 
            └── EXPRESSION: (4+2)
                ├── OPEN_PARENTHESIS: (
                ├── EXPRESSION: 4+2
                │   ├── PRECEDENCE: 4+
                │   │   ├── NUMBER: 4
                │   │   ├── OPERATOR: +
                │   │   │   └── ADDITION: +
                │   │   └── PRECEDENCE': 
                │   │       └── 
                │   └── EXPRESSION: 2
                │       └── NUMBER: 2
                └── CLOSE_PARENTHESIS: )
        """.trimIndent().trim()
    }

    @Test
    fun handleMixedLeftRecursionTest() {
        val grammar = DirectAndIndirectLeftRecursiveExpressionGrammar(EXPRESSION)
        //validate grammar is the expected at this point
        assertEquals("""EXPRESSION -> PRECEDENCE EXPRESSION | (\() EXPRESSION (\)) | EXPRESSION OPERATOR | (\d+)
OPERATOR -> (\+) | (-) | (/) | (\*)
PRECEDENCE -> EXPRESSION OPERATOR
""", grammar.toString())
        val test = GenericParser(grammar)
        val result = StringBuilder()
        captureAst(result, grammar.productions)
        test.parse("2+", true)
        assertEquals(
                """EXPRESSION -> PRECEDENCE EXPRESSION EXPRESSION' | (\() EXPRESSION (\)) EXPRESSION' | (\d+) EXPRESSION'
EXPRESSION' -> OPERATOR EXPRESSION' | ()
OPERATOR -> (\+) | (-) | (/) | (\*)
PRECEDENCE' -> EXPRESSION EXPRESSION' OPERATOR PRECEDENCE' | ()
PRECEDENCE -> (\() EXPRESSION (\)) EXPRESSION' OPERATOR PRECEDENCE' | (\d+) EXPRESSION' OPERATOR PRECEDENCE'
""",
                grammar.toString())
        assertEquals(
                "ASTNode(type=EXPRESSION, text='2+')ASTNode(type=(\\d+), text='2')ASTNode(type=EXPRESSION', text='+')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')",
                result.toString())

    }

    @Test
    fun handleMixedLeftRecursionTest2() {
        val grammar = DirectAndIndirectLeftRecursiveExpressionGrammar(EXPRESSION)
        //validate grammar is the expected at this point
        assertEquals("""EXPRESSION -> PRECEDENCE EXPRESSION | (\() EXPRESSION (\)) | EXPRESSION OPERATOR | (\d+)
OPERATOR -> (\+) | (-) | (/) | (\*)
PRECEDENCE -> EXPRESSION OPERATOR
""", grammar.toString())
        val test = GenericParser2(grammar)
        val result = StringBuilder()
        val ast = test.parse("2+", true)
        ast.forEachRecursive({
            result.append(it)
        })
        assertEquals(
                """EXPRESSION -> PRECEDENCE EXPRESSION EXPRESSION' | (\() EXPRESSION (\)) EXPRESSION' | (\d+) EXPRESSION'
EXPRESSION' -> OPERATOR EXPRESSION' | ()
OPERATOR -> (\+) | (-) | (/) | (\*)
PRECEDENCE' -> EXPRESSION EXPRESSION' OPERATOR PRECEDENCE' | ()
PRECEDENCE -> (\() EXPRESSION (\)) EXPRESSION' OPERATOR PRECEDENCE' | (\d+) EXPRESSION' OPERATOR PRECEDENCE'
""",
                grammar.toString())
        assertEquals(
                "ASTNode(type=EXPRESSION, text='2+')ASTNode(type=(\\d+), text='2')ASTNode(type=EXPRESSION', text='+')ASTNode(type=OPERATOR, text='+')ASTNode(type=(\\+), text='+')ASTNode(type=EXPRESSION', text='')ASTNode(type=(), text='')",
                result.toString())

    }

}
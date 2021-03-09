package piacenti.dslmaker

import piacenti.dslmaker.errors.ParserException
import piacenti.dslmaker.structures.ComplexGrammar2
import piacenti.dslmaker.structures.ComplexGrammarInventory2
import piacenti.dslmaker.structures.ComplexGrammarInventory2.START
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


/**
 * Created on 9/25/2018.*/
class ComplexGrammarTest {
    private lateinit var grammar: ComplexGrammar2
    private lateinit var parser: GenericParser<ComplexGrammarInventory2>
    private lateinit var parser2: GenericParser2<ComplexGrammarInventory2>

    @BeforeTest
    fun setup() {
        grammar = ComplexGrammar2(START)
        parser = GenericParser(grammar)
        parser2 = GenericParser2(grammar)
    }

    
    @Test
    fun should_fail_if_low_priority_expression_matches_what_should_be_matched_by_a_higher_priority_expression() {
        val expression = "evaluate(((com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.answer, com.piacenti.test.enums.SrfSOdfpDy.NO)) && (((com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.explanation, \"N/A\")) || (com.piacenti.test.EvuErzkZjDwfcRbo.IsAtLeast(java.lang.Integer.valueOf((int) 5 ), java.lang.Integer.valueOf((int) 6 )))))));"
        grammar.container.apply {
            grammar.p(EXPRESSION,
                    //grammar.e(CAST, EXPRESSION), cast should be happening here for it to succeed
                    grammar.e(OPEN_PARENTHESIS, EXPRESSION,
                    OPERATOR, EXPRESSION, CLOSE_PARENTHESIS), grammar.e(EXPRESSION, OPERATOR, EXPRESSION),
                    grammar.e(METHOD_CHAIN), grammar.e(OPEN_PARENTHESIS, EXPRESSION, CLOSE_PARENTHESIS),
                    grammar.e(CAST, EXPRESSION),
                    grammar.e(FIELD_NAME), grammar.e(RANGE))
        }
        parser.grammar=grammar
        parser2.grammar=grammar
        assertFailsWith<ParserException>("failed index 319") { parser.parse(expression) }
        assertFailsWith<ParserException>("failed index 319") { parser2.parse(expression) }
        //could not find a simpler expression to recreate the cycle problem
        // that may cause out of memory exception as seen in expression above
        assertFailsWith<ParserException>("failed index 15") { parser2.parse("evaluate((int) 5)") }
    }


    @Test
    fun should_give_proper_error_for_tokens_without_wrapping_productions() {
        val expression = "the_LimitType : (the_Matching_Row_1.getString(\"LimitType\"))"

        assertFailsWith<ParserException>("(;)") { parser.parse(expression) }
        assertFailsWith<ParserException>("(;)") { parser2.parse(expression) }

    }

    @Test
    fun should_give_proper_error_for_tokens_with_wrapping_productions_if_the_grammar_method_is_called() {
        val expression = "the_LimitType : (the_Matching_Row_1.getString(\"LimitType\"))"
        grammar.wrapTokensWithProductions()
        parser.grammar = grammar
        parser2.grammar = grammar
        assertFailsWith<ParserException>("SEMI_COLON") { parser.parse(expression) }
        assertFailsWith<ParserException>("SEMI_COLON") { parser2.parse(expression) }

    }

    @Test
    fun should_properly_work_with_optional_productions() {
        val expression = """
the_wrDrF: com.piacenti.test.wrDrF((?this.lHbvUHCrXtNBErzwjRg(currentModel)
&& !(?this.umbolaOutcome.getDetailByName(com.piacenti.test.StringConstants.MAP_METHOD).value.equals("N"))));
pwX_YpVULbf: com.piacenti.test.XrcPXeY();
evaluate (the_Cabula_Jpzjpdk : (pwX_YpVULbf.cabulaJpzjpdkName));
the_Prior_XrcPXeY: com.piacenti.test.XrcPXeY() from com.piacenti.test.utilities.XrcPXeYUtils.priorXrcPXeYWithUmbolaFactors(pwX_YpVULbf, com.piacenti.test.enums.LineOfBusinessEnum.BOP);
evaluate (the_Prior_Cabula_Jpzjpdk : (the_Prior_XrcPXeY.cabulaJpzjpdkName));
the_Mapped_iiEln_yrTUa: com.piacenti.test.Detail() from the_wrDrF.umbolaOutcome.getDetailByName(com.piacenti.test.StringConstants.MAP_PP);
the_Uncapped_iiEln_yrTUa: com.piacenti.test.Detail() from the_wrDrF.umbolaOutcome.getDetailByName(com.piacenti.test.StringConstants.MafqfQ_oZQiP_POINT);
the_wrDrF_Approved_Cap: com.piacenti.test.Detail() from the_wrDrF.umbolaOutcome.getDetailByName(com.piacenti.test.StringConstants.CAP);
the_Capping_Applied_ArPU: com.piacenti.test.Detail() from the_wrDrF.umbolaOutcome.getDetailByName(com.piacenti.test.StringConstants.CAPPING_APPLIED);
evaluate ((((the_Prior_Cabula_Jpzjpdk.equals("Ohio tzJBWwHQ"))
|| ((!(the_Prior_Cabula_Jpzjpdk.equals("Ohio tzJBWwHQ"))
&& !(the_Cabula_Jpzjpdk.equals("Ohio tzJBWwHQ")))))
&& ((the_Mapped_iiEln_yrTUa.doubleValue.doubleValue() + the_wrDrF_Approved_Cap.doubleValue.doubleValue()) < the_Uncapped_iiEln_yrTUa.doubleValue.doubleValue())));
         """
        val result = StringBuilder()
        val result2 = StringBuilder()
        grammar.container.apply {
            grammar.p(EXPRESSION, grammar.e(CAST, EXPRESSION), grammar.e(METHOD_CHAIN),
                    grammar.e(NEGATION_OPERATOR_OPTIONAL, OPEN_PARENTHESIS, EXPRESSION, OPERATOR, EXPRESSION,
                            CLOSE_PARENTHESIS), grammar.e(EXPRESSION, OPERATOR,
                    EXPRESSION),
                    grammar.e(NEGATION_OPERATOR_OPTIONAL, OPEN_PARENTHESIS, EXPRESSION, CLOSE_PARENTHESIS),
                    grammar.e(NEGATION_OPERATOR_OPTIONAL, FIELD_NAME), grammar.e(RANGE))
            grammar.p(NEGATION_OPERATOR_OPTIONAL, grammar.e(NEGATION_OPERATOR_LEAF), grammar.e(EMPTY_LEAF))
            parser.grammar = grammar
            parser2.grammar=grammar
        }


            grammar.a(START) { data ->
                if (data.astNode.parent == null) {
                    data.astNode.forEachRecursive({ node ->
                        result.append(node)
                    })
                }
            }

        parser.parse(expression)
        val ast=parser2.parse(expression)
        ast.forEachRecursive({node->
            result2.append(node)
        })
        assertEquals(complexAstResult,result.toString())
        assertEquals(complexAstResult,result2.toString())
    }
}

package piacenti.dslmaker.structures

import piacenti.dslmaker.GenericParser
import piacenti.dslmaker.structures.ComplexGrammarInventory1.AND_GROUP
import piacenti.dslmaker.structures.ComplexGrammarInventory1.OR_GROUP
import piacenti.dslmaker.structures.ComplexGrammarInventory1.PARAMETER
import piacenti.dslmaker.structures.ComplexGrammarInventory1.START
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ASTTest {
    lateinit var grammar: ComplexGrammar1
    lateinit var parser: GenericParser<ComplexGrammarInventory1>
    private val input = "((com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.questionCode, \"LMBOP59\")) && (com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.answer, com.piacenti.test.enums.SrfSOdfpDy.NO)) && (((com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.explanation, \"N/A\")) || (BankruptcyLimit.booleanValue()))))"

    @BeforeTest
    fun setup() {
        grammar = ComplexGrammar1(START)
        parser = GenericParser(grammar)
    }

    @Test
    fun ast_should_not_be_null_in_action() {
        lateinit var result: List<ASTNode>

        grammar.a(START) { data ->
            result = data.astNode.findAllDescendantChildrenOfTypeUnderParent(AND_GROUP, PARAMETER)
        }

        parser.parse(input, true)

        assertEquals(2, result.size)
        assertEquals(
                "com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.questionCode, \"LMBOP59\")",
                result[0].text)
        assertEquals(
                "com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.answer, com.piacenti.test.enums.SrfSOdfpDy.NO)",
                result[1].text)

    }

    @Test
    fun ast_should_not_contain_information_from_failed_branches() {
        lateinit var result: MutableList<ASTNode>
        grammar.a(AND_GROUP) { data ->
            result = data.astNode.findAllDescendantChildrenOfTypeUnderParent(AND_GROUP, PARAMETER)
        }

        parser.parse(input, true)

        assertEquals(2, result.size)
        assertEquals(
                "com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.questionCode, \"LMBOP59\")",
                result[0].text)
        assertEquals(
                "com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.answer, com.piacenti.test.enums.SrfSOdfpDy.NO)",
                result[1].text)


        result.clear()
        grammar.a(AND_GROUP) {
            //TODO why does this test break if this is removed?
        }
        grammar.a(OR_GROUP) { data ->
            result = data.astNode.findAllDescendantChildrenOfTypeUnderParent(OR_GROUP, PARAMETER)
        }
        parser.parse(input, true)
        assertEquals(2, result.size)
        assertEquals(
                "com.piacenti.test.EvuErzkZjDwfcRbo.Is(GwY_gYRHdlIpL_Wa_usTK_UXFAfiGY_Regnyi.explanation, \"N/A\")",
                result[0].text)
        assertEquals(
                "BankruptcyLimit.booleanValue()",
                result[1].text)

    }
}
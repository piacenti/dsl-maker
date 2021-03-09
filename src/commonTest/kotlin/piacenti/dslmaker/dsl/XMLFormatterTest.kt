package piacenti.dslmaker.dsl

import piacenti.dslmaker.dsl.XMLToken.ELEMENTS
import piacenti.dslmaker.dsl.XMLToken.ELEMENT_END
import piacenti.dslmaker.dsl.XMLToken.ELEMENT_END_ENCODED
import piacenti.dslmaker.dsl.XMLToken.ELEMENT_START
import piacenti.dslmaker.dsl.XMLToken.ELEMENT_START_ENCODED
import piacenti.dslmaker.dsl.XMLToken.OPTIONAL_VALUE
import piacenti.dslmaker.structures.ASTNode
import piacenti.dslmaker.validateEquals
import kotlin.test.Test

class XMLFormatter : GrammarBasedFormatter(XMLGrammar()) {

    override fun format(text: String, providedAST: ASTNode?): String {
        return super.format(text.replace(">\\s+<".toRegex(), "><"), providedAST)
    }

    override fun tabValueSupplier(ast: ASTNode): Pair<Int, Order> {
        return if ((ast.type == ELEMENT_START || (ast.type == ELEMENT_END && !ast.getSiblings().map { sibling -> sibling.type }.contains(OPTIONAL_VALUE))
                        || (ast.type == ELEMENT_START_ENCODED || (ast.type == ELEMENT_END_ENCODED && !ast.getSiblings().map { sibling -> sibling.type }.contains(XMLToken.OPTIONAL_ENCODED_VALUE)))))
            ast.findAllAncestorsWithTypeInList(listOf(ELEMENTS)).size - 1 to Order.BEFORE
        else
            0 to Order.BEFORE
    }

    override fun isLineBreakable(astNode: ASTNode): Pair<Boolean, Order> {
        return if (astNode.type == ELEMENT_END || astNode.type == ELEMENT_END_ENCODED)
            true to Order.AFTER
        else if (astNode.type == ELEMENT_START && !astNode.getSiblings().map { sibling -> sibling.type }.contains(OPTIONAL_VALUE)
                ||
                astNode.type == ELEMENT_START_ENCODED && !astNode.getSiblings().map { sibling -> sibling.type }.contains(XMLToken.OPTIONAL_ENCODED_VALUE))
            true to Order.AFTER
        else false to Order.AFTER
    }

}

class XMLFormatterTest {
    @Test
    fun formatterTest() {
        val formatted = XMLFormatter().format("""
<element>
<element>
<element><element>value</element>  <element>value value2</element>

</element>
</element>
</element>
                """.trimIndent().trim())
        formatted validateEquals """
<element>
    <element>
        <element>
            <element>value</element>
            <element>value value2</element>
        </element>
    </element>
</element>
                """.trimIndent().trim()
    }
}
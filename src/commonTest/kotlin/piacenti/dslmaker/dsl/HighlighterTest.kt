package piacenti.dslmaker.dsl

import piacenti.dslmaker.GenericParser2
import piacenti.dslmaker.decodeHtml
import piacenti.dslmaker.dsl.XMLToken.ELEMENT
import piacenti.dslmaker.dsl.XMLToken.START
import piacenti.dslmaker.encodeHtml
import piacenti.dslmaker.structures.ASTNode
import piacenti.dslmaker.validateEquals
import kotlin.test.Test

class SimpleXMLHighlighter : GrammarBasedHighlighter<XMLToken>(XMLGrammar(), START, START) {
    override fun setGrammarActions(styles: MutableList<StyleParse>) {
        grammar.a(START) {
            it.astNode.forEachRecursive({ node ->
                if (node.type == ELEMENT) {
                    styles.add(StyleParse("blue", node.trimmedStartIndex, node.endIndex, listOf("some" to "\"attribute\"")))
                }
            })
        }
    }

}

class SimpleXMLHighlighter2 : GrammarBasedHighlighter2<XMLToken>(XMLGrammar(), START, START) {
    override fun setGrammarActions(styles: MutableList<StyleParse>, ast: ASTNode) {
        ast.forEachRecursive({
            if (it.type == ELEMENT) {
                styles.add(StyleParse("blue", it.trimmedStartIndex, it.endIndex, listOf("some" to "\"attribute\"")))
            }
        })
    }

}

val xml = """
<element>
    <element>
        <element>
            <element>value</element>
        </element>
    </element>
</element>
""".trim()
val unformatted = """
<element><element>
        <element><element>value</element>
</element></element></element>
""".trim()

class HighlighterTest {

    @Test
    fun highlightTest() {
        SimpleXMLHighlighter().highlight(xml) validateEquals """
<span class='blue' some="attribute"><element>
    <span class='blue' some="attribute"><element>
        <span class='blue' some="attribute"><element>
            <span class='blue' some="attribute"><element>value</element></span>
        </element></span>
    </element></span>
</element></span>
        """.trimIndent().trim()
    }

    @Test
    fun highlightTest2() {
        SimpleXMLHighlighter2().highlight(xml) validateEquals """
<span class='blue' some="attribute"><element>
    <span class='blue' some="attribute"><element>
        <span class='blue' some="attribute"><element>
            <span class='blue' some="attribute"><element>value</element></span>
        </element></span>
    </element></span>
</element></span>
        """.trimIndent().trim()
    }

    @Test
    fun format_and_highlight() {
        val result = GrammarBasedFormatterAndHighlighter().applyAll(unformatted, XMLFormatter(), SimpleXMLHighlighter2())
        result.combined validateEquals """
<span class='blue' some="attribute"><element>
    <span class='blue' some="attribute"><element>
        <span class='blue' some="attribute"><element>
            <span class='blue' some="attribute"><element>value</element></span>
        </element></span>
    </element></span>
</element></span>
        """.trimIndent().trim()
        result.formatted validateEquals """
            <element>
                <element>
                    <element>
                        <element>value</element>
                    </element>
                </element>
            </element>
        """.trimIndent().trim()
    }


    @Test
    fun format_and_highlight_pre_parsed() {
        val result = GrammarBasedFormatterAndHighlighter().applyAll(unformatted, XMLFormatter(), SimpleXMLHighlighter2(), providedParser = GenericParser2(XMLGrammar()))
        result.combined validateEquals """
<span class='blue' some="attribute"><element>
    <span class='blue' some="attribute"><element>
        <span class='blue' some="attribute"><element>
            <span class='blue' some="attribute"><element>value</element></span>
        </element></span>
    </element></span>
</element></span>
        """.trimIndent().trim()
        result.formatted validateEquals """
            <element>
                <element>
                    <element>
                        <element>value</element>
                    </element>
                </element>
            </element>
        """.trimIndent().trim()
    }

    @Test
    fun format_and_highlight_with_customActionOnFormattedResultBeforeHighlighting() {
        val result = GrammarBasedFormatterAndHighlighter().applyAll(unformatted.encodeHtml(listOf("\"", "'")),
                XMLFormatter(),
                SimpleXMLHighlighter2(),
                providedParser = GenericParser2(XMLGrammar())
        )
        result.combined validateEquals """
<span class='blue' some="attribute">&lt;element&gt;
    <span class='blue' some="attribute">&lt;element&gt;
        <span class='blue' some="attribute">&lt;element&gt;
            <span class='blue' some="attribute">&lt;element&gt;value&lt;/element&gt;</span>
        &lt;/element&gt;</span>
    &lt;/element&gt;</span>
&lt;/element&gt;</span>
        """.trimIndent().trim()
        result.formatted.decodeHtml() validateEquals """
            <element>
                <element>
                    <element>
                        <element>value</element>
                    </element>
                </element>
            </element>
        """.trimIndent().trim()
    }

    @Test
    fun format_and_highlight_antlr() {
        val result = XMLFastHighlighterAndFormatterCommon()
        result.format(unformatted) validateEquals """
<element>
    <element>
        <element>
            <element>value</element>
        </element>
    </element>
</element>

        """.trimIndent().trimStart()
        result.highlightAndFormat(unformatted) validateEquals """
<span class='gold'><</span><span class='light-blue' data-toggle="tooltip" data-delay='{"show":1000,"hide":0}' data-placement="bottom" title="element">element</span><span class='gold'>></span>
    <span class='gold'><</span><span class='light-blue' data-toggle="tooltip" data-delay='{"show":1000,"hide":0}' data-placement="bottom" title="element/element">element</span><span class='gold'>></span>
        <span class='gold'><</span><span class='light-blue' data-toggle="tooltip" data-delay='{"show":1000,"hide":0}' data-placement="bottom" title="element/element/element">element</span><span class='gold'>></span>
            <span class='gold'><</span><span class='light-blue' data-toggle="tooltip" data-delay='{"show":1000,"hide":0}' data-placement="bottom" title="element/element/element/element">element</span><span class='gold'>></span>value<span class='gold'></</span><span class='light-blue'>element</span><span class='gold'>></span>
        <span class='gold'></</span><span class='light-blue'>element</span><span class='gold'>></span>
    <span class='gold'></</span><span class='light-blue'>element</span><span class='gold'>></span>
<span class='gold'></</span><span class='light-blue'>element</span><span class='gold'>></span>

        """.trimIndent().trimStart()

    }

}
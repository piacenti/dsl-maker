package piacenti.dslmaker.dsl.antlr

import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Token
import piacenti.dslmaker.dsl.InsertionType

interface AntlrFormatterAndHighlighter {
    var exceptionOnFailure: Boolean
    fun highlightAndFormat(text: String): String
    fun format(text: String): String
    fun highlight(text: String): String
    fun highlightAndFormatComputations(text: String): AggregateComputations
    fun formatComputations(text: String): AggregateComputations
    fun highlightComputations(text: String): AggregateComputations
}

abstract class AntlrFormatterAndHighlighterBase : AntlrFormatterAndHighlighter {
    override fun format(text: String): String {
        return highlightAndFormat(text, format = true, highlight = false).result
    }

    override fun highlight(text: String): String {
        return highlightAndFormat(text, format = false, highlight = true).result
    }

    override fun highlightAndFormat(text: String): String {
        return highlightAndFormat(text, format = true, highlight = true).result
    }

    override fun highlightAndFormatComputations(text: String): AggregateComputations {
        return highlightAndFormat(text, format = true, highlight = true).rewriter.allComputations
    }

    override fun formatComputations(text: String): AggregateComputations {
        return highlightAndFormat(text, format = true, highlight = false).rewriter.allComputations
    }

    override fun highlightComputations(text: String): AggregateComputations {
        return highlightAndFormat(text, format = false, highlight = true).rewriter.allComputations
    }

    protected abstract fun highlightAndFormat(
        text: String,
        format: Boolean,
        highlight: Boolean
    ): AntlrFormatterAndHighlighterListener
}

enum class StyleDefinitionType {
    CSS_CLASS, STYLE_ATTRIBUTE
}

interface AntlrHTMLHighlighterListener {
    val tokenStream: CommonTokenStream
    val rewriter: FastTokenStreamRewriter
    val defaultDefinitionType: StyleDefinitionType
        get() = StyleDefinitionType.CSS_CLASS
    fun highlightToken(
        token: Token?,
        definitions: String,
        attributes: List<Pair<String, String>> = emptyList(),
        definitionType: StyleDefinitionType = defaultDefinitionType
    ) {
        if (token == null)
            return
        var attributeText = attributes.joinToString(separator = " ") { "${it.first}=${it.second}" }
        if (attributes.isNotEmpty())
            attributeText = " $attributeText"
        val highlightAttributeText = getHighlightAttributeText(definitionType)
        rewriter.replace(token, "<span $highlightAttributeText='$definitions'$attributeText>${token.text}</span>")
    }

    fun highlightTokenRange(
        tokens: Pair<Token?, Token?>,
        definitions: String,
        attributes: List<Pair<String, String>> = emptyList(),
        definitionType: StyleDefinitionType = defaultDefinitionType
    ) {
        val first = tokens.first
        val second = tokens.second
        if (first == null || second == null)
            return
        val attributeText = attributes.joinToString(separator = " ") { "${it.first}=${it.second}" }
        val highlightAttributeText = getHighlightAttributeText(definitionType)
        rewriter.insertBefore(first, "<span $highlightAttributeText='$definitions'$attributeText>", InsertionType.STYLE)
        rewriter.insertAfter(second, "</span>", InsertionType.STYLE)
    }

    fun getHighlightAttributeText(definitionType: StyleDefinitionType) =
        if (definitionType == StyleDefinitionType.CSS_CLASS) "class" else "style"
}
interface AntlrHTMLFormatterListener{
    val rewriter: FastTokenStreamRewriter
    val tabType: String
}
interface AntlrFormatterAndHighlighterListener : AntlrHTMLHighlighterListener, AntlrHTMLFormatterListener {
    val highlight: Boolean
    val format: Boolean
    val result: String get() = rewriter.text
}
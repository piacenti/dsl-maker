package piacenti.dslmaker.dsl

import piacenti.dslmaker.GenericParser
import piacenti.dslmaker.GenericParser2
import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.combine
import piacenti.dslmaker.dsl.TextModifier.Companion.applyModificationsMap
import piacenti.dslmaker.interfaces.GrammarInventory
import piacenti.dslmaker.interfaces.Parser
import piacenti.dslmaker.structures.ASTNode
import piacenti.dslmaker.structures.Grammar

//"'&<>
private val htmlCharTempMap = mutableMapOf(
        "\"" to "\u00CB",
        "'" to "\u00CC",
        "&" to "\u00CD",
        "<" to "\u00CE",
        ">" to "\u00CF"
)
private val htmlTempToEncodeMap = mutableMapOf(
        "\u00CB" to "&quot;",
        "\u00CC" to "&#39;",
        "\u00CD" to "&amp;",
        "\u00CE" to "&lt;",
        "\u00CF" to "&gt;"
)


interface Highlighter : TextModifier {
    fun highlight(text: String, encodeExistingHtml: Boolean = false): String {
        val map = computeHighlighting(text)
        var text2 = text
        if (encodeExistingHtml) {
            //replace html characters with unique new mapped characters before adding styling xml so that they can be
            // replaced afterwards
            htmlCharTempMap.forEach {
                text2 = text2.replace(it.key, it.value)
            }
        }
        var result = applyModificationsMap(text2, map)
        if (encodeExistingHtml) {
            htmlTempToEncodeMap.forEach {
                result = result.replace(it.key, it.value)
            }
        }
        return result
    }

    fun computeHighlighting(text: String, providedAST: ASTNode? = null, providedParser: GenericParser2<*>? = null): MutableMap<Int, List<TextModification>>
}

data class StyleParse(val style: String, val start: Int, val end: Int, val customAttributes: List<Pair<String, String>> = emptyList())
abstract class HTMLHighlighterBase : Highlighter {
    companion object {
        fun processStyles(styles: MutableList<StyleParse>, text: String, errorClass: String): MutableMap<Int, MutableList<TextModification>> {
            val result: MutableMap<Int, MutableList<TextModification>> = mutableMapOf()
            var lastIndex = 0
            for (p in styles) {
                var attributesText = p.customAttributes.joinToString(separator = " ") { it.first + "=" + it.second }
                attributesText = if (attributesText.isNotBlank()) " $attributesText" else ""
                result.getOrPut(p.start) { mutableListOf() }.add(TextModification("<span class='${p.style}'$attributesText>", Order.BEFORE, InsertionType.STYLE))
                result.getOrPut(p.end) { mutableListOf() }.add(TextModification("</span>", Order.AFTER, InsertionType.STYLE))
                if (p.end > lastIndex)
                    lastIndex = p.end
            }
            if (lastIndex < text.length - 1) {
                result.getOrPut(lastIndex + 1) { mutableListOf() }.add(TextModification("<span class='$errorClass'>", Order.BEFORE, InsertionType.STYLE))
                result.getOrPut(text.length) { mutableListOf() }.add(TextModification("</span>", Order.AFTER, InsertionType.STYLE))
            }
            return result
        }
    }
}

abstract class GrammarBasedHighlighter<T : GrammarInventory>(val grammar: Grammar<T>,
                                                             val styleProduction: ProductionStep,
                                                             val fullParseProduction: ProductionStep,
                                                             val parser: Parser = GenericParser(grammar)
) : HTMLHighlighterBase() {
    protected abstract fun setGrammarActions(styles: MutableList<StyleParse>)

    override fun computeHighlighting(text: String, providedAST: ASTNode?, providedParser: GenericParser2<*>?): MutableMap<Int, List<TextModification>> {
        val properStyles = mutableListOf<StyleParse>()
        val partialStyles = mutableListOf<StyleParse>()
        //ignore errors while typing
        kotlin.runCatching {
            handleStyleProduction(partialStyles, text, providedAST)
            handleStartProduction(properStyles, text, providedAST)
        }
        if ((providedParser?.highestIndexParsed ?: parser.highestIndexParsed) < text.length) {
            return (processStyles(partialStyles, text, "error").combine(processStyles(properStyles, text, "error-line"))).toMutableMap()
        }
        return (processStyles(properStyles, text, "error-line")).toMutableMap()
    }

    protected open fun handleStartProduction(properStyles: MutableList<StyleParse>, text: String, providedAST: ASTNode? = null) {
        setGrammarActions(properStyles)
        grammar.startProduction = fullParseProduction
        parser.parse(text, false)
    }

    protected open fun handleStyleProduction(partialStyles: MutableList<StyleParse>, text: String, providedAST: ASTNode? = null) {
        setGrammarActions(partialStyles)
        grammar.startProduction = styleProduction
        parser.parse(text, false)
    }

}

//uses version 2 of the parser
abstract class GrammarBasedHighlighter2<T : GrammarInventory>(grammar: Grammar<T>,
                                                              styleProduction: ProductionStep,
                                                              fullParseProduction: ProductionStep,
                                                              parser: GenericParser2<T> = GenericParser2(grammar)
) : GrammarBasedHighlighter<T>(grammar, styleProduction, fullParseProduction, parser) {
    private var currentASTNode: ASTNode? = null
    protected abstract fun setGrammarActions(styles: MutableList<StyleParse>, ast: ASTNode)
    override fun setGrammarActions(styles: MutableList<StyleParse>) {
        val astNode = currentASTNode
        if (astNode != null)
            setGrammarActions(styles, astNode)
    }

    override fun handleStartProduction(properStyles: MutableList<StyleParse>, text: String, providedAST: ASTNode?) {
        grammar.startProduction = fullParseProduction
        val ast = providedAST ?: parser.parse(text, false)
        if (ast is ASTNode)
            setGrammarActions(properStyles, ast)
    }

    override fun handleStyleProduction(partialStyles: MutableList<StyleParse>, text: String, providedAST: ASTNode?) {
        grammar.startProduction = styleProduction
        val ast = providedAST ?: parser.parse(text, false)
        if (ast is ASTNode)
            setGrammarActions(partialStyles, ast)
    }

}


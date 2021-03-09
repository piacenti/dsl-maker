package piacenti.dslmaker.dsl

import kotlinx.serialization.Serializable
import piacenti.dslmaker.GenericParser2
import piacenti.dslmaker.combine

@Serializable
data class FormatterAndHighlighterResult(val combined: String, val formatted: String)
class GrammarBasedFormatterAndHighlighter {
    fun applyAll(text: String, formatter: Formatter, highlighter: Highlighter,
                 providedParser: GenericParser2<*>? = null): FormatterAndHighlighterResult {
        val preClean = formatter.preClean(text)
        val astNode = providedParser?.parse(preClean, true)
        val tabText = formatter.getTabText()
        val formatIndexes = formatter.getFormatIndexes(astNode, preClean, tabText)
        val formatted = formatter.postClean(TextModifier.applyModificationsMap(preClean, formatIndexes))
        val computeHighlighting = highlighter.computeHighlighting(preClean, astNode, providedParser)
        val combined = formatIndexes.combine(computeHighlighting)
        val appliedChanges = TextModifier.applyModificationsMap(preClean, combined)
        val result = formatter.postClean(appliedChanges)
        return FormatterAndHighlighterResult(combined = result, formatted = formatted)
    }
}
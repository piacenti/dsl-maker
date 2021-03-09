package piacenti.dslmaker.dsl

import piacenti.dslmaker.GenericParser2
import piacenti.dslmaker.genericRegex
import piacenti.dslmaker.structures.ASTNode
import piacenti.dslmaker.structures.Grammar
import piacenti.dslmaker.times

interface Formatter {
    fun format(text: String, providedAST: ASTNode? = null): String
    fun preClean(text: String): String
    fun postClean(result: String): String
    fun getFormatIndexes(providedAST: ASTNode?, result: String, tabText: String): Map<Int, List<TextModification>>
    fun getTabText(): String
}

abstract class GrammarBasedFormatter(val grammar: Grammar<*>, var useSpaces: Boolean = true, var numTabs: Int = 4) : Formatter {
    private val parser = GenericParser2(grammar)

    /**
     * Supplies the indentation number to be used at ast element trimmedStartIndex. A value of 0 means no indentation
     */
    abstract fun tabValueSupplier(ast: ASTNode): Pair<Int, Order>

    /**
     * Flags if a line break should be added at ast element endIndex
     */
    abstract fun isLineBreakable(ast: ASTNode): Pair<Boolean, Order>
    override fun format(text: String, providedAST: ASTNode?): String {
        val tabText = getTabText()
        var result = preClean(text)
        val formatIndexesList = getFormatIndexes(providedAST, result, tabText)

        result = TextModifier.applyModificationsMap(result, formatIndexesList)
        return postClean(result)
    }

    override fun getTabText(): String {
        val tabText = (if (useSpaces) " " else "\t") * numTabs
        return tabText
    }

    override fun getFormatIndexes(providedAST: ASTNode?, result: String, tabText: String): Map<Int, List<TextModification>> {
        val formatIndexes = mutableMapOf<Int, MutableList<TextModification>>()
        var tabLevel: Pair<Int, Order>
        (providedAST ?: parser.parse(result)).forEachRecursive({ astNode ->
            tabLevel = tabValueSupplier(astNode)
            val lineBreakable = isLineBreakable(astNode)
            if (lineBreakable.first) {
                formatIndexes.getOrPut(if (lineBreakable.second == Order.BEFORE) astNode.trimmedStartIndex else astNode.endIndex) { mutableListOf() }.add(TextModification("\n", lineBreakable.second))
            }
            if (tabLevel.first > 0) {
                formatIndexes.getOrPut(if (tabLevel.second == Order.BEFORE) astNode.trimmedStartIndex else astNode.endIndex) { mutableListOf() }.add(TextModification(tabText * tabLevel.first, tabLevel.second))
            }
        })

        return formatIndexes
    }

    override fun postClean(result: String) = result.trimStart().replace("\\n\\s*?\\n".toRegex(), "\n").trim()

    override fun preClean(text: String) = text.replace("(?m)^\\s*".genericRegex(), "")

}
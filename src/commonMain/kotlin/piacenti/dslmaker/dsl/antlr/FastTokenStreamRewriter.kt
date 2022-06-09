package piacenti.dslmaker.dsl.antlr

import com.strumenta.kotlinmultiplatform.assert
import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.TokenStream
import org.antlr.v4.kotlinruntime.tree.ParseTree
import org.antlr.v4.kotlinruntime.tree.TerminalNode
import piacenti.dslmaker.dsl.InsertionType
import piacenti.dslmaker.dsl.Order
import piacenti.dslmaker.dsl.TextModification
import piacenti.dslmaker.end

data class AggregateComputations(
    val tokenComputations: MutableMap<Int, MutableList<TextModification>>,
    val textComputations: MutableMap<Int, MutableList<TextModification>>
)

class FastTokenStreamRewriter(val tokenStream: TokenStream) {
    val allComputations: AggregateComputations
        get() = AggregateComputations(tokenComputations, textComputations)
    val tokenComputations: MutableMap<Int, MutableList<TextModification>> = mutableMapOf()
    val textComputations: MutableMap<Int, MutableList<TextModification>> = mutableMapOf()

    fun insertAfter(p: ParseTree?, text: Any, insertionType: InsertionType) {
        val (_, token2) = selectTokens(null, p)
        insertAfter(token2, text, insertionType)
    }

    fun insertAfter(t: Token?, text: Any, insertionType: InsertionType) {
        if (t == null) return
        tokenComputations.getOrPut(t.tokenIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.AFTER, insertionType))
        textComputations.getOrPut(t.stopIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.AFTER, insertionType))
    }

    fun insertBefore(p: ParseTree?, text: Any, insertionType: InsertionType) {
        val (token1, _) = selectTokens(p, null)
        insertBefore(token1, text, insertionType)
    }

    fun insertBefore(t: Token?, text: Any, insertionType: InsertionType) {
        if (t == null) return
        tokenComputations.getOrPut(t.tokenIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.BEFORE, insertionType))
        textComputations.getOrPut(t.startIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.BEFORE, insertionType))

    }
    fun replace(p: ParseTree?, text: Any) {
        replace(p, null, text)
    }
    fun replace(p: ParseTree?, p2: ParseTree? = null, text: Any) {
        val (token1, token2) = selectTokens(p, p2)
        if (token1 != null && token2 != null && token1 != token2) {
            replace(token1, token2, text)
        } else {
            replace(token1, text)
        }
    }

    fun replace(t: Token?, text: Any) {
        if (t == null) return
        tokenComputations.getOrPut(t.tokenIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.REPLACE))
        textComputations.getOrPut(t.startIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.REPLACE, range = t.startIndex..t.stopIndex))
    }

    fun replace(from: Token?, to: Token?, text: Any) {
        if (from == null || to == null) return
        tokenComputations.getOrPut(from.tokenIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.REPLACE, range = from.tokenIndex..to.tokenIndex))
        textComputations.getOrPut(from.startIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.REPLACE, range = from.startIndex..to.stopIndex))
    }

    fun delete(p: ParseTree?, p2: ParseTree? = null) {
        val (token1, token2) = selectTokens(p, p2)
        if (token1 != null && token2 != null && token1 != token2) {
            delete(token1, token2)
        } else {
            delete(token1)
        }
    }

    private fun selectTokens(
        p: ParseTree?,
        p2: ParseTree?
    ): Pair<Token?, Token?> {
        val token1 = when (p) {
            is ParserRuleContext -> p.start
            is TerminalNode -> p.symbol
            else -> null
        }
        val token2 = when {
            p2 is ParserRuleContext -> p2.stop
            p2 is TerminalNode -> p2.symbol
            p2 == null && p is ParserRuleContext -> p.stop
            else -> null
        }
        return Pair(token1, token2)
    }

    fun delete(t: Token?) {
        if (t == null) return
        tokenComputations.getOrPut(t.tokenIndex) { mutableListOf() }.add(TextModification("", Order.DELETE))
        textComputations.getOrPut(t.startIndex) { mutableListOf() }
            .add(TextModification("", Order.DELETE, range = t.startIndex..t.stopIndex))
    }

    fun delete(from: Token?, to: Token?) {
        if (from == null || to == null) return
        tokenComputations.getOrPut(from.tokenIndex) { mutableListOf() }
            .add(TextModification("", Order.DELETE, range = from.tokenIndex..to.tokenIndex))
        textComputations.getOrPut(from.startIndex) { mutableListOf() }
            .add(TextModification("", Order.DELETE, range = from.startIndex..to.stopIndex))
    }

    fun removeEdit(token: Token?, predicate: (TextModification) -> Boolean) {
        tokenComputations[token?.tokenIndex ?: 0]?.removeAll(predicate)
        textComputations[token?.stopIndex ?: 0]?.removeAll(predicate)
    }

    val text: String
        get() {
            return generateText()
        }

    fun generateText(p: ParseTree?, p2: ParseTree?): String {
        val (token1, token2) = selectTokens(p, p2)
        return if (token1 != null && token2 != null) {
            assert(token1.startIndex <= token2.stopIndex)
            generateText(token1.tokenIndex, token2.tokenIndex)
        } else if (token1 != null) {
            generateText(token1.tokenIndex, token1.tokenIndex)
        } else throw IllegalArgumentException("Invalid tokens")
    }

    fun generateText(startIndex: Int = 0, lastIndex: Int = maxOf(tokenStream.size() - 1, 0)): String {
        return buildString {
            var i = startIndex
            assert(startIndex <= lastIndex)
            //if full text then last item is an EOF token so we don't want to process it.
            val lastIndexIsEOF = lastIndex == tokenStream.size() - 1
            while (withinBounds(lastIndexIsEOF, i, lastIndex)) {
                val t = tokenStream[i]
                val list = tokenComputations[i]
                if (list != null) {
                    val before = list.filter { it.order == Order.BEFORE }.sortedBy { it.insertionType }
                    before.forEach {
                        append(it.text)
                    }
                    val delete = list.filter { it.order == Order.DELETE }
                    val replace = list.filter { it.order == Order.REPLACE }
                    val deleteEmpty = delete.isEmpty()
                    val replaceEmpty = replace.isEmpty()
                    if (deleteEmpty && replaceEmpty) {
                        if (withinBounds(lastIndexIsEOF, i, lastIndex))
                            append(t.text)
                        i++
                    } else if (!deleteEmpty) {
                        val fullRange = delete.foldRight(i..i) { value, acc ->
                            val range = value.range
                            if (range != null && acc.last < range.last) {
                                range
                            } else
                                acc
                        }

                        insertAfterIfNotSame(fullRange, i)
                        i = fullRange.end
                    } else if (!replaceEmpty) {
                        val overArchingMod = replace.foldRight(replace.first()) { value, acc ->
                            val range = value.range
                            val accRange = acc.range
                            if ((accRange == null && range != null) || (accRange != null && range != null && accRange.last < range.last)) {
                                value
                            } else
                                acc
                        }
                        append(overArchingMod.text)
                        insertAfterIfNotSame(overArchingMod.range, i)
                        i = overArchingMod.range?.end ?: i + 1
                    }
                    insertAfter(list)
                } else {
                    if (withinBounds(lastIndexIsEOF, i, lastIndex))
                        append(t.text)
                    i++
                }
            }
        }
    }

    private fun withinBounds(lastIndexIsEOF: Boolean, i: Int, lastIndex: Int) =
        (lastIndexIsEOF && i != lastIndex) || (!lastIndexIsEOF && i <= lastIndex)

    private fun StringBuilder.insertAfterIfNotSame(fullRange: IntRange?, i: Int) {
        //insert after statements before skipping if last index is not current index
        if (fullRange != null && fullRange.last != i)
            tokenComputations[fullRange.last]?.let {
                insertAfter(it)
            }
    }

    private fun StringBuilder.insertAfter(list: MutableList<TextModification>) {
        val after = list.filter { it.order == Order.AFTER }.sortedByDescending { it.insertionType }
        after.forEach {
            append(it.text)
        }
    }
}
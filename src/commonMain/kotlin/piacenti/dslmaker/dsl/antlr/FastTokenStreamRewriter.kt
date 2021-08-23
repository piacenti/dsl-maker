package piacenti.dslmaker.dsl.antlr

import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.TokenStream
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
    fun insertAfter(t: Token?, text: Any, insertionType: InsertionType) {
        if (t == null) return
        tokenComputations.getOrPut(t.tokenIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.AFTER, insertionType))
        textComputations.getOrPut(t.stopIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.AFTER, insertionType))
    }

    fun insertBefore(t: Token?, text: Any, insertionType: InsertionType) {
        if (t == null) return
        tokenComputations.getOrPut(t.tokenIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.BEFORE, insertionType))
        textComputations.getOrPut(t.startIndex) { mutableListOf() }
            .add(TextModification(text.toString(), Order.BEFORE, insertionType))

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

    val text: String
        get() {
            return buildString {
                var i = 0
                //last item is an EOF token so we don't want to process it.
                val lastIndex = tokenStream.size() - 1
                while (i < lastIndex) {
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
                            if (i != lastIndex)
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
                                if ((accRange==null && range!=null) || (accRange != null && range != null && accRange.last < range.last)) {
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
                        if (i != lastIndex)
                            append(t.text)
                        i++
                    }
                }
            }
        }

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
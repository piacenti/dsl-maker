package piacenti.dslmaker

import org.antlr.v4.kotlinruntime.RuleContext
import org.antlr.v4.kotlinruntime.tree.ParseTree
import piacenti.dslmaker.interfaces.children

fun MutableList<*>.removeLast() {
    this.pop()
}

fun <T> MutableList<T>.pop(): T {
    return this.removeAt(this.size - 1)
}

val IntRange.end: Int
    get() {
        return endInclusive + 1
    }

operator fun String.times(number: Int): String {
    val builder = StringBuilder()
    for (x in 0 until number) {
        builder.append(this)
    }
    return builder.toString()
}

@Suppress("unused")
fun Collection<*>.completeOverlap(other: Collection<*>): Boolean {
    return this.containsAll(other) && other.containsAll(this)
}

fun <T, V> Map<T, List<V>>.combine(other: Map<T, List<V>>): Map<T, List<V>> {
    val result = mutableMapOf<T, MutableList<V>>()
    (this.entries + other.entries).forEach { entry ->
        val list = result.getOrPut(entry.key) { mutableListOf() }
        list.addAll(entry.value)
    }
    return result
}

private val htmlEncodingMap = mutableMapOf<String, String>(
        "\"" to "&quot;",
        "'" to "&#39;",
        "&" to "&amp;",
        "<" to "&lt;",
        ">" to "&gt;"
)
private val htmlDecodingMap = htmlEncodingMap.map { it.value to it.key }.toMap()
internal fun String.encodeHtml(ignore: List<String> = emptyList()): String {
    var result = this
    htmlEncodingMap.forEach {
        if (!ignore.contains(it.key))
            result = result.replace(it.key, it.value)
    }
    return result
}

internal fun String.decodeHtml(): String {
    var result = this
    htmlDecodingMap.forEach {
        result = result.replace(it.key, it.value)
    }
    return result
}

/**
 * Show total of [maxStartAndEndCharacters] lines from string. If there are more than [maxStartAndEndCharacters] then add ellipsis between [maxStartAndEndCharacters]/2 lines from
 * beginning and same from end. Odd numbers will be rounded down for the division
 *
 * @param maxStartAndEndCharacters
 */
fun String.previewLines(maxStartAndEndCharacters: Int = 5): String {
    val split = this.split("\n")
    return if (split.size > (maxStartAndEndCharacters * 2)) {
        val prefix = split.subList(0, maxStartAndEndCharacters).joinToString(separator = "\n")
        val suffix = split.subList(split.size - maxStartAndEndCharacters, split.size).joinToString(separator = "\n")
        "$prefix\n...\n$suffix"
    } else
        this
}

/**
 * Show total of [maxStartAndEndCharacters] characters from string. If there are more than [maxStartAndEndCharacters] then add ellipsis between [maxStartAndEndCharacters]/2 characters from
 * beginning and same from end. Odd numbers will be rounded down for the division
 *
 * @param maxStartAndEndCharacters
 */
fun String.previewCharacters(maxStartAndEndCharacters: Int = 5): String {
    return if (this.length > (maxStartAndEndCharacters * 2)) {
        val prefix = this.substring(0, maxStartAndEndCharacters)
        val suffix = this.substring(this.length - maxStartAndEndCharacters, this.length)
        "$prefix...$suffix"
    } else
        this
}

@Suppress("unused")
fun RuleContext.parentHierarchy(): List<RuleContext> {
    var current: RuleContext? = this.readParent()
    val result = mutableListOf<RuleContext>()
    while (current != null) {
        result.add(current)
        current = current.readParent()
    }
    return result
}

fun ParseTree.forEachRecursive(action: (ParseTree) -> Boolean) {
    if(action(this)) {
        this.children.forEach {
            it.forEachRecursive(action)
        }
    }
}
@Suppress("unused")
fun ParseTree.descendantsToList():List<ParseTree> {
    val result= mutableListOf<ParseTree>()
    this.forEachRecursive {
        result.add(it)
        true
    }
    return result
}
fun ParseTree.root():ParseTree {
    var root= this
    while(root.readParent()!=null){
        root.readParent()?.also {
            root=it
        }
    }
    return root
}
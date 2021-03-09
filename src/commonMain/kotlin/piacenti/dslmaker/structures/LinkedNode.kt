package piacenti.dslmaker.structures

interface LinkNode<T> {
    val value: T
    val next: LinkNode<T>?
    val previous: LinkNode<T>?
}

class LinkNodeMutable<T>(override var value: T, override var next: LinkNode<T>? = null, override var previous: LinkNode<T>? = null) : LinkNode<T>
class LinkedNodes<T>(list: List<T>) {
    var node: LinkNode<T>?

    init {
        if (list.isEmpty()) {
            node = null
        } else {
            node = LinkNodeMutable(list.first())
            var current = node as LinkNodeMutable
            list.forEachIndexed { index, item ->
                if (index > 0) {
                    val next = LinkNodeMutable(item)
                    next.previous = current
                    current.next = next
                    current = next
                }
            }
        }
    }
    fun next(): LinkNode<T>? {
        node = node?.next
        return node
    }
    @Suppress("unused")
    fun previous(): LinkNode<T>? {
        node = node?.previous
        return node
    }
}
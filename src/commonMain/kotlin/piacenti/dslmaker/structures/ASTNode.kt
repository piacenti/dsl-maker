package piacenti.dslmaker.structures

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.interfaces.GraphPrinter
import piacenti.dslmaker.pop
import piacenti.dslmaker.regexMatchFromOffset
import kotlin.math.min

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ASTNode(var type: ProductionStep? = null, var startIndex: Int = 0, var endIndex: Int = 0,
              var parent: ASTNode? = null, val originalText: String,
              var obj: Any? = null) : GraphPrinter {
    val text: String by lazy {
        if (endIndex < startIndex)
            ""
        else {
            val substring = originalText.substring(trimmedStartIndex, min(originalText.length, endIndex + 1))
            if (substring.isBlank())
                ""
            else
                substring
        }
    }
    override val parents: List<GraphPrinter>
        get() {
            val parentNode = parent
            return if (parentNode == null)
                emptyList()
            else
                listOf(parentNode)
        }
    override val children: MutableList<ASTNode> by lazy { mutableListOf<ASTNode>() }
    override fun graphNodeText(): String {
        return treeNodeIdentifierString()
    }

    val trimmedStartIndex: Int by lazy {
        val leadingSpaces = regexMatchFromOffset(originalText, "^\\s+", startIndex)
        val trimmedIndex = min(startIndex + (leadingSpaces?.groups?.first()?.text?.length ?: 0), endIndex)
        trimmedIndex
    }

    fun forEachRecursive(preAction: (ASTNode) -> Unit, postAction: ((ASTNode) -> Unit)? = null, astNode: ASTNode? = null) {
        var node = astNode
        if (node == null) {
            node = this
        }
        preAction(node)
        val iteratorList = mutableListOf<Pair<Iterator<ASTNode>, ASTNode>>()
        iteratorList.add(node.children.iterator() to node)
        while (iteratorList.isNotEmpty()) {
            val current = iteratorList.lastOrNull() ?: break
            if (current.first.hasNext()) {
                val next = current.first.next()
                preAction(next)
                iteratorList.add(next.children.iterator() to next)
            } else {
                val pop = iteratorList.pop()
                postAction?.invoke(pop.second)
            }
        }
    }

    fun getIndexOfThisNodeInParentsChildrenOfSameType(): Int? {
        val parent = parent
        parent?.children?.filter { it.type == type }?.forEachIndexed { index, astNode ->
            if (astNode === this) {
                return index
            }
        }
        return null
    }

    fun getSiblings(): List<ASTNode> {
        return this.parent?.children ?: emptyList()
    }

    fun findFirstAncestorWithNonNullObject(type: ProductionStep): ASTNode? {
        val parent1 = parent ?: return null

        var currentNode = parent1
        while (true) {
            if (currentNode.type == type && currentNode.obj != null) {
                return currentNode
            }
            val currentParent = currentNode.parent
            if (currentParent == null)
                return null
            else
                currentNode = currentParent
        }
    }

    fun findFirstAncestorWithTypeInList(types: List<ProductionStep>): ASTNode? {
        val parent1 = parent ?: return null
        var currentNode = parent1
        while (true) {
            if (types.contains(currentNode.type)) {
                return currentNode
            }
            val currentParent = currentNode.parent
            if (currentParent == null)
                return null
            else
                currentNode = currentParent
        }
    }

    fun findAllDescendantChildrenOfTypeUnderParent(parentType: ProductionStep, childType: ProductionStep): MutableList<ASTNode> {
        val result = mutableListOf<ASTNode>()
        val action: (ASTNode) -> Unit = {
            val nodeParent = it.parent
            if (it.type == childType && nodeParent != null && nodeParent.type == parentType) {
                result.add(it)
            }
        }
        forEachRecursive(action, null, this)
        return result
    }

    fun findAllNodesWithDescendantSequence(parent: ProductionStep,
                                           descendantsSequence: List<ProductionStep>): MutableList<ASTNode> {
        val result = mutableListOf<ASTNode>()
        val action: (ASTNode) -> Unit = {
            if (it.type == parent && matchesDescendantSequence(it.children, descendantsSequence)) {
                result.add(it)
            }
        }
        forEachRecursive(action, null, this)
        return result
    }

    private fun matchesDescendantSequence(
            children: MutableList<ASTNode>, descendantsSequence: List<ProductionStep>): Boolean {
        var currents = children
        descendantsSequence.forEach { descendant ->
            val temp = mutableListOf<ASTNode>()
            var found = false
            currents.forEach { current ->
                val elements = current.children.filter { it.type == descendant }
                temp.addAll(elements)
                if (elements.isNotEmpty())
                    found = true
            }
            if (!found)
                return false
            currents = temp
        }
        return true
    }

    fun findAllAncestorsWithTypeInList(types: List<ProductionStep>): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        var current = this.parent
        while (current?.parent != null) {
            if (current.type in types) {
                result.add(current)
            }
            current = current.parent
        }
        return result
    }

    fun copy(): ASTNode {
        val result = ASTNode(originalText = originalText)
        result.type = type
        result.endIndex = endIndex
        result.startIndex = startIndex
        result.parent = parent
        result.children.addAll(children.map { it.copy() })
        return result
    }

    override fun toString(): String {
        return "ASTNode(type=$type, text='$text')"
    }

    override fun treeNodeIdentifierString(): String {
        return type?.name.toString()
    }

    override fun treeNodeText(): String {
        return text
    }

}
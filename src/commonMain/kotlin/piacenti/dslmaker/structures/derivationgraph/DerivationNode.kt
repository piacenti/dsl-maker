package piacenti.dslmaker.structures.derivationgraph

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.interfaces.TreePrinter

/**
 * @param <T>
 * @author Piacenti
</T> */
@Suppress("MemberVisibilityCanBePrivate")
class DerivationNode(var step: ProductionStep, var isRootNode: Boolean = false) : TreePrinter {

    var parent: DerivationNode? = null
    override var children: MutableList<DerivationNode> = mutableListOf()

    var isLeaf = false

    override fun treeNodeIdentifierString(): String? {
        return null
    }

    override fun treeNodeText(): String {
        return toString()
    }
    @Suppress("MemberVisibilityCanBePrivate")
    fun fixTreeLinks() {
        traverseTree { parent, child ->
            child.parent = parent
            true
        }
    }
    @Suppress("MemberVisibilityCanBePrivate")
    fun traverseTree(action: (parent: DerivationNode, node: DerivationNode) -> Boolean) {
        var root = this
        while (root.parent != null) {
            root = root.parent!!
        }
        traverseDown(root, action)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun traverseDown(node: DerivationNode = this, action: (parent: DerivationNode, node: DerivationNode) -> Boolean) {
        node.children.forEach {
            if (action(node, it))
                traverseDown(it, action)
        }
    }
    override fun toString(): String {
        return "$step\nleaf:$isLeaf\n root:$isRootNode"
    }
}
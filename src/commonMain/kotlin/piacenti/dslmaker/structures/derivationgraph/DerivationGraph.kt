
package piacenti.dslmaker.structures.derivationgraph

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.pop
import piacenti.dslmaker.structures.Grammar
import piacenti.dslmaker.structures.Production

/**
 * @param <T>
 * @author Piacenti
</T> */
class DerivationGraph(private val grammar: Grammar<*>) {
    private var roots: MutableList<DerivationNode> = mutableListOf()
    private var subGraphs: MutableMap<ProductionStep, DerivationNode> = HashMap()
    private val leftRecursionRemover = LeftRecursionRemover()

    init {
        try {
            leftRecursionRemover.removeLeftRecursion(grammar)
        } catch (e: Exception) {
           println(e.message)
        }

        buildDerivationTree()
        roots.forEach { it.fixTreeLinks() }
    }


    /**
     * @return
     */
    fun getSubGraphs(): Map<ProductionStep, DerivationNode> {
        return subGraphs
    }

    private fun buildDerivationTree() {
        subGraphs = HashMap()
        roots = ArrayList()
        //Algorithm
        //simply build graphs for each production
        for ((key, value) in grammar.productions) {
            buildSubTree(key, value)
        }
        mergeTwins()
    }


    private fun buildSubTree(key: ProductionStep, production: Production) {
        val localRoot = DerivationNode(key, true)
        roots.add(localRoot)
        //go over productions and append each subgroups to root
        val expressions = production.expressions
        for (expression in expressions) {
            var parent = localRoot
            val steps = expression.steps
            for (step in steps) {
                val derivationNode = DerivationNode(step)
                derivationNode.parent=parent
                parent.children.add(derivationNode)
                parent = parent.children.last()
            }
        }
        subGraphs[localRoot.step] = localRoot
    }

    private fun mergeTwins() {

        for ((_, value) in subGraphs) {
//put all nodes into a list after traversal
            val traversalNodes = getTraveraslNodes(value)
            //remove root since it doesn't have twins
            traversalNodes.removeAt(0)
            while (traversalNodes.isNotEmpty()) {
                val current = traversalNodes[0]
                val twinList = ArrayList<DerivationNode>()
                twinList.add(current)
                for (i in 1 until traversalNodes.size) {
                    if (current.step === traversalNodes[i].step && current.parent === traversalNodes[i].parent) {
                        twinList.add(traversalNodes[i])
                    }
                }
                mergeTwins(twinList)
                traversalNodes.removeAll(twinList)
            }
        }
    }

    private fun getTraveraslNodes(value: DerivationNode): MutableList<DerivationNode> {
        val result = ArrayList<DerivationNode>()
        val stack = mutableListOf<DerivationNode>()
        stack.add(value)
        while (stack.isNotEmpty()) {
            val current = stack.pop()
            result.add(current)
            //add only children that are not looping back to root
            stack.addAll(current.children.filter { child -> child !== value })
        }
        result.reverse()
        return result
    }

    private fun mergeTwins(twinList: List<DerivationNode>) {
        //pick the first as the one to remain
        val keep = twinList[0]
        //add the children of all the others to keep one and make it their parent
        for (i in 1 until twinList.size) {
            //if either of them don't have a child add a null to account for the end of match, must  be added at end of
            //list so that DFS traversal starting on left go over longer branches before going over the ones that end with null

            if (keep.children.isEmpty() || twinList[i].children.isEmpty()) {
                if (!(keep.children.isEmpty() && twinList[i].children.isEmpty())) {
                    keep.isLeaf = true
                }
            }
            keep.children.addAll(twinList[i].children)
            //remove them from their parent
            keep.parent!!.children.remove(twinList[i])
            if (twinList[i].children.isNotEmpty()) {
                for (child in twinList[i].children) {
                    child.parent = keep
                }
            }
        }
    }

}
package piacenti.dslmaker.structures.derivationgraph

import piacenti.dslmaker.abstraction.ProductionStep
import kotlin.test.Test
import kotlin.test.assertEquals

class DerivationNodeTest {
    @Test
    fun fix_tree_should_work() {
        val root=DerivationNode(ProductionStep("root"),true)
        val child1 = DerivationNode(ProductionStep("some name"), false)
        val child2 = DerivationNode(ProductionStep("some name"), false)
        root.children.add(child1)
        root.children.add(child2)
        root.fixTreeLinks()
        assertEquals(child2.parent, root)
        assertEquals(child1.parent, root)
    }

}
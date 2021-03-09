package piacenti.dslmaker.structures

import piacenti.dslmaker.structures.derivationgraph.DerivationNode

/**
 * Created by Piacenti on 10/14/2015.
 */
class FireParameters(val node: DerivationNode, val startIndex: Int, val endIndex: Int,
                     stack: StepStack = StepStack(), matchedTokens: Collection<TokenMatch>) {
    val matchTokens: MutableList<TokenMatch> = mutableListOf()
    var astNode: ASTNode? = null
    val stack:StepStack = StepStack().addAll(stack.getAsList())

    init {
        this.matchTokens.addAll(matchedTokens)
    }

    override fun toString(): String {
        return "FireParameters{" +
                "node=" + node +
                ", startIndex=" + startIndex +
                ", endIndex=" + endIndex +
                ", stack=" + stack +
                '}'.toString()
    }
}

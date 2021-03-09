package piacenti.dslmaker.structures

import piacenti.dslmaker.abstraction.ProductionStep

/**
 * Created by Piacenti on 10/14/2015.
 */
class FoundIndex(var index: Int, var found: Boolean) {
    val fireParameters = ArrayList<FireParameters>()
    val matchTokens = ArrayList<TokenMatch>()
    val fullPath = mutableListOf<ProductionStep>()
    var astNode: ASTNode? = null


    override fun toString(): String {
        return "FoundIndex{index=$index, found=$found}"
    }
}

package piacenti.dslmaker.structures

import piacenti.dslmaker.abstraction.ProductionStep

/**
 * Created by Piacenti on 4/27/2016.
 */
class AccumulationParameters {
    val branchedFireParameters = mutableListOf<FireParameters>()
    val branchedMatchTokens = mutableListOf<TokenMatch>()
    val branchedFullPath = mutableListOf<ProductionStep>()
    val astNodes = mutableListOf<ASTNode>()


    fun copy(): AccumulationParameters {
        val result = AccumulationParameters()
        result.branchedFullPath.addAll(branchedFullPath)
        result.branchedMatchTokens.addAll(branchedMatchTokens)
        result.branchedFireParameters.addAll(branchedFireParameters)
        result.astNodes.addAll(astNodes.map { it.copy() })
        return result
    }
}

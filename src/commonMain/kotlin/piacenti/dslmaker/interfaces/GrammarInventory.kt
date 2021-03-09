package piacenti.dslmaker.interfaces

import piacenti.dslmaker.abstraction.ProductionStep


interface GrammarInventory {

    val tokens: MutableList<ProductionStep>

    val productions: MutableList<ProductionStep>

    val rules: MutableList<ProductionStep>
        get() {
            return productions
        }

    fun createToken(name: String, regex: String, matchIndex: Int = 0, context: String? = null,
                    addContext: String? = null, removeContext: String? = null, validateToken: ((String) -> Boolean)?=null): ProductionStep {
        val baseProductionStep = ProductionStep(name, regex, matchIndex, false, context, addContext, removeContext, validateToken)
        tokens.add(baseProductionStep)
        return baseProductionStep
    }

    fun createProduction(name: String): ProductionStep {
        val baseProductionStep = ProductionStep(name)
        productions.add(baseProductionStep)
        return baseProductionStep
    }

    @Suppress("unused")
    fun createRule(name: String): ProductionStep {
        return createProduction(name)
    }
}
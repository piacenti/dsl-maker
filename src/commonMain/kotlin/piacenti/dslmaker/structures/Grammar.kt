package piacenti.dslmaker.structures

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.interfaces.GrammarInventory
import piacenti.dslmaker.interfaces.MatchData


/**
 * @param <T>
 * @author Piacenti
</T> */
open class Grammar<T : GrammarInventory>(var startProduction: ProductionStep, val container: T) {
    /**
     * @return
     */
    val productions: MutableMap<ProductionStep, Production> = mutableMapOf()

    private val tokenContainsMap = HashMap<ProductionStep, Set<ProductionStep>>()

    fun wrapTokensWithProductions() {
        val alreadyProcessed = HashMap<ProductionStep, ProductionStep>()
        val toAdd = HashMap<ProductionStep, Production>()
        val productionNames = HashSet<String>()
        productions.forEach { t -> productionNames.add(t.key.name) }
        productions.forEach { t ->
            t.value.expressions.forEach { tExpression ->
                for (i in 0 until tExpression.steps.size) {
                    val step = tExpression.steps[i]
                    if (alreadyProcessed[step] == null) {
                        if (thereIsNoMatchingProductionForThisStep(step, productionNames)) {
                            try {
                                val newProductionStep = ProductionStep(
                                        step.name.uppercase().replace("_LEAF".toRegex(), ""))
                                val newProduction = Production(null)
                                newProduction.expressions.add(Expression(step))
                                toAdd[newProductionStep] = newProduction
                                alreadyProcessed[step] = newProductionStep
                                tExpression.steps[i] = newProductionStep

                            } catch (e: Error) {
                                println(e.message)
                            }

                        }
                    } else {
                        //replace step in parent with production
                        tExpression.steps[i] = alreadyProcessed[step]!!
                    }
                }
            }
        }
        productions.putAll(toAdd)
    }

    private fun thereIsNoMatchingProductionForThisStep(step: ProductionStep, productionNames: Set<String>): Boolean {
        if (step.isProduction) return false
        //productions by convention should be upper case
        var name = step.name.uppercase()
        //tokens following the _LEAF ending convention should be disconsidered
        if (name.endsWith("_LEAF")) {
            name = name.replace("_LEAF", "")
        }
        return !productionNames.contains(name)
    }

    fun clearAllActions(): Grammar<T> {
        productions.entries.forEach { tProductionEntry -> tProductionEntry.value.action = null }
        return this
    }

    fun getAllTokensContainingProduction(production: ProductionStep): Set<ProductionStep> {
        val value = tokenContainsMap[production]
        if (value != null) {
            return value
        }
        val result = HashSet<ProductionStep>()
        productions.entries.forEach { prod ->
            prod.value.expressions.forEach { exp ->
                exp.steps.forEach { step ->
                    if (step === production) {
                        result.add(prod.key)
                    }
                }
            }
        }
        tokenContainsMap[production] = result
        return result
    }

    override fun toString(): String {
        val result = StringBuilder()
        result.append(startProduction).append(" -> ").append(productions[startProduction]).append("\n")
        productions.entries.filter { entry -> entry.key !== startProduction }.sortedBy { it.toString() }.forEach { entry ->
            result.append(entry.key).append(" -> ")
            result.append(entry.value).append("\n")
        }
        return result.toString()
    }

    fun p(value: ProductionStep, vararg exps: Expression) {
        if (productions[value] == null) {
            productions[value] = Production(null)
        }
        productions[value]!!.expressions.clear()
        productions[value]!!.expressions.addAll(exps)
    }

    fun r(value: ProductionStep, vararg exps: Expression) {
        p(value, *exps)
    }

    fun pAppend(value: ProductionStep, vararg exps: Expression) {
        if (productions[value] == null) {
            productions[value] = Production(null)
        }
        productions[value]!!.expressions.addAll(exps)
    }

    fun a(value: ProductionStep, action: ((MatchData) -> Unit)?) {
        if (value.isProduction) {
            if (productions[value] == null) {
                productions[value] = Production(null)
            }
            productions[value]!!.action = action
        }
    }


    fun e(vararg exps: ProductionStep): Expression {
        return Expression(*exps)
    }
}
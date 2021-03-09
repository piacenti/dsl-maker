package piacenti.dslmaker.structures

import piacenti.dslmaker.abstraction.ProductionStep

/**
 * @param <T>
 * @author Piacenti
</T> */
class Expression {
    val steps = mutableListOf<ProductionStep>()
    /**
     * @param steps
     */
    constructor(vararg steps: ProductionStep) {
        this.steps.addAll(steps)
    }

    constructor(steps: List<ProductionStep>) {
        this.steps.addAll(steps)
    }


    override fun toString(): String {
        val result = StringBuilder()
        for (step in steps) {
            result.append(step).append(" ")
        }
        return result.removeSuffix(" ").toString()
    }
}
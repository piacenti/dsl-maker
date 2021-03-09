package piacenti.dslmaker.structures

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.removeLast

/**
 * @param <T>
 * @author Piacenti
</T> */
@Suppress("unused")
class StepStack {

    private val list = mutableListOf<ProductionStep>()
    fun getAsList(): List<ProductionStep> {
        return list.toList()
    }
    fun getFirstFoundInCallStackFromList(steps: Collection<ProductionStep>): ProductionStep? {
        for (i in list.size - 1 downTo -1 + 1) {
            for (step in steps) {
                if (step === list[i]) {
                    return list[i]
                }
            }

        }
        return null
    }

    /**
     * @param steps
     * @return
     */
    fun getFirstFoundInCallStackFromList(vararg steps: ProductionStep): ProductionStep? {
        for (i in list.size - 1 downTo -1 + 1) {
            for (step in steps) {
                if (step === list[i]) {
                    return list[i]
                }
            }

        }
        return null
    }

    /**
     * @return
     */
    fun removeLast(): StepStack {
        list.removeLast()
        return this
    }

    /**
     * @param e
     * @return
     */
    fun add(e: ProductionStep): StepStack {
        list.add(e)
        return this
    }

    override fun toString(): String {
        return "StepStack{" +
                "stack=" + list +
                '}'
    }

    fun addAll(generalStack: Collection<ProductionStep>): StepStack {
        list.addAll(generalStack)
        return this
    }
}
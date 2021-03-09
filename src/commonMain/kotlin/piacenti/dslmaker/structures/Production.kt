package piacenti.dslmaker.structures

import piacenti.dslmaker.interfaces.MatchData


/**
 * @param <T>
 * @author Piacenti
</T> */
class Production{
    var action: ((MatchData) -> Unit)? = null
    /**
     * @return
     */
    val expressions = mutableListOf<Expression>()


    /**
     * @param action
     */
    constructor(action: ((MatchData) -> Unit)? = null) {
        this.action = action
    }

    constructor(action: ((MatchData) -> Unit)? = null, vararg exp: Expression) {
        this.action = action
        expressions.addAll(exp)
    }

    constructor(action: ((MatchData) -> Unit)? = null, exp: List<Expression>) {
        this.action = action
        expressions.addAll(exp)
    }


    override fun toString(): String {
        val result = StringBuilder()
        for (expression in expressions) {
            result.append(expression.toString()).append(" | ")
        }

        return result.removeSuffix(" | ").toString()
    }
}
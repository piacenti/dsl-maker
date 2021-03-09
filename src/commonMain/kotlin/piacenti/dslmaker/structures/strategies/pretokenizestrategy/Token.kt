package piacenti.dslmaker.structures.strategies.pretokenizestrategy

import piacenti.dslmaker.interfaces.TokenDefinition

/**
 * @author Piacenti
 */
@Suppress("unused")
class Token(val definition: TokenDefinition, val value: String, private val untrimmed: String, val startIndex: Int, val endIndex: Int) {

    fun print(): String {
        return untrimmed
    }

    override fun toString(): String {
        return "Token{" +
                "constant=" + definition +
                ", value='" + value + '\''.toString() +
                '}'.toString()
    }
}
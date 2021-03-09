package piacenti.dslmaker.abstraction

import piacenti.dslmaker.interfaces.ProductionStepInterface

/**
 * @author admin
 */
data class ProductionStep(val name: String, override val regexDefinition: String,
                          override val matchFilter: Int,
                          override val isProduction: Boolean, override val context: String? = null,
                          override val addContext: String? = null,
                          override val removeContext: String? = null,
                          override val validateToken: ((String) -> Boolean)?=null) : ProductionStepInterface {


    constructor(name: String, regexDefinition: String) : this(name, regexDefinition, 0, false)
    constructor(name: String) : this(name, "", 0, true)

    override fun toString(): String {
        return if (isProduction) {
            name
        } else {
            "($regexDefinition)"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProductionStep

        if (name != other.name) return false
        if (regexDefinition != other.regexDefinition) return false
        if (matchFilter != other.matchFilter) return false
        if (isProduction != other.isProduction) return false
        if (context != other.context) return false
        if (addContext != other.addContext) return false
        if (removeContext != other.removeContext) return false
        if (validateToken != other.validateToken) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 92821 * result + regexDefinition.hashCode()
        result = 92821 * result + matchFilter
        result += isProduction.hashCode()
        return result
    }


}
package piacenti.dslmaker.interfaces

/**
 * @author Piacenti
 */
interface TokenDefinition {


    /**
     * @return
     */
    val regexDefinition: String
    val matchFilter: Int
    val addContext: String?
    val removeContext: String?
    val context: String?
    val validateToken:((String) -> Boolean)?
}
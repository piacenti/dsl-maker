package piacenti.dslmaker.structures

import piacenti.dslmaker.abstraction.ProductionStep

/**
 * Created by Piacenti on 7/19/2016.
 */
@Suppress("unused")
class TokenMatch(var endIndex: Int, var startIndex: Int, var text: String?, var token: ProductionStep?)

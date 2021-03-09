package piacenti.dslmaker.interfaces

import piacenti.dslmaker.structures.ASTNode
import piacenti.dslmaker.structures.StepStack
import piacenti.dslmaker.structures.TokenMatch

/**
 *
 * @author Piacenti
 * @param <T>
</T> */
data class MatchData(val match: String, val stack: StepStack,
                                                  val matchedTokens: List<TokenMatch>, val startIndex: Int,
                                                  val trimmedStartIndex: Int, val endIndex: Int,
                                                  val astNode: ASTNode)
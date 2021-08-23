package piacenti.dslmaker.dsl.antlr

import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.atn.ActionTransition
import org.antlr.v4.kotlinruntime.atn.AtomTransition
import org.antlr.v4.kotlinruntime.atn.BasicBlockStartState
import org.antlr.v4.kotlinruntime.atn.BasicState
import org.antlr.v4.kotlinruntime.atn.BlockEndState
import org.antlr.v4.kotlinruntime.atn.EpsilonTransition
import org.antlr.v4.kotlinruntime.atn.LoopEndState
import org.antlr.v4.kotlinruntime.atn.PlusBlockStartState
import org.antlr.v4.kotlinruntime.atn.PlusLoopbackState
import org.antlr.v4.kotlinruntime.atn.PrecedencePredicateTransition
import org.antlr.v4.kotlinruntime.atn.RuleStartState
import org.antlr.v4.kotlinruntime.atn.RuleStopState
import org.antlr.v4.kotlinruntime.atn.RuleTransition
import org.antlr.v4.kotlinruntime.atn.SetTransition
import org.antlr.v4.kotlinruntime.atn.StarBlockStartState
import org.antlr.v4.kotlinruntime.atn.StarLoopEntryState
import org.antlr.v4.kotlinruntime.atn.StarLoopbackState
import org.antlr.v4.kotlinruntime.atn.Transition
import piacenti.dslmaker.pop
data class ExpectationData(val id:Int, val ruleStack:List<Int>)
data class AntlrCompletionData(val tokens: Set<ExpectationData>, val rules: Set<ExpectationData>, val highestTokenIndexReached: Int, val highestTextIndexReached: Int)
class AntlrCompletionSuggester {
    fun suggestCompletions(parser: Parser): AntlrCompletionData {
        val tokenStream = parser.tokenStream!! as CommonTokenStream
        val allDefaultChannelTokenTypes = mutableListOf<Int>()
        val allTokens = mutableListOf<Token>()
        for (i in 0 until tokenStream.size()) {
            allTokens.add(tokenStream[i])
        }
        val allDefaultChannelTokens = allTokens.filter { it.channel == 0 }
        allDefaultChannelTokenTypes.addAll(allDefaultChannelTokens.map { it.type })
        //inject a fake token to replace EOF to make sure nothing will match it
        //so that we can exhaustively search for all rules that attempt to match at index
        allDefaultChannelTokenTypes[allDefaultChannelTokenTypes.lastIndex] = -10
        val startRuleState = parser.atn.ruleToStartState!![0]!!

        val result = runSimulation(parser, startRuleState, allDefaultChannelTokenTypes)
        //stop text index of highest reached normal token
        //we just wont the stopIndex of whatever came before the highest index token because it means it got matched

        //we don't want the fake token or EOF, just whatever came before it
        val tokenIndex = allDefaultChannelTokens.getOrNull(result.highestTokenIndexReached - 1)?.tokenIndex
        val highestReachedTextIndex = if (tokenIndex != null) {
            val lastHiddenTokenToRight = tokenStream.getHiddenTokensToRight(tokenIndex)?.lastOrNull()
            lastHiddenTokenToRight?.stopIndex
                    ?: allDefaultChannelTokens.getOrNull(result.highestTokenIndexReached - 1)?.stopIndex ?: 0
        } else 0

        return result.copy(highestTextIndexReached = highestReachedTextIndex)
    }

    data class Rule(val ruleIndex: Int, val pathStack: MutableList<RulePath> = mutableListOf()/*,
                    val pathsToFailure: MutableList<MutableList<Int>> = mutableListOf(),
                    val pathToMatch: MutableList<Int> = mutableListOf()*/,
                    val precedence:Int?=null) {
        val statesSeen = mutableSetOf<Int>()
        val startTokenIndex: Int = pathStack.first().tokenIndex
        /*val stopTokenIndex: Int
            get() = pathStack.last().tokenIndex*/
    }

    enum class TransitionType {
        REGULAR, FOLLOWSET
    }

    data class RulePath(val iterator: Iterator<Transition>,
                        var tokenIndex: Int,
                        val statesPathOfMatch: MutableList<Int>, //should be copy of previous ATNState
                        val pathOfMatch: MutableList<Int>,/*should be copy of previous path state
                        should be specific for each branch*/
                        val tokensMatched: MutableList<Int>,
                        val transitionType: TransitionType = TransitionType.REGULAR,
                        ) {
        val startState = statesPathOfMatch.first()
        val stopState = statesPathOfMatch.last()
    }

    data class StateIndexPair(val stateNumber: Int, val index: Int)

    private fun runSimulation(parser: Parser, startRuleState: RuleStartState, allTokens: MutableList<Int>): AntlrCompletionData {
        var iterationCount = 0;
        var highestReachedIndex = 0
        val expectedTokens = mutableSetOf<ExpectationData>()
        val expectedRules = mutableSetOf<ExpectationData>()
        val ruleStack = mutableListOf<Rule>()
        ruleStack.add(Rule(startRuleState.ruleIndex, mutableListOf(RulePath(startRuleState.transitions.iterator(), 0,
                mutableListOf(startRuleState.stateNumber), mutableListOf(), mutableListOf()))))
        val stateIndexPairs = mutableSetOf<StateIndexPair>()
        while (ruleStack.isNotEmpty()) {
            val currentRule = ruleStack.last()
            val branchStack = currentRule.pathStack
            if (branchStack.isEmpty()) {
                val failedRule = ruleStack.pop()
                //rule failed if all branches were traversed and it is still not matched (if it was matched it would
                //have been popped in the state processing). We remove it here
                if (ruleStack.isNotEmpty())
                    ruleStack.last().pathStack.pop()
                if (startingRulePointMatchesEndToken(ruleStack, failedRule, highestReachedIndex))
                    expectedRules.add(ExpectationData(failedRule.ruleIndex, ruleStack.map { it.ruleIndex }))
                continue
            }
            inner@ while (branchStack.isNotEmpty()) {
                var succeededTransition = true
                val currentBranch = branchStack.last()
                if (currentBranch.iterator.hasNext()) {
                    val transition = currentBranch.iterator.next()
                    val target = transition.target!!
                    val statesPathOfMatch = currentBranch.statesPathOfMatch.toMutableList()
                    val pathOfMatch = currentBranch.pathOfMatch.toMutableList()
                    val tokensMatched = currentBranch.tokensMatched.toMutableList()
                    statesPathOfMatch.add(target.stateNumber)
                    currentRule.statesSeen.add(target.stateNumber)
                    val startState = parser.atn.states[currentBranch.startState]!!
                    //transitions deal with path stack
                    var currentTokenIndex = currentBranch.tokenIndex
                    val stateIndexPair = StateIndexPair(target.stateNumber, currentTokenIndex)
                    var precedence:Int?=null
                    if (!stateIndexPairs.contains(stateIndexPair)) {
//                        iterationCount++
//                        if (currentTokenIndex >= highestReachedIndex)
//                            println("iteration $iterationCount rule name '${parser.ruleNames!![currentRule.ruleIndex]}' index $highestReachedIndex state ${target.stateNumber}")
                        when (transition) {
                            is EpsilonTransition, is ActionTransition, is PrecedencePredicateTransition-> {
                                branchStack.add(RulePath(target.transitions.iterator(),
                                        currentTokenIndex,
                                        statesPathOfMatch, pathOfMatch, tokensMatched))
                            }
//                            is PrecedencePredicateTransition->{
//                                if(currentRule.precedence==null || currentRule.precedence>=transition.precedence){
//                                    branchStack.add(RulePath(target.transitions.iterator(),
//                                        currentTokenIndex,
//                                        statesPathOfMatch, pathOfMatch, tokensMatched))
//                                }
//                            }
                            is RuleTransition -> {
                                //fabricate epsilon transition to follow state
                                val fabricatedTransition = EpsilonTransition(transition.followState)
                                branchStack.add(RulePath(listOf(fabricatedTransition).iterator(),
                                        currentTokenIndex,
                                        statesPathOfMatch, pathOfMatch, tokensMatched, TransitionType.FOLLOWSET))
                                pathOfMatch.add(transition.ruleIndex)
                                precedence=transition.precedence
                            }
                            is AtomTransition -> {
                                val currentToken = allTokens[currentTokenIndex]
                                val expectedToken = transition.label
                                if (currentToken == -10) {
                                    //branch completed so clear states for other branches
                                    currentRule.statesSeen.clear()
                                    expectedTokens.add(ExpectationData(expectedToken, ruleStack.map { it.ruleIndex }))
                                    succeededTransition = false
                                } else if (expectedToken == currentToken) {
                                    tokensMatched.add(currentToken)
                                    //if we advanced the input we can allow ourselves to revisit states
                                    currentRule.statesSeen.clear()
                                    currentTokenIndex++
                                    if (currentTokenIndex > highestReachedIndex) {
//                                    println("matched token $currentToken")
                                        highestReachedIndex = currentTokenIndex
                                        expectedTokens.clear()
                                        expectedRules.clear()
                                    }
                                    branchStack.add(RulePath(target.transitions.iterator(),
                                            currentTokenIndex,
                                            statesPathOfMatch, pathOfMatch, tokensMatched))
                                } else if (currentTokenIndex == highestReachedIndex) {
                                    expectedTokens.add(ExpectationData(expectedToken, ruleStack.map { it.ruleIndex }))
                                    succeededTransition = false
                                }
                            }
                            is SetTransition -> {
                                val currentToken = allTokens[currentTokenIndex]
                                val expectedRuleTokens = transition.set.toList()
                                if (currentToken == -10) {
                                    //branch completed so clear states for other branches
                                    currentRule.statesSeen.clear()
                                    val currentStack = ruleStack.map { it.ruleIndex }
                                    expectedTokens.addAll(expectedRuleTokens.map { ExpectationData(it, currentStack) })
                                    succeededTransition = false
                                } else if (expectedRuleTokens.contains(currentToken)) {
                                    tokensMatched.add(currentToken)
                                    //if we advanced the input we can allow ourselves to revisit states
                                    currentRule.statesSeen.clear()
                                    currentTokenIndex++
                                    if (currentTokenIndex > highestReachedIndex) {
//                                    println("matched token $currentToken")
                                        highestReachedIndex = currentTokenIndex
                                        expectedTokens.clear()
                                        expectedRules.clear()
                                    }
                                    branchStack.add(RulePath(target.transitions.iterator(),
                                            currentTokenIndex,
                                            statesPathOfMatch, pathOfMatch, tokensMatched))
                                } else if (currentTokenIndex == highestReachedIndex) {
                                    val currentStack = ruleStack.map { it.ruleIndex }
                                    expectedTokens.addAll(expectedRuleTokens.map { ExpectationData(it, currentStack) })
                                    succeededTransition = false
                                }
                            }
                            else -> throw UnsupportedOperationException(transition::class.simpleName)
                        }
                        //no point in processing target of transition if it fails
                        if (succeededTransition) {
                            //state deals with matching of a rule
                            when (target) {
                                /*
                        I believe that there may issues in case I use rules with PlusBlockStartState and StarLoopEntryState
                        since those are simply branching blocks rather than specifying that a new rule will be parsed.
                        It seems that RuleStartState is still the for sure way to determine when to add something to the stack.
                        For now I'm leaving the others around since I got them from someone else's code that processed them together.
                        My approach may very well be different than theirs.
                        for reference see https://www.antlr.org/api/Java/org/antlr/v4/runtime/atn/ATNState.html
                        * */
                                is RuleStartState -> {
//                                println("adding rule ${parser.ruleNames!![target.ruleIndex]}")
                                    ruleStack.add(Rule(target.ruleIndex, mutableListOf(RulePath(target.transitions.iterator(),
                                            currentTokenIndex,
                                            mutableListOf(target.stateNumber), mutableListOf(), mutableListOf())),precedence))
                                    break@inner
                                }
                                is RuleStopState -> {
                                    if (startState is RuleStartState && startState.stopState == target) {
                                        val matched = ruleStack.pop()
                                        //since fully matched clear all seen states for this rule
                                        // in case this rule is matched from a different context for the same index
                                        //this also clears it for other indexes since we don't quite keep an accurate
                                        //representation of each state to index specially since we update the tokenIndex
                                        //of a parent rule once a child one matches. This will result in additional
                                        //computations but should still avoid infinite loops
                                        matched.pathStack.last().statesPathOfMatch.forEach { stateNumber ->
                                            stateIndexPairs.removeAll { it.stateNumber == stateNumber }
                                        }
                                        updateParentLastTokenParsed(ruleStack, matched, currentTokenIndex)
                                        break@inner
                                    }
                                }
                                is BasicState,
                                is LoopEndState,
                                is BasicBlockStartState,
                                is BlockEndState,
                                is StarLoopbackState,
                                is PlusLoopbackState,
                                is StarBlockStartState,
                                is PlusBlockStartState,
                                is StarLoopEntryState -> {
                                    //ignore
                                }
                                else -> throw UnsupportedOperationException(target::class.simpleName)
                            }
                            stateIndexPairs.add(stateIndexPair)
                        }
                    }

                } else {
                    //failed branch
                    //if current branch has no more iterations left then it failed
                    val failedBranch = branchStack.pop()
//                    currentRule.pathsToFailure.add(failedBranch.pathOfMatch)
                }
            }

        }
        return AntlrCompletionData(expectedTokens, expectedRules, highestReachedIndex, 0)
    }

    private fun startingRulePointMatchesEndToken(ruleStack: List<Rule>, failedRule: Rule, highestReachedIndex: Int): Boolean {
        var last = failedRule
        //in case of recursive rule injection we go to earliest of consecutive entries in the stack to determine if
        //rule should be added
        for (i in ruleStack.lastIndex.downTo(0)) {
            val current = ruleStack[i]
            if (current.ruleIndex == last.ruleIndex)
                last = current
            else
                break
        }
        return last.startTokenIndex == highestReachedIndex

    }

    private fun updateParentLastTokenParsed(ruleStack: List<Rule>, matched: Rule, currentTokenIndex: Int) {
        ruleStack.lastOrNull()?.let { toUpdate ->
            if (toUpdate.pathStack.last().tokenIndex < currentTokenIndex) {
                toUpdate.pathStack.last().tokenIndex = currentTokenIndex
                toUpdate.pathStack.last().tokensMatched.addAll(matched.pathStack.last().tokensMatched)
            }
        }
    }
}
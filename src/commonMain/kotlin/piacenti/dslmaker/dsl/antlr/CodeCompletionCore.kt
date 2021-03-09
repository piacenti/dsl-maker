package piacenti.dslmaker.dsl.antlr

import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.atn.*
import org.antlr.v4.kotlinruntime.misc.IntervalSet
import piacenti.dslmaker.pop


/**
 * Port of antlr-c3 javascript library to java
 *
 *
 * The c3 engine is able to provide code completion candidates useful for
 * editors with ANTLR generated parsers, independent of the actual
 * language/grammar used for the generation.
 */
class CodeCompletionCore(val parser: Parser, val preferredRules: MutableSet<Int> = mutableSetOf(), val ignoredTokens: MutableSet<Int> = mutableSetOf()) {
    /**
     * JDO returning information about matching tokens and rules
     */
    class CandidatesCollection {
        /**
         * Collection of Token ID candidates, each with a follow-on List of
         * subsequent tokens
         */
        var tokens: MutableMap<Int, MutableList<Int>> = mutableMapOf()

        /**
         * Collection of Rule candidates, each with the callstack of rules to
         * reach the candidate
         */
        var rules: MutableMap<Int, MutableList<Int>> = mutableMapOf()

        /**
         * Collection of matched Preferred Rules each with their start and end
         * offsets
         */
        var rulePositions: MutableMap<Int, MutableList<Int>> = mutableMapOf()
        override fun toString(): String {
            return "CandidatesCollection{tokens=$tokens, rules=$rules, ruleStrings=$rulePositions}"
        }
    }

    class FollowSetWithPath {
        var intervals: IntervalSet = IntervalSet()
        var path: MutableList<Int> = mutableListOf()
        var following: MutableList<Int> = mutableListOf()
    }

    class FollowSetsHolder {
        var sets: MutableList<FollowSetWithPath> = mutableListOf()
        var combined: IntervalSet = IntervalSet()
    }

    class PipelineEntry(var state: ATNState, var tokenIndex: Int) {

    }

    private val atn: ATN = parser.atn
    private var tokens: MutableList<Token> = mutableListOf()
    private var tokenStartIndex = 0
    private var statesProcessed = 0

    // A mapping of rule index to token stream position to end token positions.
    // A rule which has been visited before with the same input position will always produce the same output positions.
    private val shortcutMap: MutableMap<Int, MutableMap<Int, MutableSet<Int>>> = mutableMapOf()
    private val candidates: CandidatesCollection = CandidatesCollection() // The collected candidates (rules and tokens).


    /**
     * This is the main entry point. The caret token index specifies the token stream index for the token which currently
     * covers the caret (or any other position you want to get code completion candidates for).
     * Optionally you can pass in a parser rule context which limits the ATN walk to only that or called rules. This can significantly
     * speed up the retrieval process but might miss some candidates (if they are outside of the given context).
     */
    fun collectCandidates(caretTokenIndex: Int, context: ParserRuleContext?): CandidatesCollection {
        shortcutMap.clear()
        candidates.rules.clear()
        candidates.tokens.clear()
        statesProcessed = 0
        tokenStartIndex = context?.start?.tokenIndex ?: 0
        val tokenStream: TokenStream = parser.tokenStream!!
        val currentIndex: Int = tokenStream.index()
        tokenStream.seek(tokenStartIndex)
        tokens = mutableListOf()
        var offset = 1
        while (true) {
            val token: Token = tokenStream.LT(offset++)!!
            tokens.add(token)
            if (token.tokenIndex >= caretTokenIndex || token.type == Token.EOF) {
                break
            }
        }
        tokenStream.seek(currentIndex)
        val callStack: MutableList<Int> = mutableListOf()
        val startRule = context?.ruleIndex ?: 0
        processRule(atn.ruleToStartState!![startRule]!!, 0, callStack, "\n")
        tokenStream.seek(currentIndex)

        // now post-process the rule candidates and find the last occurrences
        // of each preferred rule and extract its start and end in the input stream
        for (ruleId in preferredRules) {
            val shortcut = shortcutMap[ruleId]
            if (shortcut == null || shortcut.isEmpty()) {
                continue
            }
            // select the right-most occurrence
            val startToken: Int = shortcut.keys.max()!!
            val endSet = shortcut[startToken]
            val endToken: Int
            endToken = if (endSet!!.isEmpty()) {
                tokens.size - 1
            } else {
                shortcut[startToken]!!.max()!!
            }
            val startOffset: Int = tokens[startToken].startIndex
            val endOffset: Int
            endOffset = if (tokens[endToken].type == Token.EOF) {
                // if last token is EOF, include trailing whitespace
                tokens[endToken].startIndex
            } else {
                // if last token is not EOF, limit to matching tokens which excludes trailing whitespace
                tokens[endToken - 1].stopIndex + 1
            }
            val ruleStartStop: MutableList<Int> = mutableListOf(startOffset, endOffset)
            candidates.rulePositions[ruleId] = ruleStartStop
        }
        return candidates
    }

    /**
     * Check if the predicate associated with the given transition evaluates to true.
     */
    private fun checkPredicate(transition: PredicateTransition): Boolean {
        return transition.predicate.eval(parser, ParserRuleContext())
    }

    /**
     * Walks the rule chain upwards to see if that matches any of the preferred rules.
     * If found, that rule is added to the collection candidates and true is returned.
     */
    private fun translateToRuleIndex(ruleStack: MutableList<Int>): Boolean {
        if (preferredRules.isEmpty()) return false

        // Loop over the rule stack from highest to lowest rule level. This way we properly handle the higher rule
        // if it contains a lower one that is also a preferred rule.
        for (i in ruleStack.indices) {
            if (preferredRules.contains(ruleStack[i])) {
                // Add the rule to our candidates list along with the current rule path,
                // but only if there isn't already an entry like that.
                val path: MutableList<Int> = ruleStack.subList(0, i).toMutableList()
                var addNew = true
                for (entry in candidates.rules.entries) {
                    if (entry.key != ruleStack[i] || entry.value.size != path.size) {
                        continue
                    }
                    // Found an entry for this rule. Same path? If so don't add a new (duplicate) entry.
                    if (path == entry.value) {
                        addNew = false
                        break
                    }
                }
                if (addNew) {
                    candidates.rules[ruleStack[i]] = path
                }
                return true
            }
        }
        return false
    }

    /**
     * This method follows the given transition and collects all symbols within the same rule that directly follow it
     * without intermediate transitions to other rules and only if there is a single symbol for a transition.
     */
    private fun getFollowingTokens(initialTransition: Transition): MutableList<Int> {
        val result: MutableList<Int> = mutableListOf()
        val pipeline: MutableList<ATNState> = mutableListOf()
        pipeline.add(initialTransition.target!!)
        while (!pipeline.isEmpty()) {
            val state: ATNState = pipeline.pop()
            for (transition in state.transitions) {
                if (transition.serializationType == Transition.ATOM) {
                    if (!transition.isEpsilon) {
                        val list: MutableList<Int> = transition.accessLabel()!!.toList().toMutableList()
                        if (list.size == 1 && !ignoredTokens.contains(list[0])) {
                            result.add(list[0])
                            pipeline.add(transition.target!!)
                        }
                    } else {
                        pipeline.add(transition.target!!)
                    }
                }
            }
        }
        return result
    }

    /**
     * Entry point for the recursive follow set collection function.
     */
    private fun determineFollowSets(start: ATNState, stop: ATNState): MutableList<FollowSetWithPath> {
        val result: MutableList<FollowSetWithPath> = mutableListOf()
        val seen: MutableSet<ATNState> = mutableSetOf()
        val ruleStack: MutableList<Int> = mutableListOf()
        collectFollowSets(start, stop, result, seen, ruleStack)
        return result
    }

    /**
     * Collects possible tokens which could be matched following the given ATN state. This is essentially the same
     * algorithm as used in the LL1Analyzer class, but here we consider predicates also and use no parser rule context.
     */
    private fun collectFollowSets(s: ATNState, stopState: ATNState, followSets: MutableList<FollowSetWithPath>,
                                  seen: MutableSet<ATNState>, ruleStack: MutableList<Int>) {
        if (seen.contains(s)) return
        seen.add(s)
        if (s.equals(stopState) || s.stateType == ATNState.RULE_STOP) {
            val set = FollowSetWithPath()
            set.intervals = IntervalSet.of(Token.EPSILON)
            set.path = ruleStack.toMutableList()
            followSets.add(set)
            return
        }
        for (transition in s.transitions) {
            if (transition.serializationType == Transition.RULE) {
                val ruleTransition: RuleTransition = transition as RuleTransition
                if (ruleStack.indexOf(ruleTransition.target!!.ruleIndex) != -1) {
                    continue
                }
                ruleStack.add(ruleTransition.target!!.ruleIndex)
                collectFollowSets(transition.target!!, stopState, followSets, seen, ruleStack)
                ruleStack.pop()
            } else if (transition.serializationType == Transition.PREDICATE) {
                if (checkPredicate(transition as PredicateTransition)) {
                    collectFollowSets(transition.target!!, stopState, followSets, seen, ruleStack)
                }
            } else if (transition.isEpsilon) {
                collectFollowSets(transition.target!!, stopState, followSets, seen, ruleStack)
            } else if (transition.serializationType == Transition.WILDCARD) {
                val set = FollowSetWithPath()
                set.intervals = IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType)
                set.path = ruleStack.toMutableList()
                followSets.add(set)
            } else {
                var label: IntervalSet? = transition.accessLabel()
                if (label != null && label.size() > 0) {
                    if (transition.serializationType == Transition.NOT_SET) {
                        label = label.complement(IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType))!!
                    }
                    val set = FollowSetWithPath()
                    set.intervals = label
                    set.path = ruleStack.toMutableList()
                    set.following = getFollowingTokens(transition)
                    followSets.add(set)
                }
            }
        }
    }

    /**
     * Walks the ATN for a single rule only. It returns the token stream position for each path that could be matched in this rule.
     * The result can be empty in case we hit only non-epsilon transitions that didn't match the current input or if we
     * hit the caret position.
     */
    private fun processRule(startState: ATNState, tokenIndex: Int, callStack: MutableList<Int>, indentation: String): MutableSet<Int> {

        // Start with rule specific handling before going into the ATN walk.

        // Check first if we've taken this path with the same input before.
        var indentation = indentation
        var positionMap = shortcutMap[startState.ruleIndex]
        if (positionMap == null) {
            positionMap = mutableMapOf()
            shortcutMap[startState.ruleIndex] = positionMap
        } else {
            if (positionMap.containsKey(tokenIndex)) {
                return positionMap[tokenIndex]!!
            }
        }
        val result: MutableSet<Int> = mutableSetOf()

        // For rule start states we determine and cache the follow set, which gives us 3 advantages:
        // 1) We can quickly check if a symbol would be matched when we follow that rule. We can so check in advance
        //    and can save us all the intermediate steps if there is no match.
        // 2) We'll have all symbols that are collectable already together when we are at the caret when entering a rule.
        // 3) We get this lookup for free with any 2nd or further visit of the same rule, which often happens
        //    in non trivial grammars, especially with (recursive) expressions and of course when invoking code completion
        //    multiple times.
        var setsPerState = followSetsByATN!![parser::class.simpleName]
        if (setsPerState == null) {
            setsPerState = mutableMapOf()
            followSetsByATN[parser::class.simpleName] = setsPerState
        }
        var followSets = setsPerState[startState.stateNumber]
        if (followSets == null) {
            followSets = FollowSetsHolder()
            setsPerState[startState.stateNumber] = followSets
            val stop: RuleStopState = atn.ruleToStopState!!.get(startState.ruleIndex)!!
            followSets.sets = determineFollowSets(startState, stop)

            // Sets are split by path to allow translating them to preferred rules. But for quick hit tests
            // it is also useful to have a set with all symbols combined.
            val combined = IntervalSet()
            for (set in followSets.sets) {
                combined.addAll(set.intervals)
            }
            followSets.combined = combined
        }
        callStack.add(startState.ruleIndex)
        var currentSymbol: Int = tokens[tokenIndex].type
        if (tokenIndex >= tokens.size - 1) { // At caret?
            if (preferredRules.contains(startState.ruleIndex)) {
                // No need to go deeper when collecting entries and we reach a rule that we want to collect anyway.
                translateToRuleIndex(callStack)
            } else {
                // Convert all follow sets to either single symbols or their associated preferred rule and add
                // the result to our candidates list.
                for (set in followSets.sets) {
                    val fullPath: MutableList<Int> = callStack.toMutableList()
                    fullPath.addAll(set.path)
                    if (!translateToRuleIndex(fullPath)) {
                        for (symbol in set.intervals.toList()) {
                            if (!ignoredTokens.contains(symbol)) {
                                if (!candidates.tokens.containsKey(symbol)) candidates.tokens[symbol] = set.following // Following is empty if there is more than one entry in the set.
                                else {
                                    // More than one following list for the same symbol.
                                    if (candidates.tokens[symbol] != set.following) { // XXX js uses !=
                                        candidates.tokens[symbol] = mutableListOf()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            callStack.pop()
            return result
        } else {
            // Process the rule if we either could pass it without consuming anything (epsilon transition)
            // or if the current input symbol will be matched somewhere after this entry point.
            // Otherwise stop here.
            if (!followSets.combined.contains(Token.EPSILON) && !followSets.combined.contains(currentSymbol)) {
                callStack.pop()
                return result
            }
        }

        // The current state execution pipeline contains all yet-to-be-processed ATN states in this rule.
        // For each such state we store the token index + a list of rules that lead to it.
        val statePipeline: MutableList<PipelineEntry> = mutableListOf()
        var currentEntry: PipelineEntry

        // Bootstrap the pipeline.
        statePipeline.add(PipelineEntry(startState, tokenIndex))
        while (!statePipeline.isEmpty()) {
            currentEntry = statePipeline.pop()
            ++statesProcessed
            currentSymbol = tokens[currentEntry.tokenIndex].type
            val atCaret = currentEntry.tokenIndex >= tokens.size - 1
            if (currentEntry.state.stateType == ATNState.RULE_START) indentation += "  "
            if (currentEntry.state.stateType == ATNState.RULE_STOP) {

                // Record the token index we are at, to report it to the caller.
                result.add(currentEntry.tokenIndex)
                continue
            }
            val transitions: MutableList<Transition> = currentEntry.state.transitions
            for (transition in transitions) {
                if (transition.serializationType == Transition.RULE) {
                    val endStatus = processRule(transition.target!!, currentEntry.tokenIndex, callStack, indentation)
                    for (position in endStatus) {
                        statePipeline.add(PipelineEntry((transition as RuleTransition).followState, position))
                    }
                }
                if (transition.serializationType == Transition.PREDICATE) {
                    if (checkPredicate(transition as PredicateTransition)) {
                        statePipeline.add(PipelineEntry(transition.target!!, currentEntry.tokenIndex))
                    }
                }
                if (transition.serializationType == Transition.WILDCARD) {
                    if (atCaret) {
                        if (!translateToRuleIndex(callStack)) {
                            for (token in IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType).toList()) {
                                if (!ignoredTokens.contains(token)) {
                                    candidates.tokens[token] = mutableListOf()
                                }
                            }
                        }
                    } else {
                        statePipeline.add(PipelineEntry(transition.target!!, currentEntry.tokenIndex + 1))
                    }
                } else {
                    if (transition.isEpsilon) {
                        // Jump over simple states with a single outgoing epsilon transition.
                        statePipeline.add(PipelineEntry(transition.target!!, currentEntry.tokenIndex))
                        continue
                    }
                    var set: IntervalSet? = transition.accessLabel()
                    if (set != null && set.size() > 0) {
                        if (transition.serializationType == Transition.NOT_SET) {
                            set = set.complement(IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType))
                        }
                        if (atCaret) {
                            if (!translateToRuleIndex(callStack)) {
                                val list: MutableList<Int> = set!!.toList().toMutableList()
                                val addFollowing = list.size == 1
                                for (symbol in list) {
                                    if (!ignoredTokens.contains(symbol)) {
                                        if (addFollowing) {
                                            candidates.tokens[symbol] = getFollowingTokens(transition)
                                        } else {
                                            candidates.tokens[symbol] = mutableListOf()
                                        }
                                    }
                                }
                            }
                        } else {
                            if (set!!.contains(currentSymbol)) {
                                statePipeline.add(PipelineEntry(transition.target!!, currentEntry.tokenIndex + 1))
                            }
                        }
                    }
                }
            }
        }
        callStack.pop()

        // Cache the result, for later lookup to avoid duplicate walks.
        positionMap[tokenIndex] = result
        return result
    }

    companion object {
        private val followSetsByATN: MutableMap<String?, MutableMap<Int, FollowSetsHolder?>?>? = mutableMapOf()
    }

}
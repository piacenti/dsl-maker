package piacenti.dslmaker

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.errors.ParserException
import piacenti.dslmaker.interfaces.MatchData
import piacenti.dslmaker.structures.*
import piacenti.dslmaker.structures.derivationgraph.DerivationGraph
import piacenti.dslmaker.structures.derivationgraph.DerivationNode

/**
 * @param <T>
 * @author Piacenti
</T> */
open class ExpressionMatcher {

    /**
     * @return
     */
    lateinit var graph: DerivationGraph
        protected set
    protected lateinit var text: String

    /**
     * @return
     */
    lateinit var grammar: Grammar<*>
        protected set
    var highestSuccessfulIndex: Int = 0
    protected lateinit var lastSuccessStack: MutableList<List<ProductionStep>>
    lateinit var expectedTokenNotMatched: MutableSet<ProductionStep>
    lateinit var possibleValidTokensIfContinuingParsing: MutableSet<ProductionStep>
        protected set
    protected lateinit var matchCache: MutableMap<String, FoundIndex>

    fun match(graph: DerivationGraph, grammar: Grammar<*>, text: String, matchAllTokens: Boolean,
              startIndex: Int = 0): FoundIndex {
        this.graph = graph
        this.grammar = grammar
        this.text = text
        //        need to organize things better to allow caching, if I only cache successful results than results that depend on failures and expectedTokens not matched
        //will not work correctly
        //        int key = grammar.hashCode() + text.hashCode();
        //        if (resultCache.get(key) != null)
        //        {
        //            processResults(resultCache.get(key));
        //            return resultCache.get(key);
        //        }
        //        else
        run {
            resetGatherers()
            //grammar says where to start the parse
            val start = graph.getSubGraphs()[grammar.startProduction]?:throw ParserException("Start production has not been defined")
            val result: FoundIndex
            try {
                result = matchProduction(start, startIndex, StepStack(), false)
                if (matchAllTokens && !result.matchTokens.isEmpty() &&
                        result.matchTokens.last().endIndex < text.length - 1 || !result.found) {
                    throw ParserException("")
                }
                if (result.found) {
                    expectedTokenNotMatched.clear()
                    updateAST(text, result.astNode!!)
                    updateFireParametersWithAST(result.fireParameters, result.astNode)
                    fixIndexes(result.astNode!!)
                    processResults(result)
                }
            } catch (e: ParserException) {
                throw throwException(text, e, null)
            }

            //            resultCache.put(key, result);
            return result
        }
    }

    private fun fixIndexes(astNode: ASTNode) {
        astNode.forEachRecursive({
            it.endIndex=it.endIndex-1
        })
    }

    private fun updateFireParametersWithAST(fireParameters: ArrayList<FireParameters>, astNode: ASTNode?) {
        fireParameters.forEach { fireParameters1 ->
            astNode!!.forEachRecursive({ tastNode ->
                if (tastNode.type === fireParameters1.node.step && fireParameters1.startIndex == tastNode.startIndex &&
                        fireParameters1.endIndex == tastNode.endIndex) {
                    fireParameters1.astNode = tastNode
                }
            })
        }
    }

    private fun updateAST(text: String, astNode: ASTNode) {
        astNode.children.forEach { node ->
            node.parent = astNode
            updateAST(text, node)
        }
    }

    private fun processResults(result: FoundIndex) {
        for (i in 0 until result.fireParameters.size) {
            val p = result.fireParameters[i]
            fireSuccessAction(p)
        }
    }

    private fun throwException(text: String, e: ParserException?, result: FoundIndex?): ParserException {
        val subList: String
        val index: Int = if (result != null && !result.matchTokens.isEmpty()) {
            result.matchTokens.last().endIndex
        } else {
            highestSuccessfulIndex
        }
        subList = if (index + 1 < text.length) {
            text.substring(index + 1, text.length)
        } else {
            ""
        }
        return ParserException(
                (if (e != null) e.message + "\n" else "") + "Not all tokens were matched, highest index reached " + highestSuccessfulIndex + "\nhighest success result index: " + index +
                        "\nlast success stacks: " + lastSuccessStack + "" + "\nexpected: " + expectedTokenNotMatched + "" + "\nremaining text: " + subList)
    }

    private fun resetGatherers() {
        highestSuccessfulIndex = 0
        lastSuccessStack = ArrayList()
        expectedTokenNotMatched = HashSet()
        possibleValidTokensIfContinuingParsing = HashSet()
        matchCache = HashMap()
    }


    /**
     * This method does all the work of the matching. It consists of two recursive operations. It starts with the production being parsed which is a node that
     * contains the information given in the Grammar. The nodes in the grammar are built so that parts of a grammar that are in sequence are represented by
     * a chain of parent child nodes. If there are certain points of a grammar that may vary then the parent may have more than one child. For example, if a production
     * says X->abc|adf|a then node 'a' has two children 'b' and 'd' while 'b' has only 'c' as child and 'd' has only 'f' as child. Node a in that case may also be treated as a leaf
     * node if the longer branches are not matched so the program always tries to match the longer branches first. As indicated a match of a production is only reached
     * when a leaf node is matched, that is a node without children. This should be simple enough except for when the grammar is recursive which means that they
     * always have children, in those situations nodes that can also be leaf nodes are used for matching. For example, a production X -> aX|bX|a|b will generate
     * a node structure that has X as the root and two children 'a' and 'b' which in turn have X as child. This would end in an infinite loop if 'a' and 'b' were
     * not marked as leaf nodes. This also means that a poorly designed grammar can lead to stack overflows due to the recursion. Had X been defined as X -> aX|bX,
     * this would definitely generate a stack overflow. Another important note about the structure is that every root node is marked as root whild child nodes are not
     * so X in X -> aX|bX|a|b is marked as root node even when it comes as a child of 'a' and 'b' while other productions present in the current production are not
     * if X -> aX|bY|a|b and Y -> aY|b then for production X, every appearance of X is marked as root while the appearances of Y are not, but Y is marked as root
     * inside its own production. This is done because as the program finds productions it recursively call this method to parse them but to avoid complications
     * and to mark the match only when the toplevel production is given, the program skips the root nodes so that it keeps going down its children until a match is
     * made and mark the match as happening to the top level of the production. For example, if we did not have this in place  the match for X would
     * happen 4 times for abab, now most people are not going to be interested in the individual pieces of the recursive production but rather the whole of it
     * which is abab. If there was for sure an interest on matching each individually then multiple productions would be more appropriate such as
     * X -> AX|BX|A|B, A->a, B->b.
     *
     *
     * Whenever the program finds another production as it is parsing the nodes recursively for the current production, it calls again the matchProduction method
     * passing the grammar root production node for that node as the start. This node is the node stored in the map of production built from the grammar definitions
     * which means that it will be a node marked as root and it not the same as the node currently being parsed. So for X -> aX|bYd|a|b and Y -> yX|y if
     * when parsing X we getText to the Y node that node has child 'd' while the actual Grammar node which marked as root is the production which has child y.
     * So the actual production node is passed as start node to the matchProduction method. If a match of Y occurs after that call is made and the current Y node has
     * no children, then it would mark X as matched, but since Y has 'd' as a child it goes on to match that node also before marking X as matched.
     *
     *
     * The match object is kept as a method level object so that when the match happens anywhere down the branch in the recursion it updates it for the production
     * and stop the matching for that production.
     *
     *
     * Every time a new production is called the stack is udpated so that we know how deep we are in the matching hierarchy of productions that depend on one
     * another. This stack is also used by the user to be able to have a single action address items parsed at different levels. The  startIndex has the start
     * point match for the current production while the result of the match will contain the final matched index so that a substring of the text represent the match.
     * Note that once a production is matched it adds to the index of the result and that is done because after the call to the method that index may advance
     * several steps instead of single step as it happens when doing a simple token match.
     *
     *
     * When a match for a production occurs a fire parameter is created and added to the result. After all matching has occurred the final result should only
     * contain the Fire parameters that should actually be used to fire the action associated to the production. The stack is passed so that the level of call
     * may be determined so that same action may be used to deal with objects that may be present at different levels of the structure
     *
     * @param start
     * @param startIndex
     * @param stack
     * @return
     */
    protected open fun matchProduction(start: DerivationNode, startIndex: Int, stack: StepStack,
                                       ignoreActions: Boolean): FoundIndex {
        val key = StringBuilder().append(start.step.hashCode()).append(startIndex).toString()
        val foundIndex = matchCache[key]
        if (foundIndex != null) {
            return foundIndex
        } else {
            stack.add(start.step)
            //Depth first search approach
            val result = FoundIndex(startIndex, false)
            result.fullPath.add(start.step)

            val branchedAccumulation = AccumulationParameters()
            val action = object : LoopTraverse {
                override fun call(node: DerivationNode, index: Int,
                                  accumulatedParameters: AccumulationParameters) {
                    var index = index

                    //if node is another production then go to that tree to deal with it
                    val subGraphNode = graph.getSubGraphs()[node.step]
                    if (subGraphNode != null) {
                        val temp: FoundIndex = if (node.step === start.step) {
                            matchProduction(subGraphNode, index, stack, true)
                        } else {
                            matchProduction(subGraphNode, index, stack, false)
                        }
                        if (temp.found) {
                            //assign index when changed because in recursive calls you will never fall in the condition that has no children
                            index = temp.index
                            result.index = index
                            accumulatedParameters.branchedMatchTokens.addAll(temp.matchTokens)
                            accumulatedParameters.branchedFireParameters.addAll(temp.fireParameters)
                            accumulatedParameters.branchedFullPath.addAll(temp.fullPath)
                            accumulatedParameters.astNodes.add(temp.astNode!!)
                        } else {
                            return
                        }
                    } else {
                        //=============BASE FAIL CASES==========================
                        if (node.step.isProduction) {
                            throw ParserException("Unsupported Production: " + node.step)
                        }
                        if (index > text.length) {
                            return
                        }
                        //step must be of same type as token step, if not, do not continue recursion for its children
                        val evalText = text.substring(index)

                        val m = ("(?s)^\\s*(?:" + node.step.regexDefinition + ")").genericRegex().find(evalText)
                        if (m == null) {
                            if (index > highestSuccessfulIndex || (index == 0 && highestSuccessfulIndex == 0)) {
                                expectedTokenNotMatched.add(node.step)
                            }
                            if (evalText.isBlank()) {
                                possibleValidTokensIfContinuingParsing.add(node.step)
                                possibleValidTokensIfContinuingParsing.add(start.step)
                            }
                            return
                        }
                        //================================================

                        val endIndex = index + m.range.end
                        if (endIndex > highestSuccessfulIndex) {
                            expectedTokenNotMatched = HashSet()
                            highestSuccessfulIndex = endIndex
                            if (evalText.isNotBlank())
                                possibleValidTokensIfContinuingParsing = HashSet()
                        }
                        accumulatedParameters.branchedMatchTokens.add(
                                TokenMatch(endIndex, index, m.groupValues[0].trim(), node.step))
                        //add AST node for terminal
                        addASTNodeToTerminal(node, index, accumulatedParameters, endIndex)
                        if (node.step.regexDefinition != "") index += m.range.end
                    }
                    if (node.children.isEmpty()) {
                        setFoundResult(index, accumulatedParameters, result, ignoreActions, start, startIndex, stack)
                        return
                    }

                    for (child in node.children) {
                        if (!result.found) {
                            call(child, index, accumulatedParameters.copy())
                        } else {
                            break
                        }
                    }
                    //if nothing worked and this node can be a leaf then mark as found stopping here
                    if (node.isLeaf && !result.found) {
                        setFoundResult(index, accumulatedParameters, result, ignoreActions, start, startIndex, stack)
                        return
                    }
                }
            }
            for (child in start.children) {
                if (!result.found) {
                    action.call(child, startIndex, branchedAccumulation.copy())
                } else {
                    break
                }
            }

            if (result.found) {
                if (result.index == highestSuccessfulIndex) {
                    lastSuccessStack = ArrayList()
                    val list = mutableListOf<ProductionStep>()
                    list.addAll(stack.getAsList())
                    lastSuccessStack.add(list)
                } else if (result.index > startIndex && result.index - 1 == highestSuccessfulIndex) {
                    val list = mutableListOf<ProductionStep>()
                    list.addAll(stack.getAsList())
                    lastSuccessStack.add(list)
                }
            } else if (startIndex > highestSuccessfulIndex) {
                expectedTokenNotMatched.add(start.step)
            } else {
                result.fullPath.removeLast()
            }
            if (!result.found && startIndex > highestSuccessfulIndex) {
                possibleValidTokensIfContinuingParsing.add(start.step)
            }
            stack.removeLast()
            matchCache[key] = result
            return result
        }
    }

    protected fun addASTNodeToTerminal(node: DerivationNode, index: Int,
                                       accumulatedParameters: AccumulationParameters, endIndex: Int) {
        val terminalNode = ASTNode(originalText = text)
        terminalNode.type = node.step
        terminalNode.endIndex = endIndex
        terminalNode.startIndex = index
        accumulatedParameters.astNodes.add(terminalNode)
    }


    fun setFoundResult(index: Int, accumulatedParameters: AccumulationParameters, result: FoundIndex,
                       ignoreActions: Boolean, start: DerivationNode, startIndex: Int,
                       stack: StepStack) {
        result.astNode = ASTNode(originalText = text)
        result.astNode!!.startIndex = startIndex
        result.astNode!!.endIndex = index
        result.astNode!!.type = start.step
        result.astNode!!.children.addAll(accumulatedParameters.astNodes)
        result.found = true
        result.index = index
        result.fireParameters.addAll(accumulatedParameters.branchedFireParameters)
        result.matchTokens.addAll(accumulatedParameters.branchedMatchTokens)
        result.fullPath.addAll(accumulatedParameters.branchedFullPath)
        addAction(index, result, ignoreActions, start, startIndex, stack, result.astNode)
    }

    private fun addAction(index: Int, result: FoundIndex, ignoreActions: Boolean, start: DerivationNode, startIndex: Int,
                          stack: StepStack, astNode: ASTNode?) {
        if (!ignoreActions && startIndex != index && grammar.productions[start.step]!!.action != null) {
            //somehow the astNode added to the firedParameter gets disconnected from the rest of the AST in some cases. Moving the logic so that we set the nodes as a post process
            //my theory is that it matches the same rule from two different branches, one that failed and the second that succeeds. But since we cache the results we are stuck with
            //the fire AST node from the first failed branch which gets disconnected from the rest of the code
            result.fireParameters.add(FireParameters(start, startIndex, index, stack, result.matchTokens))
        }
    }

    protected open fun fireSuccessAction(p: FireParameters) {
        //getText subList that pertains to this production match

        val subString = text.substring(p.startIndex, p.endIndex)
        val str = filterText(p, subString).toString().trim()
        val whiteSpaceOffset = getBeginningWhiteSpaceOffset(subString)
        grammar.productions[p.node.step]!!.action!!(MatchData(str, p.stack, p.matchTokens, p.startIndex,
                p.startIndex + whiteSpaceOffset, p.endIndex, p.astNode!!))
    }

    protected fun getBeginningWhiteSpaceOffset(subString: String): Int {
        val whiteSpaceBeginning = "^\\s*".toRegex().find(subString)
        var whiteSpaceOffset = 0
        if (whiteSpaceBeginning != null) {
            whiteSpaceOffset = whiteSpaceBeginning.range.end
        }
        return whiteSpaceOffset
    }

    protected fun filterText(p: FireParameters, subString: String): StringBuilder {
        var subStringCleaned = subString
        val str = StringBuilder()
        for (regexToken in p.matchTokens) {
            val m = ("(?s)^\\s*(?:" + regexToken.token!!.regexDefinition + ")").genericRegex().find(subStringCleaned)
            str.append(m!!.groupValues[regexToken.token!!.matchFilter])
            subStringCleaned = subStringCleaned.substring(m.range.end)
        }
        return str
    }

    fun find(graph: DerivationGraph, grammar: Grammar<*>, text: String, delimiterPattern: String?) {
        var index = 0
        while (index < text.length) {
            index = try {
                val result = match(graph, grammar, text, false, index)
                if (!result.found) {
                    incrementByDelimiterIndex(delimiterPattern, index, text)
                } else {
                    result.index
                }
            } catch (e: ParserException) {
                incrementByDelimiterIndex(delimiterPattern, index, text)
            }

        }
    }

    private fun incrementByDelimiterIndex(delimiterPattern: String?, startIndex: Int, text: String): Int {
        var index = startIndex
        if (delimiterPattern != null) {
            val sub = text.substring(index)
            val m = "(?s)(?:${delimiterPattern})".genericRegex().find(sub)
            if (m != null) {
                index += m.range.end
            } else {
                index++
            }
        } else {
            index++
        }
        return index
    }

    protected interface LoopTraverse {

        fun call(node: DerivationNode, tokenIndex: Int, accumulatedParameters: AccumulationParameters)
    }

}
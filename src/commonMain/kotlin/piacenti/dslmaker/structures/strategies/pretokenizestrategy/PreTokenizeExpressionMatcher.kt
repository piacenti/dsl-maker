package piacenti.dslmaker.structures.strategies.pretokenizestrategy

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.errors.ParserException
import piacenti.dslmaker.interfaces.MatchData
import piacenti.dslmaker.removeLast
import piacenti.dslmaker.structures.*
import piacenti.dslmaker.structures.derivationgraph.DerivationGraph
import piacenti.dslmaker.structures.derivationgraph.DerivationNode

/**
 * Created by Piacenti on 8/24/2016.
 */
class PreTokenizeExpressionMatcher : piacenti.dslmaker.ExpressionMatcher() {
    private var tokens: List<Token>? = null
    private var highestTokenIndex: Int = 0

    fun match(graph: DerivationGraph,
              grammar: Grammar<*>,
              text: String,
              tokens: List<Token>,
              matchAllTokens: Boolean): FoundIndex {
        this.tokens = tokens
        try {
            return super.match(graph, grammar, text, matchAllTokens,0)
        } catch (e: ParserException) {
            if (tokens.isNotEmpty()) {
                throw ParserException(
                        "highest token index: $highestTokenIndex, unmatched " + tokens[highestTokenIndex].toString() + "\n" + e.message)
            } else {
                throw ParserException(e.message!!)
            }
        }

    }

    override fun matchProduction(start: DerivationNode,
                                 startIndex: Int,
                                 stack: StepStack,
                                 ignoreActions: Boolean): FoundIndex {
        val key = StringBuilder().append(start.step.hashCode()).append(startIndex).toString()
        val foundIndex = matchCache[key]
        if (foundIndex != null) {
            return foundIndex
        } else {
            var stackAdd = false
            if (stack.getAsList().isEmpty() || stack.getAsList().last() !== start.step) {
                stack.add(start.step)
                stackAdd = true
            }
            //Depth first search approach
            val result = FoundIndex(startIndex, false)
            result.fullPath.add(start.step)
            result.astNode = ASTNode(originalText = text)
            val branchedAccumulation = AccumulationParameters()
            val action = object : LoopTraverse {
                override fun call(node: DerivationNode,
                                  tokenIndex: Int,
                                  accumulatedParameters: AccumulationParameters) {
                    var index = tokenIndex

                    //if node is another production then go to that tree to deal with it
                    val subGraphNode = graph.getSubGraphs()[node.step]
                    if (subGraphNode != null) {
                        val temp: FoundIndex = if (node.step === start.step) {
                            matchProduction(graph.getSubGraphs().getValue(node.step), index, stack, true)
                        } else {
                            matchProduction(graph.getSubGraphs().getValue(node.step), index, stack, false)
                        }

                        if (temp.found) {
                            //assign index when changed because in recursive calls you will never fall in the condition that has no children
                            index = temp.index
                            result.index = index
                            accumulatedParameters.branchedMatchTokens.addAll(temp.matchTokens)
                            accumulatedParameters.branchedFireParameters.addAll(temp.fireParameters)
                            accumulatedParameters.branchedFullPath.addAll(temp.fullPath)
                            accumulatedParameters.astNodes.add(temp.astNode!!)
                            temp.astNode!!.parent = result.astNode
                            //this is needed because if the last item is matched and there is a recursive grammar running it will keep matching the
                            //last item because it doesn't know that the current index is a matched index rather than an index yet to be matched
                            if (index == tokens!!.size - 1 && (node.children.isEmpty() || node.isLeaf)) {
                                setFoundResult(index,
                                        accumulatedParameters,
                                        result,
                                        ignoreActions,
                                        start,
                                        tokens!![startIndex].startIndex,
                                        stack)
                                return
                            }
                        } else {
                            return
                        }
                    } else {
                        //=============BASE FAIL CASES==========================
                        if (node.step.isProduction) {
                            throw ParserException("Unsupported Production: " + node.step)
                        }
                        if (index >= tokens!!.size) {
                            return
                        }
                        //step must be of same type as token step, if not, do not continue recursion for its children covers also empty token match
                        if (node.step !== tokens!![index].definition && node.step
                                        .regexDefinition != "") {
                            return
                        }
                        //=========================================================
                        //add AST node for terminal
                        addASTNodeToTerminal(node, tokens!![index].startIndex, accumulatedParameters,
                                tokens!![index].endIndex)
                        if (node.step.regexDefinition != "") {
                            accumulatedParameters.branchedMatchTokens
                                    .add(TokenMatch(tokens!![index].endIndex,
                                            tokens!![index].startIndex,
                                            tokens!![index].value, node.step))
                            if (index < tokens!!.size - 1) {
                                //this was needed for when there are empties matching after the last character, since the index at that point would be greater than the number of
                                //of tokens the code would just fail to match them, a better fix might be to skip all this portion if there is a empty match
                                index++
                            }
                        }

                    }
                    if (node.children.isEmpty()) {
                        setFoundResult(index,
                                accumulatedParameters,
                                result,
                                ignoreActions,
                                start,
                                tokens!![startIndex].startIndex,
                                stack)
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
                        setFoundResult(index,
                                accumulatedParameters,
                                result,
                                ignoreActions,
                                start,
                                tokens!![startIndex].startIndex,
                                stack)
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
                val endToken: Token
                if (tokens!!.size > result.index) {
                    endToken = if (result.index - 1 > -1) tokens!![result.index] else tokens!![0]
                    if (endToken.endIndex > highestSuccessfulIndex) {
                        expectedTokenNotMatched = HashSet()
                        highestSuccessfulIndex = endToken.endIndex
                        highestTokenIndex = result.index
                        possibleValidTokensIfContinuingParsing = HashSet()
                        lastSuccessStack = ArrayList()
                        val list = mutableListOf<ProductionStep>()
                        list.addAll(stack.getAsList())
                        lastSuccessStack.add(list)
                    } else if (result.index > startIndex && endToken.endIndex == highestSuccessfulIndex) {
                        val list = mutableListOf<ProductionStep>()
                        list.addAll(stack.getAsList())
                        lastSuccessStack.add(list)
                    }
                }
            } else if (startIndex < tokens!!.size && tokens!![startIndex].startIndex >= highestSuccessfulIndex) {
                expectedTokenNotMatched.add(start.step)
            } else {
                result.fullPath.removeLast()
            }
            if (startIndex < tokens!!.size && !result.found && tokens!![startIndex].startIndex > highestSuccessfulIndex) {
                possibleValidTokensIfContinuingParsing.add(start.step)
            }
            if (stackAdd) {
                stack.removeLast()
            }
            matchCache[key] = result
            return result
        }

    }


    override fun fireSuccessAction(p: FireParameters) {
        //getText subList that pertains to this production match

        val subString = text.substring(p.startIndex, tokens!![p.endIndex].endIndex)
        val whiteSpaceOffset = getBeginningWhiteSpaceOffset(subString)
        val str = filterText(p, subString)
        grammar.productions[p.node.step]!!.action!!(MatchData(str.toString().trim(), p.stack, p.matchTokens,
                p.startIndex,
                p.startIndex + whiteSpaceOffset, p.endIndex, p.astNode!!))

    }
}

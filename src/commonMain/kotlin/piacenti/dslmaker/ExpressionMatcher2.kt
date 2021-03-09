package piacenti.dslmaker

import piacenti.dslmaker.ExpressionMatcher2.MatchStatus.*
import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.errors.ParserException
import piacenti.dslmaker.structures.*
import piacenti.dslmaker.structures.strategies.pretokenizestrategy.Token
import piacenti.dslmaker.structures.strategies.pretokenizestrategy.Tokenizer

/**
 * @param <T>
 * @author Piacenti
</T> */
open class ExpressionMatcher2(var tokenize: Boolean = false) {
    var trackData: TrackData = TrackData()

    data class TrackData(
            var highestSuccessfulIndex: Int = 0,
            val fullPath: MutableList<Pair<ProductionStep, Boolean>> = mutableListOf(),
            val expectedTokenNotMatched: MutableList<ProductionStep> = mutableListOf(),
            val possibleValidTokensIfContinuingParsing: MutableList<ProductionStep> = mutableListOf(),
            val matchCache: MutableMap<String, FoundIndex> = mutableMapOf()
    )

    fun match(grammar: Grammar<*>, text: String, matchAllTokens: Boolean,
              startIndex: Int = 0): ASTNode {
        trackData = TrackData()
        val result: ASTNode
        val inputSequence = getInputSequence(text, grammar)
        try {
            result = findPath(startIndex, inputSequence, grammar, text)
            finalizeAST(result, text, inputSequence)
            if (result.children.isEmpty() || (nothingRemainingToBeParsed(text, result, inputSequence) && matchAllTokens)) {
                throw ParserException("")
            }
            trackData.expectedTokenNotMatched.clear()
            return result
        } catch (e: ParserException) {
            throw throwException(inputSequence, text, e)
        }
    }

    private fun nothingRemainingToBeParsed(text: String, result: ASTNode, inputSequence: Any) =
            ((!tokenize && text.substring(result.endIndex+1).isNotBlank()) || (tokenize && trackData.highestSuccessfulIndex < (inputSequence as List<String>).lastIndex))


    private fun throwException(inputSequence: Any, text: String, e: ParserException?): ParserException {
        val subList: String
        val index: Int = if (tokenize) (inputSequence as List<Token>)[trackData.highestSuccessfulIndex].endIndex else trackData.highestSuccessfulIndex
        subList = if (index < text.length) {
            text.substring(index, text.length)
        } else {
            ""
        }
        val fullPath = trackData.fullPath.map {
            if (!it.first.isProduction)
                "${it.first} " + 27.toChar() + "[32mmatched: ${it.second}" + 27.toChar() + "[31m"
            else
                it.first.toString()
        }
        return ParserException(
                (if (e != null) e.message + "\n" else "") + "Not all tokens were matched, highest index reached " + index + "\nhighest success result index: " + index +
                        "\nexecution path: " + fullPath + "" + "\nexpected: " + trackData.expectedTokenNotMatched + "" + "\nremaining text: " + subList)
    }

    enum class MatchStatus {
        NOT_EVALUATED, FAILED, MATCHED
    }

    class Branch(stepSequence: List<ProductionStep>) {
        var matched: MatchStatus = NOT_EVALUATED
        val activeStep: LinkedNodes<ProductionStep> = LinkedNodes(stepSequence)
    }

    class ProductionEntry(val step: ProductionStep,
                          branches: List<Branch>,
                          startIndex: Int,
                          originalText: String) {
        val activeBranch: LinkedNodes<Branch> = LinkedNodes(branches)
        var astNode = ASTNode(step, startIndex = startIndex, endIndex = startIndex, originalText = originalText)
    }

    data class CacheKey(val stepName: String, val startIndex: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as CacheKey

            if (stepName != other.stepName) return false
            if (startIndex != other.startIndex) return false

            return true
        }

        override fun hashCode(): Int {
            var result = stepName.hashCode()
            result = 92821 * result + startIndex
            return result
        }
    }

    data class CacheValue(val astNode: ASTNode, val endIndex: Int, val matched: Boolean, val evalTextIsBlank: Boolean? = null)

    /**
     * This method finds the path through the graph that matches the string.
     */
    protected open fun findPath(startIndex: Int, inputSequence: Any, grammar: Grammar<*>, originalText: String): ASTNode {
        val startTime = getCurrentTimeInMilliSeconds()
        //for very long inputs constant resizing of hashmap can be expensive
        //hashmaps resize at around 75% capacity so here we are making sure we don't see any resize
        val matchCache: MutableMap<CacheKey, CacheValue> = LinkedHashMap((originalText.length * 1.4).toInt())
        val start = grammar.productions[grammar.startProduction]
                ?: throw ParserException("Production not defined for " + grammar.startProduction.name)
        val productionStack: MutableList<ProductionEntry> = mutableListOf()
        val startEntry = ProductionEntry(grammar.startProduction, start.expressions.map { Branch(it.steps) }, startIndex, originalText)
        productionStack.add(startEntry)
        while (productionStack.isNotEmpty()) {
            val currentEntry = productionStack.peek()
            if (currentEntry != null) {
                val currentIndex = currentEntry.astNode.endIndex
                val activeBranchNode = currentEntry.activeBranch
                var branch = activeBranchNode.node?.value
                if (branch != null) {
                    if (branch.matched == MATCHED) {
                        matchProduction(matchCache, currentEntry, productionStack)
                    } else {
                        if (branch.matched == FAILED) {
                            branch = moveToNextBranch(activeBranchNode, currentEntry)
                        }
                        if (branch == null) {
                            failProduction(matchCache, currentEntry, productionStack)
                        } else {
                            processStep(branch, grammar, matchCache, currentIndex, currentEntry, productionStack, inputSequence)
                        }
                    }
                }
            }
        }
        LOG.debug("path finding done in " + (getCurrentTimeInMilliSeconds() - startTime).toString() + "ms")
        return startEntry.astNode
    }

    protected open fun getInputSequence(text: String, grammar: Grammar<*>): Any {
        return if (tokenize) {
            Tokenizer(grammar).tokenize(text)
        } else {
            text
        }
    }

    private fun processStep(branch: Branch, grammar: Grammar<*>, matchCache: MutableMap<CacheKey, CacheValue>, currentIndex: Int, currentEntry: ProductionEntry, productionStack: MutableList<ProductionEntry>, inputSequence: Any) {
        val stepNode = branch.activeStep.node
        val step = stepNode?.value
        if (step == null) {
            branch.matched = MATCHED
        } else {
            if (step.isProduction) {
                handleProductionStep(grammar, step, matchCache, currentIndex, currentEntry, productionStack)
            } else {
                handleTerminal(inputSequence, currentIndex, step, productionStack, branch, matchCache)
            }
        }
    }

    private fun finalizeAST(astNode: ASTNode, text: String, inputSequence: Any) {
        astNode.forEachRecursive({ node ->
            if (tokenize) {
                inputSequence as List<Token>
                if (node.endIndex > node.startIndex) {
                    node.startIndex = inputSequence[node.startIndex].startIndex
                    node.endIndex = inputSequence[node.endIndex - 1].endIndex
                } else {
                    val tokenStartIndex = node.startIndex
                    node.startIndex = inputSequence[tokenStartIndex].startIndex
                    node.endIndex = inputSequence[tokenStartIndex].startIndex
                }
            }
            else
            node.endIndex = node.endIndex - 1
            node.children.forEach {
                it.parent = node
                //regex endIndex is one over real index. It works for substringing but
                // causes problems for other things like styling and text modifications
            }
        })
    }

    private fun handleTerminal(inputSequence: Any, currentIndex: Int, step: ProductionStep, productionStack: MutableList<ProductionEntry>, branch: Branch, matchCache: MutableMap<CacheKey, CacheValue>) {
        val resultCache = matchCache[CacheKey(step.name, currentIndex)]
        var matchEndIndexOffset = currentIndex
        var evalTextIsBlank = if (resultCache?.evalTextIsBlank != null)
            resultCache.evalTextIsBlank
        else
            false
        val matched = if (resultCache != null) {
            resultCache.matched
        } else {
            when (inputSequence) {
                is String -> {
                    val m = Tokenizer.matchTokenRegex(step, inputSequence, currentIndex)
                    evalTextIsBlank = remainderIsBlank(inputSequence, currentIndex)
                    matchEndIndexOffset = (m?.groups?.first()?.endIndex ?: 0)
                    m != null
                }
                is List<*> -> {
                    inputSequence as List<Token>
                    evalTextIsBlank = inputSequence.lastIndex <= currentIndex
                    if (step.regexDefinition.isNotBlank())
                        matchEndIndexOffset = currentIndex + 1
                    //fail matches past last index
                    if (currentIndex > inputSequence.lastIndex)
                        false
                    else
                        inputSequence[currentIndex].definition == step || step.regexDefinition.isBlank()
                }
                else -> {
                    error("Bad input sequence")
                }
            }
        }
        if (matched) {
            val endIndex = resultCache?.endIndex ?: matchEndIndexOffset
            handleTerminalMatch(productionStack, endIndex, step, currentIndex, evalTextIsBlank, branch, matchCache)

        } else {
            failBranch(step, evalTextIsBlank, productionStack, currentIndex, matchCache)
        }
    }

    private fun remainderIsBlank(inputSequence: String, currentIndex: Int): Boolean {
        for (x in currentIndex until inputSequence.length) {
            if (!inputSequence[x].isWhitespace())
                return false
        }
        return true
    }

    private fun handleTerminalMatch(productionStack: MutableList<ProductionEntry>, endIndex: Int, step: ProductionStep, currentIndex: Int, evalTextIsBlank: Boolean, branch: Branch, matchCache: MutableMap<CacheKey, CacheValue>) {
        val astNode = ASTNode(step, startIndex = currentIndex, endIndex = endIndex, originalText = productionStack.peek()?.astNode?.originalText
                ?: "")
        productionStack.peek()?.let {
            it.astNode.endIndex = endIndex
            it.astNode.children.add(astNode)
        }
        if (endIndex > trackData.highestSuccessfulIndex) {
            trackData.expectedTokenNotMatched.clear()
            trackData.highestSuccessfulIndex = endIndex
            if (!evalTextIsBlank)
                trackData.possibleValidTokensIfContinuingParsing.clear()
        }
        matchCache[CacheKey(step.name, currentIndex)] = CacheValue(astNode, endIndex, true, evalTextIsBlank)
        //advance to next step in branch
        branch.activeStep.next()
    }

    private fun failBranch(step: ProductionStep, evalTextIsBlank: Boolean, productionStack: MutableList<ProductionEntry>, currentIndex: Int, matchCache: MutableMap<CacheKey, CacheValue>) {
        val parentsStartingWithSameIndex = getParentsStartingWithIndex(currentIndex, productionStack)
        if (currentIndex == trackData.highestSuccessfulIndex) {
            trackData.expectedTokenNotMatched.add(step)
            parentsStartingWithSameIndex.forEach {
                trackData.expectedTokenNotMatched.add(it)
            }
            if (evalTextIsBlank) {
                trackData.possibleValidTokensIfContinuingParsing.add(step)
                parentsStartingWithSameIndex.forEach {
                    trackData.possibleValidTokensIfContinuingParsing.add(it)
                }
            }
        }
        matchCache[CacheKey(step.name, currentIndex)] = CacheValue(ASTNode(originalText = productionStack.peek()?.astNode?.originalText
                ?: ""), currentIndex, false, evalTextIsBlank)
        productionStack.peek()?.let {
            //reset matching
            it.astNode.endIndex = it.astNode.startIndex
            it.activeBranch.node?.value?.matched = FAILED
            it.astNode.children.clear()
        }
    }

    private fun getParentsStartingWithIndex(currentIndex: Int, productionStack: MutableList<ProductionEntry>): List<ProductionStep> {
        val result = mutableListOf<ProductionStep>()
        for (index in (productionStack.size - 1) downTo 0) {
            val productionEntry = productionStack[index]
            if (productionEntry.astNode.startIndex == currentIndex)
                result.add(productionEntry.step)
            else
                break
        }
        return result
    }

    private fun handleProductionStep(grammar: Grammar<*>, step: ProductionStep, matchCache: MutableMap<CacheKey, CacheValue>, currentIndex: Int, currentEntry: ProductionEntry, productionStack: MutableList<ProductionEntry>) {
        val production = grammar.productions[step]
        if (production != null) {
            val cachedResult = matchCache[CacheKey(step.name, currentIndex)]
            if (cachedResult != null) {
                if (cachedResult.matched) {
                    currentEntry.astNode.children.add(cachedResult.astNode)
                    currentEntry.astNode.endIndex = cachedResult.astNode.endIndex
                    currentEntry.activeBranch.node?.value?.activeStep?.next()
                } else {
                    productionStack.peek()?.let {
                        //reset matching
                        it.astNode.endIndex = it.astNode.startIndex
                        it.activeBranch.node?.value?.matched = FAILED
                        it.astNode.children.clear()
                    }
                }
            } else
                productionStack.add(ProductionEntry(step, production.expressions.map { Branch(it.steps) }, currentIndex, productionStack.peek()?.astNode?.originalText
                        ?: ""))
        }
    }

    private fun matchProduction(matchCache: MutableMap<CacheKey, CacheValue>, currentEntry: ProductionEntry, productionStack: MutableList<ProductionEntry>) {
        matchCache[CacheKey(currentEntry.step.name, currentEntry.astNode.startIndex)] = CacheValue(currentEntry.astNode, currentEntry.astNode.endIndex, true)
        productionStack.pop()
        productionStack.peek()?.let { parent ->
            parent.astNode.children.add(currentEntry.astNode)
            parent.astNode.endIndex = currentEntry.astNode.endIndex
            parent.activeBranch.node?.value?.activeStep?.next()
        }
    }

    private fun moveToNextBranch(activeBranchNode: LinkedNodes<Branch>, currentEntry: ProductionEntry): Branch? {
        var branch: Branch? = activeBranchNode.next()?.value
        currentEntry.astNode.endIndex = currentEntry.astNode.startIndex
        currentEntry.astNode.children.clear()
        return branch
    }

    private fun failProduction(matchCache: MutableMap<CacheKey, CacheValue>, currentEntry: ProductionEntry, productionStack: MutableList<ProductionEntry>) {
        matchCache[CacheKey(currentEntry.step.name, currentEntry.astNode.startIndex)] = CacheValue(currentEntry.astNode, currentEntry.astNode.endIndex, false)
        //pop failed production
        productionStack.pop()
        productionStack.peek()?.let {
            it.activeBranch.node?.value?.matched = FAILED
            it.astNode.endIndex = it.astNode.startIndex
            it.astNode.children.clear()
        }
    }

    fun find(grammar: Grammar<*>, text: String, delimiterPattern: String?): List<ASTNode> {
        val resultAsts = mutableListOf<ASTNode>()
        var index = 0
        while (index < text.lastIndex) {
            index = try {
                val result = match(grammar, text, false, index)
                resultAsts.add(result)
                result.endIndex
            } catch (e: ParserException) {
                incrementByDelimiterIndex(delimiterPattern, index, text)
            }
        }
        return resultAsts
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

}

private fun <E> List<E>.peek(): E? {
    return lastOrNull()
}

package piacenti.dslmaker.structures.derivationgraph

import piacenti.dslmaker.abstraction.ProductionStep
import piacenti.dslmaker.errors.BadGrammar
import piacenti.dslmaker.interfaces.GrammarInventory
import piacenti.dslmaker.structures.Expression
import piacenti.dslmaker.structures.Grammar
import piacenti.dslmaker.structures.Production

/**
 * Created by Piacenti on 3/23/2017.
 */
internal class LeftRecursionRemover {
    private val erroCausers = ArrayList<Exception>()


    fun <T : GrammarInventory> removeLeftRecursion(grammar: Grammar<T>): Grammar<T> {
        erroCausers.clear()
        //first we remove direct left recursion so that when we remove indirect recursion we don't introduce more indirect recursions and the we finally remove direct left recursion again
        removeSameProductionLeftRecursion(grammar)
        removeIndirectLeftRecursion(grammar)
        removeSameProductionLeftRecursion(grammar)
        if (!erroCausers.isEmpty()) {
            for (error in erroCausers) {
                throw error
            }
        }
        return grammar
    }

    //technique to remove left side recursion on same production is the following
    //E-> EOE|(E)|N
    //becomes
    //E->(E)E`|NE`
    //E`->OEE`|empty

    private fun removeSameProductionLeftRecursion(grammar: Grammar<*>) {
        //get actual class that will be generated
        val newAndReplacementProductions = HashMap<ProductionStep, Production>()
        val emptyValueStep = ProductionStep("", "")
        grammar.productions.entries.forEach { entry ->
            //make sure that for each production there is no expression that starts with same Symbol as the rule
            if (entry.value.expressions.filter { expression ->
                        expression.steps[0] === entry.key
                    }.count() > 0) {
                //create a new production using class
                try {
                    val newProductionExpressions = ArrayList<Expression>()
                    val leftRecursionRemovingProductionType = ProductionStep(entry.key.toString() + "'")
                    //get recursive expressions and move them to the new production removing the original left recursion and adding a
                    // right recursion to the new expression relative to the new production
                    entry.value.expressions.filter { expression ->
                        expression.steps[0] === entry.key
                    }.forEach { expression ->
                        val newEx = Expression(expression.steps)
                        //remove left recursive component
                        newEx.steps.remove(entry.key)
                        //add right recursion to new production
                        newEx.steps.add(leftRecursionRemovingProductionType)
                        newProductionExpressions.add(newEx)
                    }
                    //add empty value
                    newProductionExpressions.add(Expression(emptyValueStep))
                    //add new production to maps of productions to be added
                    newAndReplacementProductions[leftRecursionRemovingProductionType] = Production(null,
                            newProductionExpressions)
                    //change the source production by removing left recursive expressions
                    entry.value.expressions.removeAll(entry.value.expressions.filter { expression ->
                        expression.steps[0] === entry.key
                    })
                    //add new production to end of remaining source productions
                    entry.value.expressions.forEach { expression ->
                        expression.steps.add(leftRecursionRemovingProductionType)
                    }

                } catch (e: Exception) {
                    println(e.message)
                }

            }
        }
        //replace and add productions to grammar
        grammar.productions.putAll(newAndReplacementProductions)
    }

    /*
    technique to remove indirect left side recursion  is the following
    S	->	Aa  |  b
    A	->	Ac  |  Sd  |  empty
    becomes
    S	->	Aa  |  b
    A	->	Ac  |  Aad  |  bd  |  empty
    so we basically identify that there is a indirect left recursion for S and wherever we find S as first item in Expression that is not part of production S
    we replace it by S contents
    Once this is done for all productions we should only have same production left recursion which is handled by the same production left recursion removal method
    * */
    private fun findNextIndirectRecursion(grammar: Grammar<*>): Pair<ProductionStep, Set<ProductionStep>>? {
        /*the algorithm has a chance depending on ordering of processing to make certain productions unreachable even though
        the grammar will match in the end. For the example above if the A was the starting production then S would become unreachable
        although in the end there would still be a match, the ast will be messed up

        To handle that situation we process the recursions that have highest number of usages first
        */
        val indirectRecursionProductions = mutableListOf<Pair<ProductionStep, Set<ProductionStep>>>()
        grammar.productions.forEach {
            val key = it.key
            val productionsWithKeyAsStartingExp = grammar.productions.filter { entry ->
                entry.key != key && entry.value.expressions.filter { expression -> expression.steps[0] === key }.count() > 0
            }
            //only keep the ones pointed to from the current
            val recursiveForKey = grammar.productions[key]!!.expressions.map { expression -> expression.steps[0] }.intersect(
                    productionsWithKeyAsStartingExp.map { entry -> entry.key })
            if (recursiveForKey.isNotEmpty()) {
                indirectRecursionProductions.add(key to recursiveForKey)
            }
        }
        return indirectRecursionProductions.maxBy { pair ->
            countNumberOfExpressionsUsingIt(grammar, pair.first)
        }
    }

    private fun countNumberOfExpressionsUsingIt(grammar: Grammar<*>, step: ProductionStep): Int {
        return grammar.productions.map {
            it.value.expressions.filter { exp ->
                exp.steps.contains(step)
            }.count()
        }.sum()
    }

    private fun removeIndirectLeftRecursion(grammar: Grammar<*>) {
        var indirectRecursions = findNextIndirectRecursion(grammar)
        while (indirectRecursions != null) {
            val pair = indirectRecursions
            //find its production expressions
            val sourceProduction = pair.first
            val replacementExpressions = grammar.productions[sourceProduction]!!.expressions
            //replace it in each production using it as first step in an expression
            pair.second.forEach { productionStep ->
                val newExpressions = mutableListOf<Expression>()
                val production = grammar.productions[productionStep]!!
                production.expressions.forEach { expression ->
                    if (expression.steps.first() === sourceProduction) {
                        if (expression.steps.size == 1) {
                            erroCausers.add(BadGrammar(
                                    "Grammar contains indirect cycles that cannot be removed, causing rules: ${sourceProduction.name} and ${productionStep.name}"))
                        } else {
                            val remainingOriginal = expression.steps.subList(1, expression.steps.size)
                            //combine remaining original (without the starting recursion) with each of the replacements
                            replacementExpressions.forEach { replExpression ->
                                newExpressions.add(Expression(replExpression.steps + remainingOriginal))
                            }
                        }
                    } else {
                        //just add it as it is
                        newExpressions.add(expression)
                    }
                }
                production.expressions.clear()
                production.expressions.addAll(newExpressions)
            }
            indirectRecursions = findNextIndirectRecursion(grammar)

        }
    }


}

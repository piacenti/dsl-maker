package piacenti.dslmaker.dsl.antlr

import org.antlr.v4.kotlinruntime.tree.ParseTree

interface StreamReWriter {
    val rewriter: FastTokenStreamRewriter
    fun ParseTree.rewriterText(): String {
        return rewriter.generateText(this,null)
    }
}
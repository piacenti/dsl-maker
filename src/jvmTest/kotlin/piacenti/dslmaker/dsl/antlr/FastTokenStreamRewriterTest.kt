package piacenti.dslmaker.dsl.antlr

import kotlin.test.Test
import org.antlr.v4.kotlinruntime.ANTLRInputStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import piacenti.dslmaker.antlr.generated.RewriterTestLexer
import piacenti.dslmaker.dsl.InsertionType
import piacenti.dslmaker.validateEquals

internal class FastTokenStreamRewriterTest(){
    @Test
    fun should_allow_to_delete(){
        val tokenStream= CommonTokenStream(RewriterTestLexer(ANTLRInputStream("Hello World guys !")))
        tokenStream.fill()
        var fastTokenStreamRewriter = FastTokenStreamRewriter(tokenStream)
        fastTokenStreamRewriter.delete(tokenStream[1])
        fastTokenStreamRewriter.text validateEquals "Helloguys!"
        fastTokenStreamRewriter = FastTokenStreamRewriter(tokenStream)
        fastTokenStreamRewriter.delete(tokenStream[1] ,tokenStream[2])
        fastTokenStreamRewriter.text validateEquals "Hello!"
    }
    @Test
    fun should_allow_to_add_after_deletion(){
        val tokenStream= CommonTokenStream(RewriterTestLexer(ANTLRInputStream("Hello World guys !")))
        tokenStream.fill()
        var fastTokenStreamRewriter = FastTokenStreamRewriter(tokenStream)
        fastTokenStreamRewriter.delete(tokenStream[1])
        fastTokenStreamRewriter.insertAfter(tokenStream[1],"\n",InsertionType.FORMAT)
        fastTokenStreamRewriter.text validateEquals """Hello
guys!"""
        fastTokenStreamRewriter = FastTokenStreamRewriter(tokenStream)
        fastTokenStreamRewriter.delete(tokenStream[1],tokenStream[2])
        fastTokenStreamRewriter.insertAfter(tokenStream[2],"\n",InsertionType.FORMAT)
        fastTokenStreamRewriter.text validateEquals """Hello
!"""
    }
    @Test
    fun should_allow_to_add_after_replacement(){
        val tokenStream= CommonTokenStream(RewriterTestLexer(ANTLRInputStream("Hello World guys !")))
        tokenStream.fill()
        var fastTokenStreamRewriter = FastTokenStreamRewriter(tokenStream)
        fastTokenStreamRewriter.replace(tokenStream[1],"@")
        fastTokenStreamRewriter.insertAfter(tokenStream[1],"\n",InsertionType.FORMAT)
        fastTokenStreamRewriter.text validateEquals """Hello@
            |guys!""".trimMargin()
        fastTokenStreamRewriter = FastTokenStreamRewriter(tokenStream)
        fastTokenStreamRewriter.replace(tokenStream[1],tokenStream[2],"@")
        fastTokenStreamRewriter.insertAfter(tokenStream[2],"\n",InsertionType.FORMAT)
        fastTokenStreamRewriter.text validateEquals """Hello@
            |!""".trimMargin()
    }
    @Test
    fun should_use_over_arching_replace_if_present(){
        val tokenStream= CommonTokenStream(RewriterTestLexer(ANTLRInputStream("Hello World guys !")))
        tokenStream.fill()
        var fastTokenStreamRewriter = FastTokenStreamRewriter(tokenStream)
        fastTokenStreamRewriter.replace(tokenStream[1],"@")
        fastTokenStreamRewriter.replace(tokenStream[1],tokenStream[2],"##")
        fastTokenStreamRewriter.text validateEquals "Hello##!"
    }
}
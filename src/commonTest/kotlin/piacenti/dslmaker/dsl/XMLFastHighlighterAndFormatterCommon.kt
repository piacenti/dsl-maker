package piacenti.dslmaker.dsl

import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker
import piacenti.dslmaker.antlr.generated.XMLLexer
import piacenti.dslmaker.antlr.generated.XMLParser
import piacenti.dslmaker.antlr.generated.XMLParserBaseListener
import piacenti.dslmaker.dsl.antlr.*
import piacenti.dslmaker.parentHierarchy
import piacenti.dslmaker.times


class XMLFastHighlighterAndFormatterCommon(override var exceptionOnFailure: Boolean = false) : AntlrFormatterAndHighlighterBase() {
    class XMLHighlightAndFormatListener(override val tokenStream: CommonTokenStream, override val highlight: Boolean = true, override val format: Boolean = true, override val tabType: String = "    ") : XMLParserBaseListener(), AntlrFormatterAndHighlighterListener {
        override val rewriter = FastTokenStreamRewriter(tokenStream)
        private val defaultElementAttributes = listOf("data-toggle" to "\"tooltip\"",
                "data-delay" to """'{"show":1000,"hide":0}'""",
                "data-placement" to "\"bottom\"")

        companion object {
            private const val GOLD = "gold"
            private const val PURPLE = "purple"
            private const val LIGHT_BLUE = "light-blue"
            private const val GREEN = "green"
        }


        override fun exitCommonElementStart(ctx: XMLParser.CommonElementStartContext) {
            if (highlight) {
                val popoverText = ctx.parentHierarchy().mapNotNull {
                    it as? XMLParser.ElementContext
                }.reversed().joinToString(separator = "/") {
                    it.findElementStart()?.findCommonElementStart()?.Name()?.text ?: ""
                }
                highlightToken(ctx.OPEN()?.symbol, GOLD)
                highlightToken(ctx.Name()?.symbol, LIGHT_BLUE, defaultElementAttributes + listOf("title" to "\"$popoverText\""))
                ctx.findAttribute().forEach {
                    highlightToken(it.Name()?.symbol, PURPLE)
                    highlightToken(it.STRING()?.symbol, GREEN)
                    highlightToken(it.EQUALS()?.symbol, GOLD)
                }
            }
        }


        override fun exitProlog(ctx: XMLParser.PrologContext) {
            if (format) {
                rewriter.insertAfter(ctx.stop!!, "\n", InsertionType.FORMAT)
            }
            if (highlight) {
                highlightToken(ctx.XMLDeclOpen()?.symbol, GOLD)
                ctx.findAttribute().forEach {
                    highlightToken(it.Name()?.symbol, PURPLE)
                    highlightToken(it.STRING()?.symbol, GREEN)
                    highlightToken(it.EQUALS()?.symbol, GOLD)
                }
                highlightToken(ctx.SPECIAL_CLOSE()?.symbol, GOLD)
            }
        }


        override fun exitElement(ctx: XMLParser.ElementContext) {
            if (format) {
                val tabLevel = tabType * ctx.parentHierarchy().filterIsInstance<XMLParser.ElementContext>().size
                if (ctx.findContent()?.findElement()?.isNotEmpty() == true) {
                    ctx.findElementStart()?.stop?.let {
                        rewriter.insertAfter(it, "\n", InsertionType.FORMAT)
                    }
                    ctx.findElementEnd()?.let {
                        rewriter.insertBefore(it.start!!, tabLevel, InsertionType.FORMAT)
                    }
                }
                rewriter.insertAfter(ctx.stop!!, "\n", InsertionType.FORMAT)
                rewriter.insertBefore(ctx.start!!, tabLevel, InsertionType.FORMAT)
            }
        }


        override fun exitElementStart(ctx: XMLParser.ElementStartContext) {
            if (highlight)
                highlightToken(ctx.CLOSE()?.symbol, GOLD)
        }

        override fun exitEmptyElement(ctx: XMLParser.EmptyElementContext) {
            if (highlight)
                highlightToken(ctx.SLASH_CLOSE()?.symbol, GOLD)

        }

        override fun exitElementEnd(ctx: XMLParser.ElementEndContext) {
            if (highlight) {
                highlightTokenRange(ctx.OPEN()?.symbol to ctx.SLASH()?.symbol, GOLD)
                highlightToken(ctx.Name()?.symbol, LIGHT_BLUE)
                highlightToken(ctx.CLOSE()?.symbol, GOLD)
            }
        }

        override fun exitChardata(ctx: XMLParser.ChardataContext) {
            if (format && ctx.text.isBlank()) {
                rewriter.delete(ctx.start!!, ctx.stop!!)
            }
        }


    }

    override fun highlightAndFormat(text: String, format: Boolean, highlight: Boolean): XMLHighlightAndFormatListener {
        val lexer = XMLLexer(ANTLRInputStream(text))
        val captureErrorListener = CaptureErrorListener()
        if (exceptionOnFailure) {
            lexer.addErrorListener(captureErrorListener)
        }
        val tokenStream = CommonTokenStream(lexer)
        val parser = XMLParser(tokenStream)
        if (exceptionOnFailure) {
            parser.errorHandler = BailoutErrorStrategy()
            parser.addErrorListener(ConsoleErrorListener())
        }
        val tree = parser.document()
        if (exceptionOnFailure) {
            if (captureErrorListener.errors.isNotEmpty()) {
                error(captureErrorListener.errors.joinToString("\n"))
            }
        }
        val formatterAndHighlighterListener = XMLHighlightAndFormatListener(tokenStream, highlight = highlight, format = format)
        ParseTreeWalker().walk(formatterAndHighlighterListener, tree)
        return formatterAndHighlighterListener
    }
}
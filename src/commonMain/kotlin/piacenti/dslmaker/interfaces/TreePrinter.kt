package piacenti.dslmaker.interfaces

import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.tree.ParseTree
import org.antlr.v4.kotlinruntime.tree.TerminalNode
import piacenti.dslmaker.previewCharacters
import kotlin.js.JsName

interface TreePrinter {
    @Suppress("unused")
    fun printTree(maxStartAndEndCharacters: Int = 5) {
        println(treeToString(maxStartAndEndCharacters))
    }

    @Suppress("MemberVisibilityCanBePrivate")
    @JsName("treeToString")
    fun treeToString(maxStartAndEndCharacters: Int = 5): String {
        val buffer = StringBuilder()
        treeToString(buffer, "", "", maxStartAndEndCharacters)
        return buffer.toString().trim()
    }

    val children: List<TreePrinter>
    fun treeNodeText(): String
    fun treeNodeIdentifierString(): String?
    private fun treeToString(buffer: StringBuilder, prefix: String, childrenPrefix: String, maxStartAndEndCharacters: Int) {
        buffer.append(prefix)
        val string = treeNodeText().previewCharacters(maxStartAndEndCharacters).replace("\n", "\\n")
        val nodeIdentifier = treeNodeIdentifierString()
        if (nodeIdentifier?.isNotBlank() == true)
            buffer.append("$nodeIdentifier: $string")
        else
            buffer.append(string)
        buffer.append('\n')
        children.forEachIndexed { index, node ->
            if (index < children.lastIndex) {
                node.treeToString(buffer, "$childrenPrefix├── ", "$childrenPrefix│   ", maxStartAndEndCharacters)
            } else {
                node.treeToString(buffer, "$childrenPrefix└── ", "$childrenPrefix    ", maxStartAndEndCharacters)
            }
        }
    }
}

enum class NodeBorder {
    OVAL, RECTANGLE
}

interface GraphPrinter : TreePrinter {
    val parents: List<GraphPrinter>
    override val children: List<GraphPrinter>
    fun dotNodeBorder(): NodeBorder {
        return NodeBorder.OVAL
    }

    fun graphNodeText(): String
    fun toDOTGraph(): String {
        val result = StringBuilder()
        result.append("""
digraph G{ 
    concentrate=true;
""".trimStart())
        when (dotNodeBorder()) {
            NodeBorder.OVAL -> result.append("node [ shape = oval ];\n")
            NodeBorder.RECTANGLE -> result.append("node [ shape = rect ];\n")
        }
        val allNodes = allNodes()
        val edges = mutableListOf<String>()
        allNodes.forEach { node ->
            edges.add("\"${node.hashCode()}\" [label=\"${node.graphNodeText()}\"];")
        }
        allNodes.forEach { node ->
            node.parents.forEach {
                edges.add("\"${node.hashCode()}\"->\"${it.hashCode()}\";")
            }
            node.children.forEach { child ->
                edges.add("\"${node.hashCode()}\"->\"${child.hashCode()}\";")
            }
        }
        result.append(edges.toSet().joinToString(separator = "\n") { "    $it" })
        result.append("\n}")
        return result.toString()
    }

    fun allNodes(): List<GraphPrinter> {
        val stack = mutableListOf<GraphPrinter>()
        val visitedNodes = mutableListOf<GraphPrinter>()
        stack.add(this)
        while (stack.isNotEmpty()) {
            val nextLevel = mutableListOf<GraphPrinter>()
            while (stack.isNotEmpty()) {
                val pop = stack.removeAt(0)
                visitedNodes.add(pop)
                pop.children.forEach { child ->
                    if (!visitedNodes.containsIdentity(child)) {
                        nextLevel.add(child)
                    }
                }
            }
            stack.addAll(nextLevel)
        }
        return visitedNodes
    }
}

internal class ParserRuleContextGraphWrapper(private val parserRuleContext: ParseTree, private val parser: Parser) : GraphPrinter {
    override fun hashCode(): Int {
        return parserRuleContext.hashCode()
    }

    override val parents: List<GraphPrinter> by lazy {
        parserRuleContext.readParent()?.let {
            listOf(ParserRuleContextGraphWrapper(it, parser))
        } ?: emptyList()
    }

    override val children: List<GraphPrinter> by lazy {
        parserRuleContext.children.map {
            ParserRuleContextGraphWrapper(it, parser)
        }
    }


    override fun graphNodeText(): String {
        return treeNodeIdentifierString()!!
    }

    override fun treeNodeText(): String {
        return treeNodeIdentifierString()!!
    }

    override fun treeNodeIdentifierString(): String? {
        return if (parserRuleContext is TerminalNode) {
            val text = parserRuleContext.text
            if (text.isBlank()) {
                parser.vocabulary.getDisplayName(parserRuleContext.symbol.type)
            } else text.replace("\n", "\\n")
        } else {
            parserRuleContext as ParserRuleContext
            parser.ruleNames[parserRuleContext.ruleIndex]
        }
    }
}

val ParseTree.children: List<ParseTree>
    get() {
        val result = mutableListOf<ParseTree>()
        for (x in 0 until childCount) {
            result.add(this.getChild(x)!!)
        }
        return result
    }

private fun <E> List<E>.containsIdentity(child: E): Boolean {
    this.forEach {
        if (it === child) {
            return true
        }
    }
    return false
}

fun ParserRuleContext.toDOTGraph(parser: Parser): String {
    return ParserRuleContextGraphWrapper(this, parser).toDOTGraph()
}

fun ParserRuleContext.treeToString(parser: Parser, maxStartAndEndCharacters: Int = 5): String {
    val buffer = StringBuilder()
    treeToString(this, parser, buffer, "", "", maxStartAndEndCharacters)
    return buffer.toString().trim()
}

private fun treeToString(tree: ParseTree, parser: Parser, buffer: StringBuilder, prefix: String, childrenPrefix: String, maxStartAndEndCharacters: Int) {
    buffer.append(prefix)
    var string = tree.text.previewCharacters(maxStartAndEndCharacters).replace("\n", "\\n")
    if (tree is ParserRuleContext) {
        val nodeIdentifier = parser.ruleNames[tree.ruleIndex]
        if (nodeIdentifier.isNotBlank())
            buffer.append("$nodeIdentifier: $string")
        else
            buffer.append(string)
    } else
        buffer.append(string)
    buffer.append('\n')
    for (index in 0 until tree.childCount) {
        val node = tree.getChild(index)!!
        if (index < tree.childCount - 1) {
            treeToString(node, parser, buffer, "$childrenPrefix├── ", "$childrenPrefix│   ", maxStartAndEndCharacters)
        } else {
            treeToString(node, parser, buffer, "$childrenPrefix└── ", "$childrenPrefix    ", maxStartAndEndCharacters)
        }
    }
}

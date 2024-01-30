lexer grammar IndentBasedLexer;
tokens { INDENT, DEDENT }
@lexer::header {
  import org.antlr.v4.kotlinruntime.*
}
@lexer::members{
var dedentAtEnd=true
private var indentStack = mutableListOf<Int>()
private var extraTokens = mutableListOf<Token>()
private var lastToken: Token? = null
private var finished = false
private fun clear() {
    indentStack.clear()
    extraTokens.clear()
    lastToken = null
    finished = false
}

override fun nextToken(): Token {
    if (extraTokens.isEmpty() && finished) {
        clear()
    } else if (inputStream.LA(1) == EOF && !finished && dedentAtEnd) {
        repeat(indentStack.size) { _ ->
            extraTokens.add(CommonToken(lastToken!!).apply {
                type = Tokens.DEDENT
                channel = Token.DEFAULT_CHANNEL
            })
        }
        indentStack.clear()
        finished = true
    }
    if (extraTokens.isNotEmpty()) {
        return extraTokens.removeAt(0)
    }
    var nextToken = super.nextToken() as CommonToken
    lastToken = nextToken
    if (nextToken.type == Tokens.DATA_PATH) {
        val find = "\\s+as(\\s+\\w+)+?\\s*$".toRegex().find(nextToken.text!!)
        if (find != null) {
            val offset = nextToken.text!!.length - find.groupValues[0].length
            resetAcceptPosition(inputStream, _tokenStartCharIndex + offset, _tokenStartLine, _tokenStartCharPositionInLine + offset)
            emit()
            return token!!
        }
    }
    if (nextToken.type == Tokens.SPACES) {
        val numberOfSpaces = nextToken.text!!.split("\n").last().length
        if (numberOfSpaces > indentStack.lastOrNull() ?: 0) {
            nextToken.type = Tokens.INDENT
            nextToken.channel = Token.DEFAULT_CHANNEL
            indentStack.add(numberOfSpaces)
        } else if (numberOfSpaces < indentStack.lastOrNull() ?: 0) {
            nextToken.type = Tokens.DEDENT
            nextToken.channel = Token.DEFAULT_CHANNEL
            //from the end find the indentation that matches num spaces and dedent as many times as needed
            for (x in indentStack.lastIndex downTo 0) {
                if (indentStack[x] == numberOfSpaces) {
                    break
                } else {
                    indentStack.removeAt(x)
                    extraTokens.add(nextToken)
                }
            }
        }
    }
    if (extraTokens.isNotEmpty()) {
        return extraTokens.removeAt(0)
    }
    return nextToken
}

private fun resetAcceptPosition(input: CharStream, index: Int, line: Int, charPositionInLine: Int) {
    input.seek(index)
    this.line = line
    this.charPositionInLine = charPositionInLine
    interpreter?.consume(input)
}

}
PRINT:'print' ' '*;
NAME:'name' ' '*;
NEW_LINE:'\n'+ -> pushMode(INDENT_MODE);
ALIAS: ' '* 'as' ' '+ (IDENTIFIER ' '*)+ ;
fragment IDENTIFIER: [a-zA-Z]+;
//jsonpath or xpath parsing is done separately for highlighting
// here we just ignore it
DATA_PATH: ~[\n]+ ;
WS:[ \t]-> channel(HIDDEN);
mode INDENT_MODE;
SPACES:[ \t\n]*  -> popMode, channel(HIDDEN);


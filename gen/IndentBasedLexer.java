// Generated from /Users/n0253249/Desktop/workspace/dsl-maker/src/commonAntlrTest/antlr/IndentBasedLexer.g4 by ANTLR 4.9.1

  import org.antlr.v4.kotlinruntime.*

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class IndentBasedLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		INDENT=1, DEDENT=2, PRINT=3, NAME=4, NEW_LINE=5, ALIAS=6, DATA_PATH=7, 
		WS=8, SPACES=9;
	public static final int
		INDENT_MODE=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "INDENT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"PRINT", "NAME", "NEW_LINE", "ALIAS", "IDENTIFIER", "DATA_PATH", "WS", 
			"SPACES"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "INDENT", "DEDENT", "PRINT", "NAME", "NEW_LINE", "ALIAS", "DATA_PATH", 
			"WS", "SPACES"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


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
	    } else if (input.LA(1) == EOF && !finished && dedentAtEnd) {
	        repeat(indentStack.size) { _ ->
	            extraTokens.add(CommonToken(lastToken!!).apply {
	                type = Tokens.DEDENT.id
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
	    if (nextToken.type == Tokens.DATA_PATH.id) {
	        val find = "\\s+as(\\s+\\w+)+?\\s*$".toRegex().find(nextToken.text!!)
	        if (find != null) {
	            val offset = nextToken.text!!.length - find.groupValues[0].length
	            resetAcceptPosition(input, _tokenStartCharIndex + offset, _tokenStartLine, _tokenStartCharPositionInLine + offset)
	            emit()
	            return token!!
	        }
	    }
	    if (nextToken.type == Tokens.SPACES.id) {
	        val numberOfSpaces = nextToken.text!!.split("\n").last().length
	        if (numberOfSpaces > indentStack.lastOrNull() ?: 0) {
	            nextToken.type = Tokens.INDENT.id
	            nextToken.channel = Token.DEFAULT_CHANNEL
	            indentStack.add(numberOfSpaces)
	        } else if (numberOfSpaces < indentStack.lastOrNull() ?: 0) {
	            nextToken.type = Tokens.DEDENT.id
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



	public IndentBasedLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "IndentBasedLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\13b\b\1\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\3\2\3\2\3\2"+
		"\3\2\3\2\3\2\3\2\7\2\34\n\2\f\2\16\2\37\13\2\3\3\3\3\3\3\3\3\3\3\3\3\7"+
		"\3\'\n\3\f\3\16\3*\13\3\3\4\6\4-\n\4\r\4\16\4.\3\4\3\4\3\5\7\5\64\n\5"+
		"\f\5\16\5\67\13\5\3\5\3\5\3\5\3\5\6\5=\n\5\r\5\16\5>\3\5\3\5\7\5C\n\5"+
		"\f\5\16\5F\13\5\6\5H\n\5\r\5\16\5I\3\6\6\6M\n\6\r\6\16\6N\3\7\6\7R\n\7"+
		"\r\7\16\7S\3\b\3\b\3\b\3\b\3\t\7\t[\n\t\f\t\16\t^\13\t\3\t\3\t\3\t\2\2"+
		"\n\4\5\6\6\b\7\n\b\f\2\16\t\20\n\22\13\4\2\3\6\4\2C\\c|\3\2\f\f\4\2\13"+
		"\13\"\"\4\2\13\f\"\"\2i\2\4\3\2\2\2\2\6\3\2\2\2\2\b\3\2\2\2\2\n\3\2\2"+
		"\2\2\16\3\2\2\2\2\20\3\2\2\2\3\22\3\2\2\2\4\24\3\2\2\2\6 \3\2\2\2\b,\3"+
		"\2\2\2\n\65\3\2\2\2\fL\3\2\2\2\16Q\3\2\2\2\20U\3\2\2\2\22\\\3\2\2\2\24"+
		"\25\7r\2\2\25\26\7t\2\2\26\27\7k\2\2\27\30\7p\2\2\30\31\7v\2\2\31\35\3"+
		"\2\2\2\32\34\7\"\2\2\33\32\3\2\2\2\34\37\3\2\2\2\35\33\3\2\2\2\35\36\3"+
		"\2\2\2\36\5\3\2\2\2\37\35\3\2\2\2 !\7p\2\2!\"\7c\2\2\"#\7o\2\2#$\7g\2"+
		"\2$(\3\2\2\2%\'\7\"\2\2&%\3\2\2\2\'*\3\2\2\2(&\3\2\2\2()\3\2\2\2)\7\3"+
		"\2\2\2*(\3\2\2\2+-\7\f\2\2,+\3\2\2\2-.\3\2\2\2.,\3\2\2\2./\3\2\2\2/\60"+
		"\3\2\2\2\60\61\b\4\2\2\61\t\3\2\2\2\62\64\7\"\2\2\63\62\3\2\2\2\64\67"+
		"\3\2\2\2\65\63\3\2\2\2\65\66\3\2\2\2\668\3\2\2\2\67\65\3\2\2\289\7c\2"+
		"\29:\7u\2\2:<\3\2\2\2;=\7\"\2\2<;\3\2\2\2=>\3\2\2\2><\3\2\2\2>?\3\2\2"+
		"\2?G\3\2\2\2@D\5\f\6\2AC\7\"\2\2BA\3\2\2\2CF\3\2\2\2DB\3\2\2\2DE\3\2\2"+
		"\2EH\3\2\2\2FD\3\2\2\2G@\3\2\2\2HI\3\2\2\2IG\3\2\2\2IJ\3\2\2\2J\13\3\2"+
		"\2\2KM\t\2\2\2LK\3\2\2\2MN\3\2\2\2NL\3\2\2\2NO\3\2\2\2O\r\3\2\2\2PR\n"+
		"\3\2\2QP\3\2\2\2RS\3\2\2\2SQ\3\2\2\2ST\3\2\2\2T\17\3\2\2\2UV\t\4\2\2V"+
		"W\3\2\2\2WX\b\b\3\2X\21\3\2\2\2Y[\t\5\2\2ZY\3\2\2\2[^\3\2\2\2\\Z\3\2\2"+
		"\2\\]\3\2\2\2]_\3\2\2\2^\\\3\2\2\2_`\b\t\4\2`a\b\t\3\2a\23\3\2\2\2\16"+
		"\2\3\35(.\65>DINS\\\5\7\3\2\2\3\2\6\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
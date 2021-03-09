// Generated from /Users/n0253249/Desktop/workspace/dsl-maker/src/commonAntlrTest/antlr/XMLLexer.g4 by ANTLR 4.9.1
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class XMLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMENT=1, CDATA=2, DTD=3, OPEN_ENCODED=4, EntityRef=5, CharRef=6, SEA_WS=7, 
		OPEN=8, XMLDeclOpen=9, TEXT=10, CLOSE=11, SPECIAL_CLOSE=12, SLASH_CLOSE=13, 
		SLASH=14, EQUALS=15, STRING=16, Name=17, S=18, PI=19;
	public static final int
		INSIDE=1, PROC_INSTR=2;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "INSIDE", "PROC_INSTR"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"COMMENT", "CDATA", "DTD", "OPEN_ENCODED", "EntityRef", "CharRef", "SEA_WS", 
			"OPEN", "XMLDeclOpen", "SPECIAL_OPEN", "TEXT", "CLOSE", "SPECIAL_CLOSE", 
			"SLASH_CLOSE", "SLASH", "EQUALS", "STRING", "Name", "S", "HEXDIGIT", 
			"DIGIT", "NameChar", "NameStartChar", "PI", "IGNORE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, "'&lt;'", null, null, null, "'<'", null, null, 
			"'>'", null, "'/>'", "'/'", "'='"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMENT", "CDATA", "DTD", "OPEN_ENCODED", "EntityRef", "CharRef", 
			"SEA_WS", "OPEN", "XMLDeclOpen", "TEXT", "CLOSE", "SPECIAL_CLOSE", "SLASH_CLOSE", 
			"SLASH", "EQUALS", "STRING", "Name", "S", "PI"
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


	public XMLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "XMLLexer.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\25\u00f2\b\1\b\1"+
		"\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4"+
		"\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t"+
		"\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t"+
		"\30\4\31\t\31\4\32\t\32\3\2\3\2\3\2\3\2\3\2\3\2\7\2>\n\2\f\2\16\2A\13"+
		"\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3R\n"+
		"\3\f\3\16\3U\13\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\7\4_\n\4\f\4\16\4b\13"+
		"\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\7\3\7"+
		"\3\7\3\7\6\7w\n\7\r\7\16\7x\3\7\3\7\3\7\3\7\3\7\3\7\3\7\6\7\u0082\n\7"+
		"\r\7\16\7\u0083\3\7\3\7\5\7\u0088\n\7\3\b\3\b\5\b\u008c\n\b\3\b\6\b\u008f"+
		"\n\b\r\b\16\b\u0090\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3"+
		"\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\f\6\f\u00aa\n\f\r\f\16"+
		"\f\u00ab\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17"+
		"\3\17\3\20\3\20\3\21\3\21\3\22\3\22\7\22\u00c2\n\22\f\22\16\22\u00c5\13"+
		"\22\3\22\3\22\3\22\7\22\u00ca\n\22\f\22\16\22\u00cd\13\22\3\22\5\22\u00d0"+
		"\n\22\3\23\3\23\7\23\u00d4\n\23\f\23\16\23\u00d7\13\23\3\24\3\24\3\24"+
		"\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\27\3\27\5\27\u00e5\n\27\3\30\5\30"+
		"\u00e8\n\30\3\31\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\5?S`\2\33\5\3"+
		"\7\4\t\5\13\6\r\7\17\b\21\t\23\n\25\13\27\2\31\f\33\r\35\16\37\17!\20"+
		"#\21%\22\'\23)\24+\2-\2/\2\61\2\63\25\65\2\5\2\3\4\f\4\2\13\13\"\"\4\2"+
		"((>>\4\2$$>>\4\2))>>\5\2\13\f\17\17\"\"\5\2\62;CHch\3\2\62;\4\2/\60aa"+
		"\5\2\u00b9\u00b9\u0302\u0371\u2041\u2042\n\2<<C\\c|\u2072\u2191\u2c02"+
		"\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2\uffff\2\u00fc\2\5\3\2\2\2\2\7\3\2"+
		"\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2"+
		"\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\3\33\3\2\2\2\3\35\3"+
		"\2\2\2\3\37\3\2\2\2\3!\3\2\2\2\3#\3\2\2\2\3%\3\2\2\2\3\'\3\2\2\2\3)\3"+
		"\2\2\2\4\63\3\2\2\2\4\65\3\2\2\2\5\67\3\2\2\2\7F\3\2\2\2\tZ\3\2\2\2\13"+
		"g\3\2\2\2\rn\3\2\2\2\17\u0087\3\2\2\2\21\u008e\3\2\2\2\23\u0092\3\2\2"+
		"\2\25\u0096\3\2\2\2\27\u00a0\3\2\2\2\31\u00a9\3\2\2\2\33\u00ad\3\2\2\2"+
		"\35\u00b1\3\2\2\2\37\u00b6\3\2\2\2!\u00bb\3\2\2\2#\u00bd\3\2\2\2%\u00cf"+
		"\3\2\2\2\'\u00d1\3\2\2\2)\u00d8\3\2\2\2+\u00dc\3\2\2\2-\u00de\3\2\2\2"+
		"/\u00e4\3\2\2\2\61\u00e7\3\2\2\2\63\u00e9\3\2\2\2\65\u00ee\3\2\2\2\67"+
		"8\7>\2\289\7#\2\29:\7/\2\2:;\7/\2\2;?\3\2\2\2<>\13\2\2\2=<\3\2\2\2>A\3"+
		"\2\2\2?@\3\2\2\2?=\3\2\2\2@B\3\2\2\2A?\3\2\2\2BC\7/\2\2CD\7/\2\2DE\7@"+
		"\2\2E\6\3\2\2\2FG\7>\2\2GH\7#\2\2HI\7]\2\2IJ\7E\2\2JK\7F\2\2KL\7C\2\2"+
		"LM\7V\2\2MN\7C\2\2NO\7]\2\2OS\3\2\2\2PR\13\2\2\2QP\3\2\2\2RU\3\2\2\2S"+
		"T\3\2\2\2SQ\3\2\2\2TV\3\2\2\2US\3\2\2\2VW\7_\2\2WX\7_\2\2XY\7@\2\2Y\b"+
		"\3\2\2\2Z[\7>\2\2[\\\7#\2\2\\`\3\2\2\2]_\13\2\2\2^]\3\2\2\2_b\3\2\2\2"+
		"`a\3\2\2\2`^\3\2\2\2ac\3\2\2\2b`\3\2\2\2cd\7@\2\2de\3\2\2\2ef\b\4\2\2"+
		"f\n\3\2\2\2gh\7(\2\2hi\7n\2\2ij\7v\2\2jk\7=\2\2kl\3\2\2\2lm\b\5\3\2m\f"+
		"\3\2\2\2no\7(\2\2op\5\'\23\2pq\7=\2\2q\16\3\2\2\2rs\7(\2\2st\7%\2\2tv"+
		"\3\2\2\2uw\5-\26\2vu\3\2\2\2wx\3\2\2\2xv\3\2\2\2xy\3\2\2\2yz\3\2\2\2z"+
		"{\7=\2\2{\u0088\3\2\2\2|}\7(\2\2}~\7%\2\2~\177\7z\2\2\177\u0081\3\2\2"+
		"\2\u0080\u0082\5+\25\2\u0081\u0080\3\2\2\2\u0082\u0083\3\2\2\2\u0083\u0081"+
		"\3\2\2\2\u0083\u0084\3\2\2\2\u0084\u0085\3\2\2\2\u0085\u0086\7=\2\2\u0086"+
		"\u0088\3\2\2\2\u0087r\3\2\2\2\u0087|\3\2\2\2\u0088\20\3\2\2\2\u0089\u008f"+
		"\t\2\2\2\u008a\u008c\7\17\2\2\u008b\u008a\3\2\2\2\u008b\u008c\3\2\2\2"+
		"\u008c\u008d\3\2\2\2\u008d\u008f\7\f\2\2\u008e\u0089\3\2\2\2\u008e\u008b"+
		"\3\2\2\2\u008f\u0090\3\2\2\2\u0090\u008e\3\2\2\2\u0090\u0091\3\2\2\2\u0091"+
		"\22\3\2\2\2\u0092\u0093\7>\2\2\u0093\u0094\3\2\2\2\u0094\u0095\b\t\3\2"+
		"\u0095\24\3\2\2\2\u0096\u0097\7>\2\2\u0097\u0098\7A\2\2\u0098\u0099\7"+
		"z\2\2\u0099\u009a\7o\2\2\u009a\u009b\7n\2\2\u009b\u009c\3\2\2\2\u009c"+
		"\u009d\5)\24\2\u009d\u009e\3\2\2\2\u009e\u009f\b\n\3\2\u009f\26\3\2\2"+
		"\2\u00a0\u00a1\7>\2\2\u00a1\u00a2\7A\2\2\u00a2\u00a3\3\2\2\2\u00a3\u00a4"+
		"\5\'\23\2\u00a4\u00a5\3\2\2\2\u00a5\u00a6\b\13\4\2\u00a6\u00a7\b\13\5"+
		"\2\u00a7\30\3\2\2\2\u00a8\u00aa\n\3\2\2\u00a9\u00a8\3\2\2\2\u00aa\u00ab"+
		"\3\2\2\2\u00ab\u00a9\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\32\3\2\2\2\u00ad"+
		"\u00ae\7@\2\2\u00ae\u00af\3\2\2\2\u00af\u00b0\b\r\6\2\u00b0\34\3\2\2\2"+
		"\u00b1\u00b2\7A\2\2\u00b2\u00b3\7@\2\2\u00b3\u00b4\3\2\2\2\u00b4\u00b5"+
		"\b\16\6\2\u00b5\36\3\2\2\2\u00b6\u00b7\7\61\2\2\u00b7\u00b8\7@\2\2\u00b8"+
		"\u00b9\3\2\2\2\u00b9\u00ba\b\17\6\2\u00ba \3\2\2\2\u00bb\u00bc\7\61\2"+
		"\2\u00bc\"\3\2\2\2\u00bd\u00be\7?\2\2\u00be$\3\2\2\2\u00bf\u00c3\7$\2"+
		"\2\u00c0\u00c2\n\4\2\2\u00c1\u00c0\3\2\2\2\u00c2\u00c5\3\2\2\2\u00c3\u00c1"+
		"\3\2\2\2\u00c3\u00c4\3\2\2\2\u00c4\u00c6\3\2\2\2\u00c5\u00c3\3\2\2\2\u00c6"+
		"\u00d0\7$\2\2\u00c7\u00cb\7)\2\2\u00c8\u00ca\n\5\2\2\u00c9\u00c8\3\2\2"+
		"\2\u00ca\u00cd\3\2\2\2\u00cb\u00c9\3\2\2\2\u00cb\u00cc\3\2\2\2\u00cc\u00ce"+
		"\3\2\2\2\u00cd\u00cb\3\2\2\2\u00ce\u00d0\7)\2\2\u00cf\u00bf\3\2\2\2\u00cf"+
		"\u00c7\3\2\2\2\u00d0&\3\2\2\2\u00d1\u00d5\5\61\30\2\u00d2\u00d4\5/\27"+
		"\2\u00d3\u00d2\3\2\2\2\u00d4\u00d7\3\2\2\2\u00d5\u00d3\3\2\2\2\u00d5\u00d6"+
		"\3\2\2\2\u00d6(\3\2\2\2\u00d7\u00d5\3\2\2\2\u00d8\u00d9\t\6\2\2\u00d9"+
		"\u00da\3\2\2\2\u00da\u00db\b\24\7\2\u00db*\3\2\2\2\u00dc\u00dd\t\7\2\2"+
		"\u00dd,\3\2\2\2\u00de\u00df\t\b\2\2\u00df.\3\2\2\2\u00e0\u00e5\5\61\30"+
		"\2\u00e1\u00e5\t\t\2\2\u00e2\u00e5\5-\26\2\u00e3\u00e5\t\n\2\2\u00e4\u00e0"+
		"\3\2\2\2\u00e4\u00e1\3\2\2\2\u00e4\u00e2\3\2\2\2\u00e4\u00e3\3\2\2\2\u00e5"+
		"\60\3\2\2\2\u00e6\u00e8\t\13\2\2\u00e7\u00e6\3\2\2\2\u00e8\62\3\2\2\2"+
		"\u00e9\u00ea\7A\2\2\u00ea\u00eb\7@\2\2\u00eb\u00ec\3\2\2\2\u00ec\u00ed"+
		"\b\31\6\2\u00ed\64\3\2\2\2\u00ee\u00ef\13\2\2\2\u00ef\u00f0\3\2\2\2\u00f0"+
		"\u00f1\b\32\4\2\u00f1\66\3\2\2\2\25\2\3\4?S`x\u0083\u0087\u008b\u008e"+
		"\u0090\u00ab\u00c3\u00cb\u00cf\u00d5\u00e4\u00e7\b\b\2\2\7\3\2\5\2\2\7"+
		"\4\2\6\2\2\2\3\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
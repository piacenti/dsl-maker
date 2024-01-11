parser grammar IndentBasedParser;

options { tokenVocab=IndentBasedLexer; }
@parser::header{
import piacenti.dslmaker.dsl.DataType
}
main locals[dataType:DataType=DataType.UNKNOWN]: NEW_LINE? block EOF ;
blocks: block+;
block: dataPath alias? NEW_LINE INDENT  (action)? blocks? NEW_LINE? DEDENT ;
action: print | names;
print:PRINT NEW_LINE ;
names:NAME NEW_LINE;
alias: ALIAS;
dataPath : (DATA_PATH | NAME | PRINT)+;
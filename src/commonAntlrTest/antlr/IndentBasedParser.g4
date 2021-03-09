parser grammar IndentBasedParser;

options { tokenVocab=IndentBasedLexer; }
@parser::header{
import piacenti.dslmaker.dsl.DataType
}
main locals[dataType:DataType=DataType.UNKNOWN]: NEW_LINE? block EOF ;
blocks: block+;
block: dataPath alias? NEW_LINE INDENT  (action)? blocks? NEW_LINE? DEDENT ;
action: print | name;
print:PRINT NEW_LINE ;
name:NAME NEW_LINE;
alias: ALIAS;
dataPath : (DATA_PATH | NAME | PRINT)+;
grammar FreeForm;

main: NEW_LINE* (criteria)+ EOF;
criteria:criteriaKey NEW_LINE statement+ ;
group: andGroup| orGroup |value  ;
type: CODES|OBJECTS|VERBALIZATIONS;

andGroup: openParen orGroup closeParen andValues? | value andValues;
orGroup: openParen andGroup closeParen orValues? | value orValues;
andValues: and andGroup | and value andValues?;
orValues: or orGroup | or value orValues?;
statement:  type group  NEW_LINE;
value: SINGLE_QUOTED_STRING| DOUBLE_QUOTED_STRING | ID+ ;
and:AND;
or:OR;
openParen: OPEN;
closeParen:CLOSE;
criteriaKey:CRITERIA;

OPEN:'(';
CLOSE:')';
CRITERIA:'criteria';
SP: [ \t]+ ->channel(HIDDEN) ;

NEW_LINE:'\n';
AND: 'and';
OR: 'or';
CODES:'codes:';
OBJECTS:'objects:';
VERBALIZATIONS:'verbalizations:';
SINGLE_QUOTED_STRING:'\''('\\\''|~'\'')*'\'';
DOUBLE_QUOTED_STRING:'"'('\\"'|~'"')*'"';
ID: ~[ \t\r\n()'":]+;

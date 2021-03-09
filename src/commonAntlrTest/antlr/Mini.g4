
grammar Mini;


main: value (operator value)+ NL;
value: (ID|AND|OR)+?;
operator: AND | OR;
SP: ' ' ->skip ;
NL:'\n';
AND: 'and';
OR: 'or';
ID: ~[ \t\r\n]+;
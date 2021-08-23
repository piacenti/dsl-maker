grammar RewriterTest;

main: ID+;
ID: ~[ \t\r\n()'":]+;

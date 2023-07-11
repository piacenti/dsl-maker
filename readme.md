# DSL Maker

### History / Features

Originally created to support context-sensitive and regex supporting grammars. It consisted
of a recursive descent parser with several optimizations for localized caches and on the fly
tokenization (rather than an initial tokenization step). As a result it was fairly fast
for a recursive descent parser, only about 3 times slower than ANTLR. It was able to parse 
more easily certain types of texts and would not run the same regex evaluation more than once 
from the same point in the parsing process. It also exposed a lot of what is needed to create 
functionality around autocompletion to the point that I was able to create several smaller DSLs
with IDE like support. The main problem with it was that although decently fast for what it did
it was not fast enough for a real time typing experience.

To help with that I started using ANTLR 4. Although ANTLR is faster and able to handle much bigger
inputs, it lacked functionality around auto-completion and most suggestions involved simple token
recommendations. I tried C3 for auto-completion but also noticed it needed some hand holding in order
to know what things it should look for auto-completion. I wanted ANTRL to provide a similar experience as
the initial parser I had created provided. This library was enhanced with several utilities to allow
ANTRL 4 usage that makes it easier to be used for the items below among others:
* Auto-completion (based on a recursive descent implementation that leverages ANTLR 4 ATN states)
  * auto-completion suggests both tokens and entire parse rules. There is no need to tell it which
ones you are looking for, you are just responsible for providing the auto-completion text for the parse rules
recommended. This allows you to provide auto-completion suggestions/examples that completes the entire 
parse rule.
* Highlighting (Currently HTML based, browser focused)
* Formatting
* Parse Tree Printing in ASCII format
* FastTokenRewriter
  * While using the existing implementation of ANTRL TokenRewriter it was clear it was very slow for the use
above, specially for real time highlighting. After examining its implementation and points of slowness I created
this version that is significantly faster
  * This also supports providing text change computations. Do you want to highlight and format a file using
different listeners focused on doing each action individually? with computations, you can, you can merge
them and create the final output that contains both sets of changes. This allows you to not have to write 
code only used for highlighting, another one for formatting and a third that correctly does both at the same time.

### Examples

[ANTLR Completion](src%2FcommonTest%2Fkotlin%2Fpiacenti%2Fdslmaker%2Fdsl%2FFreeFormAntlrCompleterTest.kt)
[ANTLR Completion with Indentation](src%2FcommonTest%2Fkotlin%2Fpiacenti%2Fdslmaker%2Fdsl%2FFreeFormAntlrCompleterTest.kt)
[Recursive Descent Completer](src%2FcommonTest%2Fkotlin%2Fpiacenti%2Fdslmaker%2Fdsl%2FCompleterTest.kt)
[ANTRL Highlighting and Formatting](src%2FcommonTest%2Fkotlin%2Fpiacenti%2Fdslmaker%2Fdsl%2FXMLFastHighlighterAndFormatterCommon.kt)

**public key for validating dependency available from keyserver.ubuntu.com**
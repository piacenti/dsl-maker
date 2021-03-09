package piacenti.dslmaker

internal expect fun getCurrentTimeInMilliSeconds(): Long
expect fun  String.genericRegex(): Regex
data class Group(val text:String?, val startIndex: Int, val endIndex:Int)
data class RegexMatch(val groups:List<Group>)
expect fun regexMatchFromOffset(text:String, patternString:String, startIndex:Int):RegexMatch?
interface Logger{
    fun info(message:String)
    fun debug(message:String)
    fun error(message:String)
}
internal expect val Any.LOG:Logger

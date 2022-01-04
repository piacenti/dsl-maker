package piacenti.dslmaker

import kotlin.js.Date
import kotlin.js.RegExp

internal actual fun getCurrentTimeInMilliSeconds(): Long {
    return Date().getTime().toLong()
}


actual fun String.genericRegex(): Regex {
    val pair = genericRegexString()
    var result = pair.first
    val options = pair.second
    return result.toRegex(options)
}

private fun String.genericRegexString(): Pair<String, MutableSet<RegexOption>> {
    var result = this
    val options = mutableSetOf<RegexOption>()
    val regex = """\(\?([ims]+?)\)""".toRegex()
    regex.find(this)?.let {
        val match = it.groupValues[1]
        when {
            match.contains("m") -> options.add(RegexOption.MULTILINE)
            match.contains("i") -> options.add(RegexOption.IGNORE_CASE)
            match.contains("s") -> result = result.replace("([^\\\\])\\.".toRegex(), """$1[\\s\\S]""")
            else -> Unit
        }
    }
    result = result.replace(regex, "")
    return Pair(result, options)
}

enum class LogLevel {
    INFO, DEBUG, ERROR
}

var logLevel = LogLevel.ERROR


class JSLogger(val className: String) : Logger {
    override fun info(message: String) {
        if (logLevel in listOf(LogLevel.INFO, LogLevel.DEBUG))
            console.info("$className INFO: $message")
    }

    override fun debug(message: String) {
        if (logLevel in listOf(LogLevel.DEBUG))
            console.log("$className DEBUG: $message")
    }

    override fun error(message: String) {
        if (logLevel in listOf(LogLevel.INFO, LogLevel.DEBUG, LogLevel.ERROR))
            console.error("$className ERROR: $message")
    }

}

internal actual val Any.LOG: Logger
    get() = JSLogger(this::class.simpleName ?: "no name")

actual fun regexMatchFromOffset(text: String, patternString: String, startIndex: Int): RegexMatch? {
    val result = regexMatch1(patternString, text, startIndex)
    return result
}

private fun regexMatch1(patternString: String, text: String, startIndex: Int): RegexMatch? {
    val match = patternString.genericRegex().find(text.substring(startIndex))
    return match?.let { matchResult ->
        RegexMatch(matchResult.groups.mapNotNull { it }.map { Group(it.value, startIndex + matchResult.range.first, startIndex + matchResult.range.end) })
    }
}

data class PatternAndOptions(val pattern: String, val options: String)

private val patternMap = mutableMapOf<String, PatternAndOptions>()
//this should be faster but it is actually slower so ignoring it for now
private fun regexMatch2(patternString: String, text: String, startIndex: Int): RegexMatch? {
    val patternAndOption = patternMap.getOrPut(patternString) {
        val (pattern, options) = patternString.genericRegexString()
        val flags = options.joinToString("") { it.value } + "g"
        val startsWithLineBeginning = pattern.startsWith("^")
        val finalPattern = if (startsWithLineBeginning) pattern.drop(1) else pattern
        PatternAndOptions(finalPattern, flags)
    }
    val regexExp = RegExp(patternAndOption.pattern, patternAndOption.options)
    regexExp.lastIndex = startIndex
    val match = regexExp.exec(text)
    return if (match != null) {
        if (match.index == startIndex) {
            RegexMatch((0 until match.length).map { matchResultIndex ->
                val matchText = match[matchResultIndex]
                Group(matchText ?: "", match.index, regexExp.lastIndex)
            })
        } else null
    } else null
}
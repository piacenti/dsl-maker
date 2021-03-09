package piacenti.dslmaker

import org.slf4j.LoggerFactory
import java.util.regex.Pattern

internal actual fun getCurrentTimeInMilliSeconds(): Long {
    return System.currentTimeMillis()
}

actual fun String.genericRegex(): Regex {
    return this.toRegex(RegexOption.DOT_MATCHES_ALL)
}

class JVMLogger(private val logger: org.slf4j.Logger, private val className: String) : Logger {
    override fun info(message: String) {
        if (logger.isInfoEnabled)
            logger.info(message)
    }

    override fun debug(message: String) {
        if (logger.isDebugEnabled)
            logger.debug(message)
    }

    override fun error(message: String) {
        if (logger.isErrorEnabled)
            logger.error(message)
    }

}

private val loggerMap: MutableMap<String, Logger> = mutableMapOf()
internal actual val Any.LOG: Logger
    get() {
        val simpleName = this::class.java.simpleName
        val cachedLogger = loggerMap[simpleName]
        val logger = if (cachedLogger == null) {
            val newLogger = JVMLogger(LoggerFactory.getLogger(this::class.java), simpleName)
            loggerMap[simpleName] = newLogger
            newLogger

        } else {
            cachedLogger
        }
        return logger
    }
val patternMap: MutableMap<String, Pattern> = mutableMapOf()
actual fun regexMatchFromOffset(text: String, patternString: String, startIndex: Int): RegexMatch? {
    val pattern = patternMap.getOrPut(patternString) { Pattern.compile(patternString) }
    val matcher = pattern.matcher(text)
    matcher.region(startIndex, text.length)
    if (matcher.find()) {
        val groups = mutableListOf<Group>()
        for (x in 0..matcher.groupCount()) {
            groups.add(Group(matcher.group(x), matcher.start(x), matcher.end(x)))
        }
        return RegexMatch(groups)
    }
    return null
}
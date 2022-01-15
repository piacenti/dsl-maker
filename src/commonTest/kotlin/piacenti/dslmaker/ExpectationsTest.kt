package piacenti.dslmaker

import kotlin.test.Test
import kotlin.test.assertEquals

class ExpectationsTest {
    @Test
    fun genericRegexShouldWork() {
        val sample = """
definitions
    ...
if
	...
		..fasfdasdf

then
	...
        """.trimIndent()
        val genericRegex = "(?smi)^\\s*if(.+?)^\\s*then\\s".genericRegex()
        println("([^\\\\])\\.".toRegex())
        val group = genericRegex.find(sample)?.groupValues?.get(1)
        assertEquals(
            """
	...
		..fasfdasdf
""", group
        )
    }

}
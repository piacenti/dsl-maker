package piacenti.dslmaker

import kotlin.test.Test


class ExtensionsKtTest {

    @Test
    fun previewLines() {
        """
            a
            b
            c
            d
            e
            f
            g
        """.trimIndent().previewLines(2) validateEquals """
            a
            b
            ...
            f
            g
        """.trimIndent()
        """
            a
            b
            c
            d
            e
            f
        """.trimIndent().previewLines(3) validateEquals """
            a
            b
            c
            d
            e
            f
        """.trimIndent()
    }

    @Test
    fun previewCharacters() {
        "abcdefg".previewCharacters(2) validateEquals "ab...fg"
        "abcdef".previewCharacters(3) validateEquals "abcdef"
    }
}
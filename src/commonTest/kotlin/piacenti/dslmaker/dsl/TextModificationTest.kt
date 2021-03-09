package piacenti.dslmaker.dsl

import kotlinx.serialization.json.Json
import piacenti.dslmaker.validateEquals
import kotlin.test.Test


class TextModificationTest {
    val json = Json{ignoreUnknownKeys=true}

    @Test
    fun should_be_serializable() {
        var obj = TextModification("test", Order.AFTER)
        var result = json.encodeToString(TextModification.serializer(), obj)
        obj validateEquals json.decodeFromString(TextModification.serializer(), result)

        obj = TextModification("test", Order.AFTER, InsertionType.STYLE, 0..2)
        result = json.encodeToString(TextModification.serializer(), obj)
        obj validateEquals json.decodeFromString(TextModification.serializer(), result)
    }
}
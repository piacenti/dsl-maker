package piacenti.dslmaker.dsl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import piacenti.dslmaker.end

enum class Order {
    BEFORE, AFTER, REPLACE, DELETE
}

enum class InsertionType {
    FORMAT, STYLE
}

@Serializer(forClass = IntRange::class)
object IntRangeSerializer : KSerializer<IntRange> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DashboardTool") {
        element<String>("start") // req will have index 0
        element<String>("end") // req will have index 1
    }

    private const val startIndex = 0
    private const val endIndex = 1

    override fun serialize(encoder: Encoder, obj: IntRange) {
        val compositeOutput = encoder.beginStructure(descriptor)
        compositeOutput.encodeStringElement(descriptor, startIndex, obj.first.toString())
        compositeOutput.encodeStringElement(descriptor, endIndex, obj.last.toString())
        compositeOutput.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): IntRange {
        val dec: CompositeDecoder = decoder.beginStructure(descriptor)
        lateinit var start: String // consider using flags or bit mask if you
        lateinit var end: String // need to read nullable non-optional properties
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                startIndex -> start = dec.decodeStringElement(descriptor, i)
                endIndex -> end = dec.decodeStringElement(descriptor, i)
            }
        }
        dec.endStructure(descriptor)
        return IntRange(start.toInt(), end.toInt())
    }
}

@Serializable
data class TextModification(val text: String, val order: Order, val insertionType: InsertionType = InsertionType.FORMAT, @Serializable(with = IntRangeSerializer::class) val range: IntRange? = null)
interface TextModifier {
    companion object {
        fun applyModificationsMap(text: String, map: Map<Int, List<TextModification>>, originalCharTransformation: (Char) -> String = { it.toString() }): String {
            return buildString {
                var i = 0
                while (i < text.length) {
                    val char = text[i]
                    val list = map[i]
                    if (list != null) {
                        val before = list.filter { it.order == Order.BEFORE }.sortedBy { it.insertionType }
                        before.forEach {
                            append(it.text)
                        }
                        val delete = list.filter { it.order == Order.DELETE }
                        val replace = list.filter { it.order == Order.REPLACE }
                        val deleteEmpty = delete.isEmpty()
                        val replaceEmpty = replace.isEmpty()
                        if (deleteEmpty && replaceEmpty) {
                            append(originalCharTransformation(char))
                            i++
                        } else if (!deleteEmpty) {
                            val fullRange = delete.foldRight(i..i) { value, acc ->
                                val range = value.range
                                if (range != null && acc.last < range.last) {
                                    range
                                } else
                                    acc
                            }
                            insertAfterIfNotSame(fullRange, i,map)
                            i = if (fullRange.end > i) fullRange.end else i + 1
                        } else if (!replaceEmpty) {
                            val overArchingMod = replace.foldRight(replace.first()) { value, acc ->
                                val range = value.range
                                val accRange = acc.range
                                if (accRange != null && range != null && accRange.last < range.last) {
                                    value
                                } else
                                    acc
                            }
                            append(overArchingMod.text)
                            val end = overArchingMod.range?.end
                            insertAfterIfNotSame(overArchingMod.range, i,map)
                            i = if (end != null && end > i) end else i + 1
                        }
                        insertAfter(list)
                    } else {
                        append(originalCharTransformation(char))
                        i++
                    }
                }
                val lastChange = map[text.length]
                if (lastChange != null) {
                    lastChange.forEach {
                        append(it.text)
                    }

                }
            }

        }
        private fun StringBuilder.insertAfterIfNotSame(fullRange: IntRange?, i: Int, textComputations: Map<Int, List<TextModification>>) {
            //insert after statements before skipping if last index is not current index
            if (fullRange != null && fullRange.last != i)
                textComputations[fullRange.last]?.let {
                    insertAfter(it)
                }
        }
        private fun StringBuilder.insertAfter(list: List<TextModification>) {
            val after = list.filter { it.order == Order.AFTER }.sortedByDescending { it.insertionType }
            after.forEach {
                append(it.text)
            }
        }
    }
}
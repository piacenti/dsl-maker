package piacenti.dslmaker.dsl

// multiplatform implementation of Dice's Coefficient
fun String.similarity(other: String): Double {
    // remove spaces
    val first = this.replace("""\s+""", "")
    val second = other.replace("""\s+""", "")

    if (first.isEmpty() && second.isEmpty()) return 1.0                   // if both are empty strings
    if (first.isEmpty() || second.isEmpty()) return 0.0                   // if only one is empty string
    if (first === second) return 1.0                                 // identical
    if (first.length == 1 && second.length == 1) return 0.0         // both are 1-letter strings
    if (first.length < 2 || second.length < 2) return 0.0         // if either is a 1-letter string

    val firstBigrams = mutableMapOf<String, Int>()
    for (i in 0 until first.length - 2) {
        val bigram = first.substring(i, i + 2)
        val count = (firstBigrams[bigram] ?: 0) + 1
        firstBigrams[bigram] = count
    }

    var intersectionSize = 0
    for (i in 0 until second.length - 2) {
        val bigram = second.substring(i, i + 2)
        val count = firstBigrams[bigram] ?: 0
        if (count > 0) {
            firstBigrams[bigram] = count - 1
            intersectionSize++
        }
    }
    return (2.0 * intersectionSize) / (first.length + second.length - 2)
}
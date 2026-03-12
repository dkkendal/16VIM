package inc.flide.vim8.ime.nlp

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the pure logic helpers in [WordFrequencyRepository].
 *
 * The real repository requires an Android [android.content.Context] and a
 * live SQLite database, both unavailable in the JVM unit-test environment.
 * [WordFrequencyRepositoryTestSubject] mirrors the portable pieces so we can
 * validate them without the Android runtime.
 */
class WordFrequencyRepositorySpec : FunSpec({

    val subject = WordFrequencyRepositoryTestSubject()

    // -------------------------------------------------------------------------
    // parseSeedLine
    // -------------------------------------------------------------------------

    context("parseSeedLine — valid CSV lines") {
        withData(
            nameFn = { (line, _) -> "'$line'" },
            "hello,50" to ("hello" to 50),
            "world , 100" to ("world" to 100),
            "THE,200" to ("the" to 200),
            "  spaces  , 42" to ("spaces" to 42)
        ) { (line, expected) ->
            subject.parseSeedLine(line) shouldBe expected
        }
    }

    context("parseSeedLine — invalid CSV lines return null") {
        withData(
            nameFn = { "'$it'" },
            "",
            "   ",
            "nocomma",
            "word,notanumber",
            ",50"
        ) { line ->
            subject.parseSeedLine(line) shouldBe null
        }
    }

    // -------------------------------------------------------------------------
    // shouldRecordWord — filtering rules for recordWord
    // -------------------------------------------------------------------------

    context("shouldRecordWord — words that pass the filter") {
        withData(
            nameFn = { it },
            "hi",
            "the",
            "hello",
            "keyboard"
        ) { word ->
            subject.shouldRecordWord(word) shouldBe true
        }
    }

    context("shouldRecordWord — words that are ignored") {
        withData(
            nameFn = { "'$it'" },
            "",
            " ",
            "a",
            "I"
        ) { word ->
            subject.shouldRecordWord(word) shouldBe false
        }
    }

    // -------------------------------------------------------------------------
    // normaliseSeedLines — batch CSV parsing (used inside seedIfNeeded)
    // -------------------------------------------------------------------------

    test("normaliseSeedLines parses a realistic mini-seed correctly") {
        val csv = listOf(
            "the,200",
            "be,198",
            "bad line",
            "to,196",
            ",0",
            "a,190"
        )
        val result = subject.normaliseSeedLines(csv)
        result shouldContainExactlyInAnyOrder listOf(
            "the" to 200,
            "be" to 198,
            "to" to 196,
            "a" to 190
        )
    }

    test("normaliseSeedLines returns empty list for all-invalid input") {
        val csv = listOf("nocomma", "bad", "", "  ")
        subject.normaliseSeedLines(csv).shouldBeEmpty()
    }
})

// ---------------------------------------------------------------------------
// Test subject — portable logic extracted from WordFrequencyRepository
// ---------------------------------------------------------------------------

private class WordFrequencyRepositoryTestSubject {

    /**
     * Parses one CSV line (`word,frequency`) and returns a normalised pair,
     * or null if the line is malformed.
     */
    fun parseSeedLine(line: String): Pair<String, Int>? {
        val parts = line.split(",")
        if (parts.size < 2) return null
        val word = parts[0].trim().lowercase()
        val freq = parts[1].trim().toIntOrNull() ?: return null
        if (word.isBlank()) return null
        return word to freq
    }

    /**
     * Returns true when [word] should be persisted in the frequency database.
     * Mirrors the guard in [WordFrequencyRepository.recordWord].
     */
    fun shouldRecordWord(word: String): Boolean {
        val lower = word.lowercase().trim()
        return lower.isNotBlank() && lower.length >= 2
    }

    /**
     * Parses a list of CSV lines and returns all valid (word, frequency) pairs.
     */
    fun normaliseSeedLines(lines: List<String>): List<Pair<String, Int>> =
        lines.mapNotNull { parseSeedLine(it) }
}

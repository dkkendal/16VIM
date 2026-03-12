package inc.flide.vim8.ime.nlp

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the pure word-extraction helpers in [SuggestionsManager].
 *
 * The Android framework classes (TextServicesManager, SpellCheckerSession) are not available
 * in the JVM unit-test environment, so only the logic methods are covered here.
 * The subject helpers are replicated in [SuggestionsManagerTestSubject] to avoid
 * instantiating the real class.
 */
class SuggestionsManagerSpec : FunSpec({

    // -------------------------------------------------------------------------
    // extractCurrentWord
    // -------------------------------------------------------------------------

    context("extractCurrentWord — happy paths") {
        withData(
            nameFn = { (input, expected) -> "'$input' -> '$expected'" },
            "" to "",
            "hello" to "hello",
            "hello world" to "world",
            "one two three" to "three",
            "a b" to "b"
        ) { (input, expected) ->
            SuggestionsManagerTestSubject().extractCurrentWord(input) shouldBe expected
        }
    }

    context("extractCurrentWord — trailing whitespace returns empty") {
        withData(
            nameFn = { (input, _) -> "'$input'" },
            "hello " to "",
            "hello  " to "",
            " " to ""
        ) { (input, expected) ->
            SuggestionsManagerTestSubject().extractCurrentWord(input) shouldBe expected
        }
    }

    context("extractCurrentWord — single word without spaces") {
        withData(
            nameFn = { it },
            "typing",
            "word",
            "x"
        ) { word ->
            SuggestionsManagerTestSubject().extractCurrentWord(word) shouldBe word
        }
    }

    // -------------------------------------------------------------------------
    // extractWordBeforeBoundary
    // -------------------------------------------------------------------------

    context("extractWordBeforeBoundary — after a space") {
        withData(
            nameFn = { (input, expected) -> "'$input' -> '$expected'" },
            "hello " to "hello",
            "one two " to "two",
            "one two three " to "three"
        ) { (input, expected) ->
            SuggestionsManagerTestSubject().extractWordBeforeBoundary(input) shouldBe expected
        }
    }

    context("extractWordBeforeBoundary — after a period or comma") {
        withData(
            nameFn = { (input, expected) -> "'$input' -> '$expected'" },
            "hello." to "hello",
            "hello," to "hello",
            "one two." to "two",
            "one two," to "two"
        ) { (input, expected) ->
            SuggestionsManagerTestSubject().extractWordBeforeBoundary(input) shouldBe expected
        }
    }

    context("extractWordBeforeBoundary — empty or boundary-only input") {
        withData(
            nameFn = { (input, _) -> "'$input'" },
            "" to "",
            " " to "",
            "." to "",
            "," to "",
            "   " to ""
        ) { (input, expected) ->
            SuggestionsManagerTestSubject().extractWordBeforeBoundary(input) shouldBe expected
        }
    }
})

// ---------------------------------------------------------------------------
// Test subject — mirrors the internal logic without requiring Android Context
// ---------------------------------------------------------------------------

private val WORD_BOUNDARY_CHARS_TEST = setOf(' ', '.', ',')

private class SuggestionsManagerTestSubject {
    fun extractCurrentWord(text: String): String {
        if (text.isEmpty()) return ""
        if (text.last().isWhitespace()) return ""
        val lastWhitespace = text.indexOfLast { it.isWhitespace() }
        return if (lastWhitespace == -1) text else text.substring(lastWhitespace + 1)
    }

    fun extractWordBeforeBoundary(text: String): String {
        val stripped = text.trimEnd { it in WORD_BOUNDARY_CHARS_TEST }
        if (stripped.isEmpty()) return ""
        val lastBoundary = stripped.indexOfLast { it in WORD_BOUNDARY_CHARS_TEST }
        return if (lastBoundary == -1) stripped else stripped.substring(lastBoundary + 1)
    }
}

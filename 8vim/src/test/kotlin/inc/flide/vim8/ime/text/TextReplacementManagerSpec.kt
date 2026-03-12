package inc.flide.vim8.ime.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for the pure-logic helpers in [TextReplacementManager].
 *
 * The class constructor requires an Android [Context] (for [appPreferenceModel]), so all
 * test-driven logic is exercised through [TextReplacementManagerTestSubject], which mirrors
 * the internal helper implementations without any Android dependencies.
 *
 * This mirrors the pattern used in [inc.flide.vim8.ime.nlp.SuggestionsManagerSpec].
 */
class TextReplacementManagerSpec : FunSpec({

    val subject = TextReplacementManagerTestSubject()

    // -------------------------------------------------------------------------
    // parseEntries
    // -------------------------------------------------------------------------

    context("parseEntries — valid entries") {
        withData(
            nameFn = { (input, _) -> "raw='$input'" },
            setOf("btw|||By the way") to mapOf("btw" to "By the way"),
            setOf("omw|||On my way!", "btw|||By the way") to
                mapOf("omw" to "On my way!", "btw" to "By the way"),
            setOf("a|||b") to mapOf("a" to "b")
        ) { (rawSet, expected) ->
            subject.parseEntries(rawSet) shouldContainExactly expected
        }
    }

    context("parseEntries — malformed entries are skipped") {
        withData(
            nameFn = { it.toString() },
            setOf("no-separator-here"),
            setOf("|||onlySeparatorAtStart"),
            setOf("valid|||ok", "bad-entry")
        ) { rawSet ->
            val result = subject.parseEntries(rawSet)
            // Entries without a separator should be absent from the map
            result.keys.none { it.contains("bad-entry") || it == "no-separator-here" } shouldBe true
        }
    }

    test("parseEntries — empty set returns empty map") {
        subject.parseEntries(emptySet()).shouldBeEmpty()
    }

    test("parseEntries — entry with separator in expansion is handled correctly") {
        // The expansion itself may contain |||; we split on the FIRST occurrence only
        val raw = setOf("abbr|||exp|||with|||pipes")
        val result = subject.parseEntries(raw)
        result["abbr"] shouldBe "exp|||with|||pipes"
    }

    // -------------------------------------------------------------------------
    // encodeEntry
    // -------------------------------------------------------------------------

    test("encodeEntry — produces expected format") {
        subject.encodeEntry("btw", "By the way") shouldBe "btw|||By the way"
    }

    test("encodeEntry — round-trip through parseEntries") {
        val encoded = subject.encodeEntry("omw", "On my way!")
        val result = subject.parseEntries(setOf(encoded))
        result["omw"] shouldBe "On my way!"
    }

    // -------------------------------------------------------------------------
    // findReplacement
    // -------------------------------------------------------------------------

    context("findReplacement — no match returns null") {
        withData(
            nameFn = { (text, _) -> "text='$text'" },
            // Unknown abbreviation
            "xyz " to mapOf("btw" to "By the way"),
            // Empty map
            "btw " to emptyMap(),
            // Text does not end with trigger char
            "btw" to mapOf("btw" to "By the way"),
            // Empty text
            "" to mapOf("btw" to "By the way"),
            // Only a trigger char — word is empty
            " " to mapOf("btw" to "By the way")
        ) { (text, map) ->
            subject.findReplacement(text, map) shouldBe null
        }
    }

    context("findReplacement — match at end of text (space trigger)") {
        withData(
            nameFn = { (text, expected) -> "'$text' -> $expected" },
            // Simple word + space
            "btw " to Pair(4, "By the way "),
            // Word preceded by other text
            "hello btw " to Pair(4, "By the way ")
        ) { (text, expected) ->
            val map = mapOf("btw" to "By the way")
            subject.findReplacement(text, map) shouldBe expected
        }
    }

    context("findReplacement — trigger character is preserved in replacement") {
        withData(
            nameFn = { (trigger, _) -> "trigger='$trigger'" },
            '.' to Pair(4, "By the way."),
            ',' to Pair(4, "By the way,"),
            '!' to Pair(4, "By the way!"),
            '?' to Pair(4, "By the way?"),
            ' ' to Pair(4, "By the way ")
        ) { (trigger, expected) ->
            val map = mapOf("btw" to "By the way")
            subject.findReplacement("btw$trigger", map) shouldBe expected
        }
    }

    test("findReplacement — charsToDelete equals abbreviation length + 1") {
        // "omw" (3 chars) + space (1) = 4
        val map = mapOf("omw" to "On my way!")
        val result = subject.findReplacement("omw ", map)
        result shouldNotBe null
        result!!.first shouldBe 4
    }

    test("findReplacement — case-sensitive: 'BTW' does not match 'btw' mapping") {
        val map = mapOf("btw" to "By the way")
        subject.findReplacement("BTW ", map) shouldBe null
    }

    test("findReplacement — word after another trigger char is matched") {
        // "hello.btw " — the word before ' ' is "btw", which starts after the '.'
        val map = mapOf("btw" to "By the way")
        val result = subject.findReplacement("hello.btw ", map)
        result shouldNotBe null
        result!!.first shouldBe 4 // "btw" + ' '
        result.second shouldBe "By the way "
    }

    test("findReplacement — non-trigger last char returns null") {
        val map = mapOf("btw" to "By the way")
        subject.findReplacement("btw", map) shouldBe null
        subject.findReplacement("btw\n", map) shouldBe null
    }

    // -------------------------------------------------------------------------
    // encodeEntry / addEntry / removeEntry logic (via TestSubject)
    // -------------------------------------------------------------------------

    test("addEntry — new entry is stored and readable") {
        val store = mutableSetOf<String>()
        subject.addEntryTo(store, "omw", "On my way!")
        val map = subject.parseEntries(store)
        map["omw"] shouldBe "On my way!"
    }

    test("addEntry — duplicate abbreviation overwrites existing expansion") {
        val store = mutableSetOf<String>()
        subject.addEntryTo(store, "omw", "On my way!")
        subject.addEntryTo(store, "omw", "On my way home!")
        val map = subject.parseEntries(store)
        map["omw"] shouldBe "On my way home!"
        map.size shouldBe 1
    }

    test("removeEntry — removes the correct entry") {
        val store = mutableSetOf<String>()
        subject.addEntryTo(store, "omw", "On my way!")
        subject.addEntryTo(store, "btw", "By the way")
        subject.removeEntryFrom(store, "omw")
        val map = subject.parseEntries(store)
        map.containsKey("omw") shouldBe false
        map["btw"] shouldBe "By the way"
    }

    test("removeEntry — no-op for non-existent abbreviation") {
        val store = mutableSetOf<String>()
        subject.addEntryTo(store, "btw", "By the way")
        subject.removeEntryFrom(store, "xyz")
        subject.parseEntries(store).size shouldBe 1
    }
})

// ---------------------------------------------------------------------------
// Test subject — replicates internal pure logic without Android Context
// ---------------------------------------------------------------------------

private const val SEP = "|||"

private class TextReplacementManagerTestSubject {
    fun parseEntries(rawSet: Set<String>): Map<String, String> =
        rawSet.mapNotNull { entry ->
            val idx = entry.indexOf(SEP)
            if (idx < 0) null
            else entry.substring(0, idx) to entry.substring(idx + SEP.length)
        }.toMap()

    fun encodeEntry(abbreviation: String, expansion: String): String =
        "$abbreviation$SEP$expansion"

    fun findReplacement(
        textBeforeCursor: String,
        map: Map<String, String>
    ): Pair<Int, String>? {
        if (textBeforeCursor.isEmpty()) return null
        val triggerChars = setOf(' ', '.', ',', '!', '?')
        val boundaryChar = textBeforeCursor.last()
        if (boundaryChar !in triggerChars) return null
        val withoutBoundary = textBeforeCursor.dropLast(1)
        if (withoutBoundary.isEmpty()) return null
        val lastBoundaryIdx = withoutBoundary.indexOfLast { it in triggerChars }
        val word = if (lastBoundaryIdx == -1) withoutBoundary
        else withoutBoundary.substring(lastBoundaryIdx + 1)
        if (word.isEmpty()) return null
        val expansion = map[word] ?: return null
        return Pair(word.length + 1, "$expansion$boundaryChar")
    }

    fun addEntryTo(store: MutableSet<String>, abbreviation: String, expansion: String) {
        store.removeAll { it.startsWith("$abbreviation$SEP") }
        store.add(encodeEntry(abbreviation, expansion))
    }

    fun removeEntryFrom(store: MutableSet<String>, abbreviation: String) {
        store.removeAll { it.startsWith("$abbreviation$SEP") }
    }
}

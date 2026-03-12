package inc.flide.vim8.ime.text

import android.content.Context
import inc.flide.vim8.Vim8ImeService
import inc.flide.vim8.appPreferenceModel

/**
 * Manages the Automated Text Replacement feature.
 *
 * Users define abbreviation → expansion pairs (e.g. "omw" → "On my way!") in Settings.
 * Whenever [checkAndReplace] is called after a trigger character is committed, the text
 * before the cursor is scanned for a matching abbreviation.  If one is found, the
 * abbreviation plus its trailing trigger character are deleted and the expansion plus
 * the trigger character are inserted in their place.
 *
 * All preference I/O is synchronous and happens on whatever thread calls these methods.
 * [checkAndReplace] should therefore only be called from the IME's text-commit path
 * (i.e. [inc.flide.vim8.ime.keyboard.text.KeyboardManager.handleText]), which already
 * runs on the main thread.
 */
class TextReplacementManager(context: Context) {
    private val prefs by appPreferenceModel()

    companion object {
        /**
         * Separator between abbreviation and expansion in the stored [Set<String>].
         * Chosen to be unlikely in typical abbreviation/expansion text.
         */
        internal const val ENTRY_SEPARATOR = "|||"

        /**
         * Characters that signal the end of a word.  When the user types one of these
         * after an abbreviation the replacement check is triggered.
         */
        val TRIGGER_CHARS: Set<Char> = setOf(' ', '.', ',', '!', '?')
    }

    // -------------------------------------------------------------------------
    // Internal pure-logic helpers (unit-testable without Android context)
    // -------------------------------------------------------------------------

    /**
     * Converts the raw [Set<String>] stored in preferences into an abbreviation→expansion map.
     * Entries that do not contain [ENTRY_SEPARATOR] are silently ignored.
     */
    internal fun parseEntries(rawSet: Set<String>): Map<String, String> =
        rawSet.mapNotNull { entry ->
            val idx = entry.indexOf(ENTRY_SEPARATOR)
            if (idx < 0) null
            else entry.substring(0, idx) to entry.substring(idx + ENTRY_SEPARATOR.length)
        }.toMap()

    /**
     * Encodes a single abbreviation+expansion pair into the storage format.
     */
    internal fun encodeEntry(abbreviation: String, expansion: String): String =
        "$abbreviation$ENTRY_SEPARATOR$expansion"

    /**
     * Given [textBeforeCursor] and a pre-parsed [map] of abbreviations, determines whether
     * the word immediately preceding the last character (which must be a [TRIGGER_CHARS] member)
     * is a known abbreviation.
     *
     * @return A [Pair] of (charsToDelete, replacementText) if a replacement should be made,
     *         or `null` if no replacement applies.
     *         `charsToDelete` covers the abbreviation length plus 1 for the trigger character.
     *         `replacementText` is `expansion + triggerChar`.
     */
    internal fun findReplacement(
        textBeforeCursor: String,
        map: Map<String, String>
    ): Pair<Int, String>? {
        if (textBeforeCursor.isEmpty()) return null

        val boundaryChar = textBeforeCursor.last()
        if (boundaryChar !in TRIGGER_CHARS) return null

        val withoutBoundary = textBeforeCursor.dropLast(1)
        if (withoutBoundary.isEmpty()) return null

        // The current word starts after the last trigger/whitespace character.
        val lastBoundaryIdx = withoutBoundary.indexOfLast { it in TRIGGER_CHARS }
        val word = if (lastBoundaryIdx == -1) withoutBoundary
        else withoutBoundary.substring(lastBoundaryIdx + 1)

        if (word.isEmpty()) return null

        val expansion = map[word] ?: return null

        val charsToDelete = word.length + 1 // abbreviation + trigger char
        val replacementText = "$expansion$boundaryChar"
        return Pair(charsToDelete, replacementText)
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns all stored abbreviation→expansion pairs as a list sorted by abbreviation.
     */
    fun getEntries(): List<Pair<String, String>> =
        parseEntries(prefs.textReplacement.entries.get())
            .toList()
            .sortedBy { it.first }

    /**
     * Persists a new abbreviation→expansion pair.
     *
     * If an entry for [abbreviation] already exists it is overwritten (this is the
     * mechanism used for editing an existing entry).
     *
     * @throws IllegalArgumentException if [abbreviation] is blank or contains [ENTRY_SEPARATOR].
     */
    fun addEntry(abbreviation: String, expansion: String) {
        require(abbreviation.isNotBlank()) { "Abbreviation must not be blank" }
        require(ENTRY_SEPARATOR !in abbreviation) {
            "Abbreviation must not contain '$ENTRY_SEPARATOR'"
        }
        val current = prefs.textReplacement.entries.get().toMutableSet()
        // Remove any pre-existing entry for this abbreviation (handles edit/overwrite).
        current.removeAll { it.startsWith("$abbreviation$ENTRY_SEPARATOR") }
        current.add(encodeEntry(abbreviation, expansion))
        prefs.textReplacement.entries.set(current)
    }

    /**
     * Removes the entry for [abbreviation] from persistent storage.
     * No-op if the abbreviation does not exist.
     */
    fun removeEntry(abbreviation: String) {
        val current = prefs.textReplacement.entries.get().toMutableSet()
        current.removeAll { it.startsWith("$abbreviation$ENTRY_SEPARATOR") }
        prefs.textReplacement.entries.set(current)
    }

    /**
     * Checks whether the text immediately before the cursor ends with a known abbreviation
     * followed by a trigger character, and if so performs the replacement via the active
     * [android.view.inputmethod.InputConnection].
     *
     * This method is a no-op if:
     * - there is no active [InputConnection]
     * - the text before cursor is unavailable or empty
     * - the last character is not a [TRIGGER_CHARS] member
     * - no abbreviation matches the word before the trigger character
     */
    fun checkAndReplace() {
        val ic = Vim8ImeService.currentInputConnection() ?: return
        val textBeforeCursor = ic.getTextBeforeCursor(500, 0)?.toString() ?: return
        val map = parseEntries(prefs.textReplacement.entries.get())
        val (charsToDelete, replacement) = findReplacement(textBeforeCursor, map) ?: return
        ic.deleteSurroundingText(charsToDelete, 0)
        ic.commitText(replacement, 1)
    }
}

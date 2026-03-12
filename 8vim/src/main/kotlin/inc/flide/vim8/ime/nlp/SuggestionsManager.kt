package inc.flide.vim8.ime.nlp

import android.content.Context
import inc.flide.vim8.Vim8ImeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Characters that signal the end of a word and the start of the next. */
internal val WORD_BOUNDARY_CHARS = setOf(' ', '.', ',')

/**
 * Manages word suggestions for the keyboard.
 *
 * Uses a private [WordFrequencyRepository] backed by a local SQLite database.  Words gain
 * frequency each time they are typed or selected from the suggestion bar, so predictions
 * improve organically over time starting from a built-in seed list.
 *
 * All database I/O is performed on [Dispatchers.IO]; the resulting suggestions are posted back
 * to the [suggestions] [StateFlow] on the same dispatcher (Compose collects it safely on any
 * thread via `collectAsState()`).
 */
class SuggestionsManager(
    context: Context,
    private val repository: WordFrequencyRepository = WordFrequencyRepository(context)
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    /**
     * The number of characters belonging to the current partial word being typed.
     * Set to 0 in next-word prediction mode so that committing a suggestion does not
     * delete any text before the cursor.
     */
    var currentWordLength: Int = 0
        private set

    /**
     * Called whenever the text before the cursor changes.
     *
     * Two modes:
     * - **Completion mode** — the cursor is inside a word: queries the repository for prefix
     *   completions and updates [currentWordLength].
     * - **Next-word mode** — the last character is a word boundary (space, `.`, `,`): records
     *   the word that was just completed and returns the globally most-frequent words as
     *   next-word candidates.
     */
    fun onTextBeforeCursor(text: CharSequence) {
        val str = text.toString()
        if (str.isEmpty()) {
            _suggestions.value = emptyList()
            currentWordLength = 0
            return
        }

        if (str.last() in WORD_BOUNDARY_CHARS) {
            currentWordLength = 0
            val completedWord = extractWordBeforeBoundary(str)
            scope.launch {
                // Record the completed word so its frequency grows over time.
                if (completedWord.isNotEmpty()) {
                    repository.recordWord(completedWord)
                }
                _suggestions.value = repository.getTopWords(MAX_SUGGESTIONS)
            }
        } else {
            val currentWord = extractCurrentWord(str)
            currentWordLength = currentWord.length
            if (currentWord.isEmpty()) {
                _suggestions.value = emptyList()
                return
            }
            scope.launch {
                _suggestions.value = repository.getCompletions(currentWord, MAX_SUGGESTIONS)
            }
        }
    }

    /**
     * Records that [word] was explicitly selected by the user (e.g. a suggestion chip tap).
     * Delegates to the repository on the IO dispatcher.
     */
    fun recordWord(word: String) {
        scope.launch { repository.recordWord(word) }
    }

    /**
     * Clears the current suggestions list and resets the word-length counter.
     */
    fun clearSuggestions() {
        _suggestions.value = emptyList()
        currentWordLength = 0
    }

    /**
     * Commits the suggestion displayed at the given visual slot via a gesture.
     *
     * Visual slot → rank index mapping (matches [SuggestionsBar] layout):
     * - 0 = left chip  → rank 3 (`suggestions[2]`)
     * - 1 = centre chip → rank 1 (`suggestions[0]`)
     * - 2 = right chip → rank 2 (`suggestions[1]`)
     *
     * If the slot is empty or there is no active [InputConnection] the call is a no-op.
     */
    fun commitSuggestion(visualSlot: Int) {
        val rankIndex = when (visualSlot) {
            0 -> 2 // left visual → suggestions[2]
            1 -> 0 // centre visual → suggestions[0]
            2 -> 1 // right visual → suggestions[1]
            else -> return
        }
        val word = _suggestions.value.getOrNull(rankIndex) ?: return
        val ic = Vim8ImeService.currentInputConnection() ?: return
        if (currentWordLength > 0) {
            ic.deleteSurroundingText(currentWordLength, 0)
        }
        ic.commitText("$word ", 1)
        scope.launch { repository.recordWord(word) }
        clearSuggestions()
    }

    /**
     * Cancels the background coroutine scope.  Call from [android.inputmethodservice.InputMethodService.onDestroy].
     */
    fun destroy() {
        scope.cancel()
    }

    /**
     * Returns the partial word currently being typed (characters after the last whitespace).
     * Returns an empty string when [text] is empty or ends with whitespace.
     */
    internal fun extractCurrentWord(text: String): String {
        if (text.isEmpty()) return ""
        if (text.last().isWhitespace()) return ""
        val lastWhitespace = text.indexOfLast { it.isWhitespace() }
        return if (lastWhitespace == -1) text else text.substring(lastWhitespace + 1)
    }

    /**
     * Returns the last complete word that immediately precedes a word-boundary character.
     * Used in next-word prediction mode to determine which word was just finished.
     */
    internal fun extractWordBeforeBoundary(text: String): String {
        val stripped = text.trimEnd { it in WORD_BOUNDARY_CHARS }
        if (stripped.isEmpty()) return ""
        val lastBoundary = stripped.indexOfLast { it in WORD_BOUNDARY_CHARS }
        return if (lastBoundary == -1) stripped else stripped.substring(lastBoundary + 1)
    }

    companion object {
        const val MAX_SUGGESTIONS = 3
    }
}

package inc.flide.vim8.ime.nlp

import android.content.ContentValues
import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase

/**
 * Repository for the private per-app word-frequency database.
 *
 * All public methods are **blocking** and must be called from a background thread (e.g.
 * inside a coroutine launched with [kotlinx.coroutines.Dispatchers.IO]).
 *
 * Word frequency is used for two purposes:
 * - **Completion mode** ([getCompletions]): return words matching the current prefix, sorted by
 *   descending frequency so frequently-typed words float to the top.
 * - **Next-word mode** ([getTopWords]): return the most-frequent words overall as a starting
 *   point for after a word boundary; this improves naturally as the user types more.
 */
class WordFrequencyRepository(private val context: Context) {

    private val db by lazy { WordFrequencyDatabase(context) }

    /**
     * Returns up to [limit] words whose first characters match [prefix] (case-insensitive),
     * ordered by frequency descending.
     */
    fun getCompletions(prefix: String, limit: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()
        val cursor = db.readableDatabase.query(
            WF_TABLE,
            arrayOf(WF_COL_WORD),
            "$WF_COL_WORD LIKE ?",
            arrayOf("${prefix.lowercase()}%"),
            null,
            null,
            "$WF_COL_FREQ DESC",
            limit.toString()
        )
        return cursor.use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
    }

    /**
     * Returns the top [limit] most-frequently-used words globally.
     * Used for next-word prediction when no partial word is being typed yet.
     */
    fun getTopWords(limit: Int = 3): List<String> {
        val cursor = db.readableDatabase.query(
            WF_TABLE,
            arrayOf(WF_COL_WORD),
            null,
            null,
            null,
            null,
            "$WF_COL_FREQ DESC",
            limit.toString()
        )
        return cursor.use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
    }

    /**
     * Records that [word] was used. If the word already exists, its frequency is incremented
     * by one. If it is new, it is inserted with frequency 1.
     *
     * Words shorter than 2 characters and blank strings are ignored.
     */
    fun recordWord(word: String) {
        val lower = word.lowercase().trim()
        if (lower.isBlank() || lower.length < 2) return

        val wdb = db.writableDatabase
        val cv = ContentValues().apply {
            put(WF_COL_WORD, lower)
            put(WF_COL_FREQ, 1)
        }
        // Try to insert; if the word exists (CONFLICT_IGNORE skips the insert) fall through
        // to an explicit frequency increment.
        val rowId = wdb.insertWithOnConflict(WF_TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        if (rowId == -1L) {
            // Word already existed — bump its frequency.
            wdb.execSQL(
                "UPDATE $WF_TABLE SET $WF_COL_FREQ = $WF_COL_FREQ + 1 WHERE $WF_COL_WORD = ?",
                arrayOf(lower)
            )
        }
    }

    /**
     * Seeds the database from [assetManager]'s `word_seed.csv` asset if the table is still
     * empty.  The CSV format is `word,frequency` with one entry per line.
     *
     * This is idempotent: if at least one row already exists the seed is skipped.
     */
    fun seedIfNeeded(assetManager: AssetManager) {
        val wdb = db.writableDatabase
        val count = wdb
            .compileStatement("SELECT COUNT(*) FROM $WF_TABLE")
            .simpleQueryForLong()
        if (count > 0L) return

        val lines = assetManager.open("word_seed.csv").bufferedReader().readLines()
        wdb.beginTransaction()
        try {
            for (line in lines) {
                val parts = line.split(",")
                if (parts.size < 2) continue
                val w = parts[0].trim().lowercase()
                val f = parts[1].trim().toIntOrNull() ?: continue
                if (w.isBlank()) continue
                val cv = ContentValues().apply {
                    put(WF_COL_WORD, w)
                    put(WF_COL_FREQ, f)
                }
                wdb.insertWithOnConflict(WF_TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            }
            wdb.setTransactionSuccessful()
        } finally {
            wdb.endTransaction()
        }
    }
}

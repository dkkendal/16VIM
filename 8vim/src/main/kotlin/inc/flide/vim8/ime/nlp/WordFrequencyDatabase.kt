package inc.flide.vim8.ime.nlp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal const val WF_DB_NAME = "word_frequency.db"
internal const val WF_DB_VERSION = 1
internal const val WF_TABLE = "words"
internal const val WF_COL_WORD = "word"
internal const val WF_COL_FREQ = "frequency"

/**
 * SQLiteOpenHelper for the private per-app word-frequency database.
 *
 * Schema: a single [WF_TABLE] table with a [WF_COL_WORD] (PK) and [WF_COL_FREQ] integer column.
 */
internal class WordFrequencyDatabase(context: Context) : SQLiteOpenHelper(
    context,
    WF_DB_NAME,
    null,
    WF_DB_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $WF_TABLE (
                $WF_COL_WORD TEXT PRIMARY KEY,
                $WF_COL_FREQ INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $WF_TABLE")
        onCreate(db)
    }
}

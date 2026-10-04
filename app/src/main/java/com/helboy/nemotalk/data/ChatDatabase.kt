package com.helboy.nemotalk.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.helboy.nemotalk.model.ChatMessage
import com.helboy.nemotalk.model.Conversation

/**
 * Local persistence for chat sessions.
 * Stores conversations and messages in a private SQLite database.
 * ponytail: uses raw SQLiteOpenHelper — no Room dependency needed for this scale.
 */
class ChatDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "nemotalk_chats.db"
        private const val DB_VERSION = 2

        private const val TABLE_CONVOS = "conversations"
        private const val TABLE_MSGS = "messages"
        private const val TABLE_MEMORY = "long_term_memory"

        private const val C_ID = "id"
        private const val C_TITLE = "title"
        private const val C_CREATED_AT = "created_at"
        private const val C_UPDATED_AT = "updated_at"

        private const val M_ID = "id"
        private const val M_CONVO_ID = "conversation_id"
        private const val M_CONTENT = "content"
        private const val M_IS_USER = "is_user"
        private const val M_TIMESTAMP = "timestamp"
        private const val M_MODEL = "model_used"
        private const val M_IMAGE_URI = "image_uri"
        private const val M_IS_ERROR = "is_error"

        private const val MEM_ID = "id"
        private const val MEM_KIND = "kind"
        private const val MEM_CONTENT = "content"
        private const val MEM_SCORE = "score"
        private const val MEM_HITS = "hits"
        private const val MEM_CREATED_AT = "created_at"
        private const val MEM_LAST_SEEN = "last_seen"
    }

    private val db: SQLiteDatabase get() = writableDatabase

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE_CONVOS (
                $C_ID TEXT PRIMARY KEY,
                $C_TITLE TEXT NOT NULL,
                $C_CREATED_AT INTEGER NOT NULL,
                $C_UPDATED_AT INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE $TABLE_MSGS (
                $M_ID TEXT PRIMARY KEY,
                $M_CONVO_ID TEXT NOT NULL,
                $M_CONTENT TEXT NOT NULL,
                $M_IS_USER INTEGER NOT NULL,
                $M_TIMESTAMP INTEGER NOT NULL,
                $M_MODEL TEXT,
                $M_IMAGE_URI TEXT,
                $M_IS_ERROR INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY($M_CONVO_ID) REFERENCES $TABLE_CONVOS($C_ID) ON DELETE CASCADE
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_msg_convo ON $TABLE_MSGS($M_CONVO_ID)")

        // ponytail: long-term memory, no extra search dependency — SQLite FTS4 is
        // stdlib on every Android API level we support. `mem` is the unindexed
        // rowid alias FTS needs to join back to the metadata table.
        db.execSQL(
            """CREATE TABLE $TABLE_MEMORY (
                $MEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $MEM_KIND TEXT NOT NULL,
                $MEM_CONTENT TEXT NOT NULL,
                $MEM_SCORE REAL NOT NULL DEFAULT 1.0,
                $MEM_HITS INTEGER NOT NULL DEFAULT 0,
                $MEM_CREATED_AT INTEGER NOT NULL,
                $MEM_LAST_SEEN INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE VIRTUAL TABLE memory_fts USING fts4(
                content,
                content=`$TABLE_MEMORY`
            )""".trimIndent()
        )
        db.execSQL(
            "CREATE TRIGGER memory_ai AFTER INSERT ON $TABLE_MEMORY BEGIN " +
            "INSERT INTO memory_fts(rowid, content) VALUES (new.$MEM_ID, new.$MEM_CONTENT); END"
        )
        db.execSQL(
            "CREATE TRIGGER memory_ad AFTER DELETE ON $TABLE_MEMORY BEGIN " +
            "DELETE FROM memory_fts WHERE rowid = old.$MEM_ID; END"
        )
        db.execSQL(
            "CREATE TRIGGER memory_au AFTER UPDATE ON $TABLE_MEMORY BEGIN " +
            "DELETE FROM memory_fts WHERE rowid = old.$MEM_ID; " +
            "INSERT INTO memory_fts(rowid, content) VALUES (new.$MEM_ID, new.$MEM_CONTENT); END"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // v2 added long-term memory — keep all chat history, just add the tables.
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS $TABLE_MEMORY (
                    $MEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $MEM_KIND TEXT NOT NULL,
                    $MEM_CONTENT TEXT NOT NULL,
                    $MEM_SCORE REAL NOT NULL DEFAULT 1.0,
                    $MEM_HITS INTEGER NOT NULL DEFAULT 0,
                    $MEM_CREATED_AT INTEGER NOT NULL,
                    $MEM_LAST_SEEN INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS memory_fts USING fts4(content, content=`$TABLE_MEMORY`)"
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS memory_ai AFTER INSERT ON $TABLE_MEMORY BEGIN " +
                "INSERT INTO memory_fts(rowid, content) VALUES (new.$MEM_ID, new.$MEM_CONTENT); END"
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS memory_ad AFTER DELETE ON $TABLE_MEMORY BEGIN " +
                "DELETE FROM memory_fts WHERE rowid = old.$MEM_ID; END"
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS memory_au AFTER UPDATE ON $TABLE_MEMORY BEGIN " +
                "DELETE FROM memory_fts WHERE rowid = old.$MEM_ID; " +
                "INSERT INTO memory_fts(rowid, content) VALUES (new.$MEM_ID, new.$MEM_CONTENT); END"
            )
            return
        }
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MSGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONVOS")
        onCreate(db)
    }

    // ---------- Conversations ----------

    fun insertConversation(conversation: Conversation) {
        val values = ContentValues().apply {
            put(C_ID, conversation.id)
            put(C_TITLE, conversation.title)
            put(C_CREATED_AT, conversation.createdAt)
            put(C_UPDATED_AT, conversation.updatedAt)
        }
        db.insert(TABLE_CONVOS, null, values)
    }

    fun updateConversation(conversation: Conversation) {
        val values = ContentValues().apply {
            put(C_TITLE, conversation.title)
            put(C_UPDATED_AT, System.currentTimeMillis())
        }
        db.update(TABLE_CONVOS, values, "$C_ID = ?", arrayOf(conversation.id))
    }

    fun getAllConversations(): List<Conversation> {
        val result = mutableListOf<Conversation>()
        val cursor = db.query(
            TABLE_CONVOS, null, null, null, null, null,
            "$C_UPDATED_AT DESC", "50"
        )
        cursor.use {
            while (it.moveToNext()) {
                val convoId = it.getString(it.getColumnIndexOrThrow(C_ID))
                result.add(
                    Conversation(
                        id = convoId,
                        title = it.getString(it.getColumnIndexOrThrow(C_TITLE)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(C_CREATED_AT)),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow(C_UPDATED_AT)),
                        lastMessage = getLastMessageText(convoId),
                        lastMessageTime = getLastMessageTime(convoId)
                    )
                )
            }
        }
        return result
    }

    fun deleteConversation(id: String) {
        db.delete(TABLE_MSGS, "$M_CONVO_ID = ?", arrayOf(id))
        db.delete(TABLE_CONVOS, "$C_ID = ?", arrayOf(id))
    }

    private fun getLastMessageText(conversationId: String): String {
        val cursor = db.query(
            TABLE_MSGS,
            arrayOf(M_CONTENT),
            "$M_CONVO_ID = ?",
            arrayOf(conversationId),
            null, null, "$M_TIMESTAMP DESC", "1"
        )
        return cursor.use { if (it.moveToFirst()) it.getString(0) else "" }
    }

    private fun getLastMessageTime(conversationId: String): Long {
        val cursor = db.query(
            TABLE_MSGS,
            arrayOf(M_TIMESTAMP),
            "$M_CONVO_ID = ?",
            arrayOf(conversationId),
            null, null, "$M_TIMESTAMP DESC", "1"
        )
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else 0L }
    }

    // ---------- Messages ----------

    fun insertMessage(conversationId: String, message: ChatMessage) {
        val values = ContentValues().apply {
            put(M_ID, message.id)
            put(M_CONVO_ID, conversationId)
            put(M_CONTENT, message.content)
            put(M_IS_USER, if (message.isUser) 1 else 0)
            put(M_TIMESTAMP, message.timestamp)
            put(M_MODEL, message.modelUsed)
            put(M_IMAGE_URI, message.imageUri)
            put(M_IS_ERROR, if (message.isError) 1 else 0)
        }
        db.insert(TABLE_MSGS, null, values)
        touchConversation(conversationId)
    }
    fun getMessagesForConversation(conversationId: String): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        val cursor = db.query(
            TABLE_MSGS, null, "$M_CONVO_ID = ?", arrayOf(conversationId),
            null, null, "$M_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    ChatMessage(
                        id = it.getString(it.getColumnIndexOrThrow(M_ID)),
                        content = it.getString(it.getColumnIndexOrThrow(M_CONTENT)),
                        isUser = it.getInt(it.getColumnIndexOrThrow(M_IS_USER)) == 1,
                        timestamp = it.getLong(it.getColumnIndexOrThrow(M_TIMESTAMP)),
                        modelUsed = it.getString(it.getColumnIndexOrThrow(M_MODEL)),
                        imageUri = it.getString(it.getColumnIndexOrThrow(M_IMAGE_URI)),
                        isError = it.getInt(it.getColumnIndexOrThrow(M_IS_ERROR)) == 1
                    )
                )
            }
        }
        return result
    }

    fun clearConversationMessages(conversationId: String) {
        db.delete(TABLE_MSGS, "$M_CONVO_ID = ?", arrayOf(conversationId))
        touchConversation(conversationId)
    }

    private fun touchConversation(id: String) {
        val values = ContentValues().apply { put(C_UPDATED_AT, System.currentTimeMillis()) }
        db.update(TABLE_CONVOS, values, "$C_ID = ?", arrayOf(id))
    }

    // ---------- Long-term memory ----------
    // ponytail: retrieval is the whole point of "scale" here — SQLite FTS4
    // scores by rank, then we decay by recency, so the model gets the facts it
    // actually needs without an embedding model or a vector store.

    fun insertMemory(content: String, kind: String) {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(MEM_KIND, kind)
            put(MEM_CONTENT, content)
            put(MEM_SCORE, 1.0)
            put(MEM_HITS, 0)
            put(MEM_CREATED_AT, now)
            put(MEM_LAST_SEEN, now)
        }
        db.insert(TABLE_MEMORY, null, values)
    }

    /**
     * Free-text recall. Returns facts and preferences whose wording overlaps
     * [query], ordered by FTS rank with a recency nudge so recent context wins.
     */
    fun recall(query: String, limit: Int = 8): List<String> {
        if (query.isBlank()) return emptyList()
        val sanitized = query.trim().replace("\"", " ")
        if (sanitized.isBlank()) return emptyList()

        val out = mutableListOf<String>()
        val cursor = db.rawQuery(
            """SELECT m.$MEM_CONTENT
                 FROM memory_fts f
                 JOIN $TABLE_MEMORY m ON m.$MEM_ID = f.rowid
                WHERE memory_fts MATCH ?
                ORDER BY (rank + (m.$MEM_LAST_SEEN / 1000000.0)) ASC
                LIMIT ?""".trimIndent(),
            arrayOf(sanitized, limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) out.add(it.getString(0))
        }
        return out
    }

    /** Every memory, newest first — used by the memory panel. */
    fun allMemory(): List<String> {
        val out = mutableListOf<String>()
        val cursor = db.query(
            TABLE_MEMORY, arrayOf(MEM_CONTENT), null, null,
            null, null, "$MEM_LAST_SEEN DESC", "60"
        )
        cursor.use { while (it.moveToNext()) out.add(it.getString(0)) }
        return out
    }

    fun clearMemory() = db.delete(TABLE_MEMORY, null, null)
}

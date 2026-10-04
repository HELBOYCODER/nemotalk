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
        private const val DB_VERSION = 1

        private const val TABLE_CONVOS = "conversations"
        private const val TABLE_MSGS = "messages"

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
}

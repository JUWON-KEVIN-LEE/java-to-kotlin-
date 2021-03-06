package com.udacity.notepad.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns._ID
import com.udacity.notepad.data.NotesContract.NoteTable.CREATED_AT
import com.udacity.notepad.data.NotesContract.NoteTable.IS_PINNED
import com.udacity.notepad.data.NotesContract.NoteTable.TEXT
import com.udacity.notepad.data.NotesContract.NoteTable.UPDATED_AT
import com.udacity.notepad.data.NotesContract.NoteTable._TABLE_NAME
import org.jetbrains.anko.db.transaction
import java.util.*

class NoteDatabase(context: Context) {

    private val helper: NotesOpenHelper

    fun getAll(): List<Note> {
        val cursor = helper.readableDatabase.query(_TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                CREATED_AT)
        return cursor.use(this::allFromCursor) // use 사용 시 자동으로 닫아준다
    }

    init {
        helper = NotesOpenHelper(context)
    }


    fun loadAllByIds(vararg ids: Int): List<Note> {
        val questionMarks = ids.map { "?" }.joinToString { ", " }
        val args = ids.map { it.toString() }
        val selection = "$_ID IN ($questionMarks)"
        val cursor = helper.readableDatabase.query(_TABLE_NAME,
                null,
                selection,
                args.toTypedArray(),
                null,
                null,
                CREATED_AT)

        return cursor.use(this::allFromCursor)
    }

    fun insert(vararg notes: Note) {
        val values = fromNotes(notes)
        val db = helper.writableDatabase
        // anko
        db.transaction {
            for (value in values) {
                insert(_TABLE_NAME, null, value)
            }
        }
    }

    fun update(note: Note) {
        val values = fromNote(note)
        helper.writableDatabase.update(_TABLE_NAME,
                values,
                _ID + " = ?",
                arrayOf(Integer.toString(note.id)))
    }

    fun delete(note: Note) {
        helper.writableDatabase.delete(_TABLE_NAME,
                _ID + " = ?",
                arrayOf(Integer.toString(note.id)))
    }

    private fun fromCursor(cursor: Cursor): Note {
        var col = 0
        return Note().apply {
            id = cursor.getInt(col++)
            text = cursor.getString(col++)
            isPinned = cursor.getInt(col++) != 0
            createdAt = Date(cursor.getLong(col++))
            updatedAt = Date(cursor.getLong(col))
        }
    }

    private fun allFromCursor(cursor: Cursor): List<Note> {
        val retval = ArrayList<Note>()
        while (cursor.moveToNext()) {
            retval.add(fromCursor(cursor))
        }
        return retval
    }

    private fun fromNote(note: Note): ContentValues {
        return ContentValues().apply {
            val id = note.id
            if (id != -1) {
                put(_ID, id)
            }
            put(TEXT, note.text)
            put(IS_PINNED, note.isPinned)
            put(CREATED_AT, note.createdAt.time)
            put(UPDATED_AT, note.updatedAt!!.time)
        }
    }

    // [ Generic ]
    // Java 의 wildcard 기능 == kotlin 의 in/out
    // ? extends T == out ; read only
    // ? super T == in ; write only
    // 감사하게도 잘 정리된 포스트를 참고할 수 있었다.
    // [출처] http://thdev.tech/kotlin/androiddev/2017/10/03/Kotlin-Generics.html
    private fun fromNotes(notes: Array<out Note>): List<ContentValues> {
        val values = ArrayList<ContentValues>()
        for (note in notes) {
            values.add(fromNote(note))
        }
        return values
    }
}

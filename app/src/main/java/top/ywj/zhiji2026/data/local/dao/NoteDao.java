package top.ywj.zhiji2026.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import top.ywj.zhiji2026.data.model.Note;

@Dao
public interface NoteDao {

    @Insert
    void insert(Note note);

    @Query("SELECT * FROM notes WHERE lectureId = :lectureId ORDER BY createdAt DESC")
    List<Note> getByLectureId(long lectureId);

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    List<Note> getAll();
}

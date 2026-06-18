package top.ywj.zhiji2026.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import top.ywj.zhiji2026.data.model.Lecture;

@Dao
public interface LectureDao {

    @Query("SELECT * FROM lectures WHERE courseId = :courseId ORDER BY createdAt DESC")
    LiveData<List<Lecture>> getByCourseId(String courseId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Lecture lecture);

    @Query("UPDATE lectures SET status = :status WHERE id = :id")
    void updateStatus(String id, String status);

    @Query("UPDATE lectures SET transcript=:transcript, status=:status WHERE id=:id")
    void saveTranscript(String id, String transcript, String status);

    @Query("SELECT * FROM lectures WHERE id = :id")
    Lecture getById(String id);
    @Update
    void update(Lecture lecture);
    @Query("UPDATE lectures SET status=:status, audioPath=:audioPath, transcript=:transcript, markdown=:markdown, photoCount=:photoCount WHERE id=:id")
    void finishLecture(String id, String status, String audioPath, String transcript, String markdown, int photoCount);
    @Delete
    void delete(Lecture lecture);
    @Query("SELECT * FROM lectures WHERE courseId = :courseId")
    List<Lecture> getByCourseIdSync(String courseId);
    @Query("DELETE FROM lectures WHERE courseId = :courseId")
    void deleteLecturesByCourseId(String courseId);


}

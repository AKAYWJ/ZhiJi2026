package top.ywj.zhiji2026.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import top.ywj.zhiji2026.data.model.Course;

@Dao
public interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY createdAt DESC")
    LiveData<List<Course>> getAllCourses();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Course course);

    @Query("SELECT * FROM courses WHERE id = :id")
    Course getById(String id);

    @Delete
    void delete(Course course);

    @Query("DELETE FROM lectures WHERE courseId = :courseId")
    void deleteLecturesByCourseId(String courseId);

}

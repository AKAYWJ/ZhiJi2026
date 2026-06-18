package top.ywj.zhiji2026.data.repository;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import top.ywj.zhiji2026.data.local.AppDatabase;
import top.ywj.zhiji2026.data.model.Course;

public class CourseRepository {

    private final AppDatabase db;

    public CourseRepository(AppDatabase db) {
        this.db = db;
    }

    public LiveData<List<Course>> getAllCourses() {
        return db.courseDao().getAllCourses();
    }

    public Completable createCourse(String name, String semester) {
        Course course = new Course();
        course.setId(UUID.randomUUID().toString());
        course.setName(name);
        course.setSemester(semester);
        course.setCreatedAt(System.currentTimeMillis());
        return Completable.fromAction(() -> db.courseDao().insert(course))
                .subscribeOn(Schedulers.io());
    }
}

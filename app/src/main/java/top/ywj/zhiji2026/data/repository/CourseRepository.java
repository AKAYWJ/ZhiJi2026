package top.ywj.zhiji2026.data.repository;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import top.ywj.zhiji2026.data.local.AppDatabase;
import top.ywj.zhiji2026.data.model.Course;
import top.ywj.zhiji2026.data.model.Lecture;

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
    private void deleteDir(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteDir(child);
            }
        }
        dir.delete();
    }


    public Completable deleteCourse(Course course) {
        return Completable.fromAction(() -> {
            // 删所有讲座关联的文件
            List<Lecture> lectures = db.lectureDao().getByCourseIdSync(course.getId());
            for (Lecture l : lectures) {
                String audioPath = l.getAudioPath();
                if (audioPath != null && !audioPath.isEmpty()) {
                    File audioFile = new File(audioPath);
                    File parentDir = audioFile.getParentFile();
                    if (parentDir != null && parentDir.exists()) {
                        if ("audio".equals(parentDir.getName())) {
                            audioFile.delete();
                        } else {
                            deleteDir(parentDir);
                        }
                    }
                }
            }
            // 删讲座记录
            db.lectureDao().deleteLecturesByCourseId(course.getId());
            // 删课程记录
            db.courseDao().delete(course);
        }).subscribeOn(Schedulers.io());
    }

}

package top.ywj.zhiji2026.ui.course;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import top.ywj.zhiji2026.data.model.Lecture;
import top.ywj.zhiji2026.data.repository.CourseRepository;
import top.ywj.zhiji2026.data.repository.LectureRepository;

public class CourseDetailViewModel extends ViewModel {

    private final LectureRepository lectureRepository;
    private final String courseId;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public CourseDetailViewModel(CourseRepository courseRepo, LectureRepository lectureRepo, String courseId) {
        this.lectureRepository = lectureRepo;
        this.courseId = courseId;
    }

    public LiveData<List<Lecture>> getLectures() {
        return lectureRepository.getLecturesByCourse(courseId);
    }

    public void deleteLecture(Lecture lecture) {
        disposables.add(
                lectureRepository.deleteLecture(lecture)
                        .subscribeOn(Schedulers.io())
                        .subscribe()
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final CourseRepository courseRepo;
        private final LectureRepository lectureRepo;
        private final String courseId;

        public Factory(CourseRepository courseRepo, LectureRepository lectureRepo, String courseId) {
            this.courseRepo = courseRepo;
            this.lectureRepo = lectureRepo;
            this.courseId = courseId;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new CourseDetailViewModel(courseRepo, lectureRepo, courseId);
        }
    }
}

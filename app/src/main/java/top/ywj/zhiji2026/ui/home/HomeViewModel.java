package top.ywj.zhiji2026.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import top.ywj.zhiji2026.data.model.Course;
import top.ywj.zhiji2026.data.repository.CourseRepository;

public class HomeViewModel extends ViewModel {

    private final CourseRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public HomeViewModel(CourseRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Course>> getCourses() {
        return repository.getAllCourses();
    }

    public void createCourse(String name, String semester) {
        disposables.add(
                repository.createCourse(name, semester)
                        .subscribe(() -> {}, Throwable::printStackTrace)
        );
    }
    public void deleteCourse(Course course) {
        disposables.add(
                repository.deleteCourse(course)
                        .subscribe(() -> {}, Throwable::printStackTrace)
        );
    }



    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final CourseRepository repository;

        public Factory(CourseRepository repository) {
            this.repository = repository;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new HomeViewModel(repository);
        }


    }
}

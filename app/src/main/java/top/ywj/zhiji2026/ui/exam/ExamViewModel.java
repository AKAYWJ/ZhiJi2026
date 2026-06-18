package top.ywj.zhiji2026.ui.exam;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import top.ywj.zhiji2026.data.repository.NoteRepository;

public class ExamViewModel extends ViewModel {

    private final NoteRepository noteRepo;
    private final String courseId;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final MutableLiveData<String> reviewHtml = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ExamViewModel(NoteRepository noteRepo, String courseId) {
        this.noteRepo = noteRepo;
        this.courseId = courseId;
    }

    public LiveData<String> getReviewHtml() {
        return reviewHtml;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void generateReview(String scope, String focus) {
        disposables.add(
                noteRepo.getAllNotes()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(notes -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append("<html><head><style>")
                                    .append("body{font-family:sans-serif;padding:16px;line-height:1.8;color:#333}")
                                    .append("h2{color:#1976D2}")
                                    .append("h3{color:#FF5722}")
                                    .append("</style></head><body>");
                            sb.append("<h2>复习提纲</h2>");
                            if (courseId != null) {
                                sb.append("<p><b>课程ID：</b>").append(courseId).append("</p>");
                            }
                            sb.append("<p><b>考试范围：</b>").append(scope).append("</p>");
                            if (!focus.isEmpty()) {
                                sb.append("<p><b>重点关注：</b>").append(focus).append("</p>");
                            }
                            sb.append("<hr>");
                            sb.append("<h3>笔记内容</h3>");
                            if (notes.isEmpty()) {
                                sb.append("<p>暂无笔记，请先录制课程。</p>");
                            } else {
                                for (int i = 0; i < notes.size(); i++) {
                                    sb.append("<p>").append(i + 1).append(". ")
                                            .append(notes.get(i).getContent()).append("</p>");
                                }
                            }
                            sb.append("</body></html>");
                            reviewHtml.postValue(sb.toString());
                        }, throwable -> {
                            error.postValue("生成失败：" + throwable.getMessage());
                        })
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final NoteRepository noteRepo;
        private final String courseId;

        public Factory(NoteRepository noteRepo, String courseId) {
            this.noteRepo = noteRepo;
            this.courseId = courseId;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new ExamViewModel(noteRepo, courseId);
        }
    }
}

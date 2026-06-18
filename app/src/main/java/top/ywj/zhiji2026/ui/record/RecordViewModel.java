package top.ywj.zhiji2026.ui.record;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import top.ywj.zhiji2026.data.model.Lecture;
import top.ywj.zhiji2026.data.remote.RetrofitClient;
import top.ywj.zhiji2026.data.repository.CourseRepository;
import top.ywj.zhiji2026.data.repository.LectureRepository;

public class RecordViewModel extends ViewModel {

    private final LectureRepository lectureRepo;
    private final String lectureId;
    private final String courseId;
    private final String courseName;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final CompositeDisposable uploadDisposables = new CompositeDisposable();

    private String audioPath;
    private int photoCount;
    private String currentLectureId;
    private final RetrofitClient api;

    public RecordViewModel(LectureRepository lectureRepo, CourseRepository courseRepo,
                           String lectureId, String courseId, String courseName, RetrofitClient api) {
        this.lectureRepo = lectureRepo;
        this.courseId = courseId;
        this.lectureId = lectureId;
        this.courseName = courseName;
        this.api = RetrofitClient.getInstance();

        if (lectureId == null) {
            createNewLecture();
        } else {
            this.currentLectureId = lectureId;
        }
    }

    private void createNewLecture() {
        currentLectureId = UUID.randomUUID().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

        Lecture lecture = new Lecture();
        lecture.setId(currentLectureId);
        lecture.setCourseId(courseId);
        lecture.setTitle((courseName != null ? courseName : "未命名课程") + " " + sdf.format(new Date()));
        lecture.setStatus("recording");

        disposables.add(
                lectureRepo.insertLecture(lecture)
                        .subscribeOn(Schedulers.io())
                        .subscribe()
        );
    }

    public String getCurrentLectureId() {
        return currentLectureId;
    }

    public void addPhoto(String photoPath) {
        photoCount++;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public void finishRecording() {
        if (currentLectureId == null) return;
        disposables.add(
                // finishRecording: "recording", audioPath, photoCount
                lectureRepo.finishLecture(currentLectureId, "recording",
                                audioPath != null ? audioPath : "",
                                "", "",
                                photoCount)

                        .subscribeOn(Schedulers.io())
                        .subscribe(() -> {
                            android.util.Log.d("RecordVM", "finish success: " + currentLectureId);
                        }, throwable -> {
                            android.util.Log.e("RecordVM", "finish failed", throwable);
                        })
        );
    }
    public void startAsr(String audioPath, String lectureTitle) {
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) return;

        this.audioPath = audioPath;

        // 立即更新状态为 "asr"，让列表立即显示"解析中"
        uploadDisposables.add(
                lectureRepo.finishLecture(currentLectureId, "asr", audioPath, "", "", photoCount)
                        .subscribeOn(Schedulers.io())
                        .subscribe()
        );

        RequestBody audioBody = RequestBody.create(audioFile, MediaType.parse("audio/mp4"));
        MultipartBody.Part audioPart = MultipartBody.Part.createFormData(
                "audio_file", audioFile.getName(), audioBody);

        uploadDisposables.add(
                api.getApi().asr(audioPart)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                response -> {
                                    String transcript = response.getTranscript();
                                    lectureRepo.saveTranscript(currentLectureId, transcript, "asr")
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(() -> {
                                                generateNote(transcript, lectureTitle);
                                            });
                                },
                                error -> {
                                    lectureRepo.finishLecture(currentLectureId, "error",
                                                    this.audioPath, "", "", photoCount)
                                            .subscribeOn(Schedulers.io())
                                            .subscribe();
                                }
                        )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final LectureRepository lectureRepo;
        private final CourseRepository courseRepo;
        private final String lectureId;
        private final String courseId;
        private final String courseName;

        public Factory(LectureRepository lectureRepo, CourseRepository courseRepo,
                       String lectureId, String courseId, String courseName) {
            this.lectureRepo = lectureRepo;
            this.courseRepo = courseRepo;
            this.lectureId = lectureId;
            this.courseId = courseId;
            this.courseName = courseName;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new RecordViewModel(lectureRepo, courseRepo, lectureId, courseId, courseName,
                    RetrofitClient.getInstance());
        }
    }
    public void generateNote(String transcript, String lectureTitle) {
        lectureRepo.saveTranscript(currentLectureId, transcript, "note_generating")
                .subscribeOn(Schedulers.io())
                .subscribe();

        Map<String, String> body = new HashMap<>();
        body.put("content", transcript);
        body.put("lecture_title", lectureTitle);

        uploadDisposables.add(
                api.getApi().generateNote(body)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                response -> {
                                    lectureRepo.finishLecture(currentLectureId, "done",
                                                    audioPath, transcript,
                                                    response.getMarkdown(), photoCount)
                                            .subscribeOn(Schedulers.io())
                                            .subscribe();
                                },
                                error -> {
                                    lectureRepo.finishLecture(currentLectureId, "error",
                                                    audioPath, transcript, "", photoCount)
                                            .subscribeOn(Schedulers.io())
                                            .subscribe();
                                }
                        )
        );
    }


}

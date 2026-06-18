package top.ywj.zhiji2026.data.repository;

import androidx.lifecycle.LiveData;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import top.ywj.zhiji2026.data.local.AppDatabase;
import top.ywj.zhiji2026.data.model.Lecture;
import top.ywj.zhiji2026.data.remote.RetrofitClient;
import top.ywj.zhiji2026.data.remote.dto.LectureUploadResponse;
import top.ywj.zhiji2026.data.remote.dto.StatusResponse;

public class LectureRepository {

    private final AppDatabase db;
    private final RetrofitClient api;

    public LectureRepository(AppDatabase db) {
        this.db = db;
        this.api = RetrofitClient.getInstance();
    }

    public LiveData<List<Lecture>> getLecturesByCourse(String courseId) {
        return db.lectureDao().getByCourseId(courseId);
    }
    public Completable insertLecture(Lecture lecture) {
        return Completable.fromAction(() -> {
            db.lectureDao().insert(lecture);
        }).subscribeOn(Schedulers.io());
    }


    public Single<LectureUploadResponse> uploadLecture(
            String courseId, List<String> photoPaths,
            String audioPath, List<Integer> marks) {

        RequestBody courseIdBody = RequestBody.create(courseId, MediaType.parse("text/plain"));
        RequestBody marksBody = RequestBody.create(
                new Gson().toJson(marks), MediaType.parse("application/json"));

        List<MultipartBody.Part> photoParts = new ArrayList<>();
        for (int i = 0; i < photoPaths.size(); i++) {
            File file = new File(photoPaths.get(i));
            RequestBody photoBody = RequestBody.create(file, MediaType.parse("image/jpeg"));
            photoParts.add(MultipartBody.Part.createFormData(
                    "photos", "photo_" + i + ".jpg", photoBody));
        }

        File audioFile = new File(audioPath);
        RequestBody audioBody = RequestBody.create(audioFile, MediaType.parse("audio/mp4"));
        MultipartBody.Part audioPart = MultipartBody.Part.createFormData(
                "audio", "lecture.m4a", audioBody);

        return api.getApi().uploadLecture(courseIdBody, marksBody, photoParts, audioPart)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<StatusResponse> getStatus(String lectureId) {
        return api.getApi().getLectureStatus(lectureId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    public Completable updateLecture(Lecture lecture) {
        return Completable.fromAction(() -> {
            db.lectureDao().update(lecture);
        }).subscribeOn(Schedulers.io());
    }
    public Completable finishLecture(String lectureId, String status, String audioPath,
                                     String transcript, String markdown, int photoCount) {
        return Completable.fromAction(() ->
                db.lectureDao().finishLecture(lectureId, status, audioPath, transcript, markdown, photoCount)
        ).subscribeOn(Schedulers.io());
    }

    public Completable saveTranscript(String lectureId, String transcript, String status) {
        return Completable.fromAction(() ->
                db.lectureDao().saveTranscript(lectureId, transcript, status)
        ).subscribeOn(Schedulers.io());
    }


    public Completable deleteLecture(Lecture lecture) {
        return Completable.fromAction(() -> db.lectureDao().delete(lecture))
                .subscribeOn(Schedulers.io());
    }

    // LectureRepository.java，deleteLecture 上方加
    public Single<Lecture> getLectureById(String id) {
        return Single.fromCallable(() -> db.lectureDao().getById(id))
                .subscribeOn(Schedulers.io());
    }



}

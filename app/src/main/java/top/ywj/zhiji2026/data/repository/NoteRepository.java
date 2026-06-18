package top.ywj.zhiji2026.data.repository;

import android.util.Log;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import top.ywj.zhiji2026.data.local.AppDatabase;
import top.ywj.zhiji2026.data.model.Note;

public class NoteRepository {

    private final AppDatabase database;

    public NoteRepository(AppDatabase database) {
        this.database = database;
    }

    public Completable insertNote(Note note) {
        return Completable.fromAction(() -> {
            database.noteDao().insert(note);
        }).subscribeOn(Schedulers.io());
    }

    public Single<List<Note>> getNotesByLecture(long lectureId) {
        return Single.create(emitter -> {
            try {
                List<Note> notes = database.noteDao().getByLectureId(lectureId);
                emitter.onSuccess(notes);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    public Single<List<Note>> getAllNotes() {
        return Single.create(emitter -> {
            try {
                List<Note> notes = database.noteDao().getAll();
                emitter.onSuccess(notes);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}

package top.ywj.zhiji2026.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import top.ywj.zhiji2026.data.local.dao.CourseDao;
import top.ywj.zhiji2026.data.local.dao.LectureDao;
import top.ywj.zhiji2026.data.local.dao.NoteDao;
import top.ywj.zhiji2026.data.model.Course;
import top.ywj.zhiji2026.data.model.Lecture;
import top.ywj.zhiji2026.data.model.Note;
import top.ywj.zhiji2026.data.model.PhotoItem;

@Database(
        entities = {Course.class, Lecture.class, Note.class, PhotoItem.class},
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CourseDao courseDao();
    public abstract LectureDao lectureDao();
    public abstract NoteDao noteDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "zhiji.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}

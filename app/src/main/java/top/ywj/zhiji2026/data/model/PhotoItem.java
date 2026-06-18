package top.ywj.zhiji2026.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "photo_items",
        foreignKeys = @ForeignKey(entity = Lecture.class,
                parentColumns = "id",
                childColumns = "lectureId",
                onDelete = ForeignKey.CASCADE))
public class PhotoItem {

    @PrimaryKey
    @NonNull
    private String id;
    @ColumnInfo(index = true)
    private long lectureId;

    private String filePath;
    private long timestamp;

    public PhotoItem() {}

    @Ignore
    public PhotoItem(@NonNull String id, long lectureId, String filePath, long timestamp) {
        this.id = id;
        this.lectureId = lectureId;
        this.filePath = filePath;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public long getLectureId() { return lectureId; }
    public void setLectureId(long lectureId) { this.lectureId = lectureId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

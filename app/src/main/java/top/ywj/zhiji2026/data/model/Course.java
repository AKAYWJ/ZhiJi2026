package top.ywj.zhiji2026.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "courses")
public class Course {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String semester;
    private long createdAt;

    public Course() {}

    @Ignore
    public Course(@NonNull String id, String name, String semester, long createdAt) {
        this.id = id;
        this.name = name;
        this.semester = semester;
        this.createdAt = createdAt;
    }


    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

package top.ywj.zhiji2026.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "lectures")
public class Lecture {

    @PrimaryKey
    @NonNull
    private String id = UUID.randomUUID().toString();

    private String courseId;
    private String title;
    private String audioPath;
    private int photoCount;
    private String transcript;
    private String markdown;
    private String mindmap;
    private String graphData;

    private String status;
    private long createdAt;

    public Lecture() {}

    @Ignore
    public Lecture(@NonNull String id, String courseId, String title,
                   String audioPath, int photoCount, String status,
                   String transcript, String markdown, long createdAt) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.audioPath = audioPath;
        this.photoCount = photoCount;
        this.status = status;
        this.transcript = transcript;
        this.markdown = markdown;
        this.mindmap = null;
        this.graphData = null;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public int getPhotoCount() { return photoCount; }
    public void setPhotoCount(int photoCount) { this.photoCount = photoCount; }

    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }

    public String getMarkdown() { return markdown; }
    public void setMarkdown(String markdown) { this.markdown = markdown; }

    public String getMindmap() { return mindmap; }
    public void setMindmap(String mindmap) { this.mindmap = mindmap; }

    public String getGraphData() { return graphData; }
    public void setGraphData(String graphData) { this.graphData = graphData; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

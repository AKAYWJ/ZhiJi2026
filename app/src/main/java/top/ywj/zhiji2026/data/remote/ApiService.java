package top.ywj.zhiji2026.data.remote;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import top.ywj.zhiji2026.data.model.Course;
import top.ywj.zhiji2026.data.remote.dto.AsrResponse;
import top.ywj.zhiji2026.data.remote.dto.KnowledgeGraphResponse;
import top.ywj.zhiji2026.data.remote.dto.LectureUploadResponse;
import top.ywj.zhiji2026.data.remote.dto.MindMapResponse;
import top.ywj.zhiji2026.data.remote.dto.NoteFromAudioResponse;
import top.ywj.zhiji2026.data.remote.dto.NoteGenerateResponse;
import top.ywj.zhiji2026.data.remote.dto.NoteResponse;
import top.ywj.zhiji2026.data.remote.dto.StatusResponse;

public interface ApiService {

    @POST("api/courses")
    Single<Course> createCourse(@Body Map<String, String> body);

    @GET("api/courses")
    Single<List<Course>> getCourses();

    @Multipart
    @POST("api/lectures/upload")
    Single<LectureUploadResponse> uploadLecture(
            @Part("course_id") RequestBody courseId,
            @Part("marks") RequestBody marks,
            @Part List<MultipartBody.Part> photos,
            @Part MultipartBody.Part audio
    );

    @GET("api/lectures/{id}/status")
    Single<StatusResponse> getLectureStatus(@Path("id") String lectureId);

    @GET("api/lectures/{id}/note")
    Single<NoteResponse> getNote(@Path("id") String lectureId);

    @POST("api/lectures/{id}/exam-prep")
    Single<NoteResponse> examPrep(@Path("id") String lectureId, @Body Map<String, Object> body);

    @Multipart
    @POST("note/from-audio")
    Single<NoteFromAudioResponse> noteFromAudio(
            @Part("lecture_title") RequestBody lectureTitle,
            @Part MultipartBody.Part audio
    );
    @POST("mindmap")
    Single<MindMapResponse> generateMindMap(@Body Map<String, String> body);

    @Multipart
    @POST("asr")
    Single<AsrResponse> asr(@Part MultipartBody.Part audio_file);

    @POST("note/generate")
    Single<NoteGenerateResponse> generateNote(@Body Map<String, String> body);

    @POST("knowledge-graph")
    Single<KnowledgeGraphResponse> generateKnowledgeGraph(@Body Map<String, String> body);

}

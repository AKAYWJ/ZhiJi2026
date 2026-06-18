package top.ywj.zhiji2026.ui.course;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import top.ywj.zhiji2026.R;
import top.ywj.zhiji2026.ZhiJiApplication;
import top.ywj.zhiji2026.data.model.Lecture;
import top.ywj.zhiji2026.data.remote.RetrofitClient;
import top.ywj.zhiji2026.data.repository.CourseRepository;
import top.ywj.zhiji2026.data.repository.LectureRepository;
import top.ywj.zhiji2026.ui.exam.ExamActivity;
import top.ywj.zhiji2026.ui.note.NoteActivity;
import top.ywj.zhiji2026.ui.record.RecordActivity;
import top.ywj.zhiji2026.util.FileUtil;

public class CourseDetailActivity extends AppCompatActivity {

    private CourseDetailViewModel viewModel;
    private LectureAdapter adapter;
    private String courseId;
    private  String courseName;

    private final ActivityResultLauncher<String[]> pickAudio =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    uploadAudioFile(uri);
                }
            });
    private LectureRepository lectureRepo;
    private AlertDialog loadingDialog;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        courseId = getIntent().getStringExtra("course_id");
        courseName = getIntent().getStringExtra("course_name");
        if (courseId == null) {
            finish();
            return;
        }

        CourseRepository courseRepo = new CourseRepository(ZhiJiApplication.getDatabase());
        LectureRepository lectureRepo = new LectureRepository(ZhiJiApplication.getDatabase());
        this.lectureRepo = lectureRepo;


        viewModel = new ViewModelProvider(this,
                new CourseDetailViewModel.Factory(courseRepo, lectureRepo, courseId))
                .get(CourseDetailViewModel.class);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(courseName);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        setupFab();
        observeData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.course_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_exam) {
            Intent intent = new Intent(this, ExamActivity.class);
            intent.putExtra("course_id", getIntent().getStringExtra("course_id"));
            intent.putExtra("course_name", getIntent().getStringExtra("course_name"));
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rv_lectures);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LectureAdapter(lecture -> {
            if ("done".equals(lecture.getStatus())) {
                Intent intent = new Intent(this, NoteActivity.class);
                intent.putExtra("lecture_id", lecture.getId());
                intent.putExtra("lecture_title", lecture.getTitle());
                startActivity(intent);
            } else if ("asr".equals(lecture.getStatus()) || "note_generating".equals(lecture.getStatus())) {
                Toast.makeText(this, "正在处理中，请稍后查看", Toast.LENGTH_SHORT).show();
            } else if ("error".equals(lecture.getStatus())) {
                retryUpload(lecture);
            } else {

                Intent intent = new Intent(this, RecordActivity.class);
                intent.putExtra("lecture_id", lecture.getId());
                intent.putExtra("lecture_title", lecture.getTitle());
                startActivity(intent);
            }
        });

        rv.setAdapter(adapter);

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Lecture lecture = adapter.getCurrentList().get(pos);
                adapter.notifyItemChanged(pos); // 恢复显示

                new AlertDialog.Builder(CourseDetailActivity.this)
                        .setTitle("删除讲座")
                        .setMessage("确定删除「" + lecture.getTitle() + "」吗？")
                        .setNegativeButton("取消", (dialog, which) -> adapter.notifyItemChanged(pos))
                        .setPositiveButton("删除", (dialog, which) -> viewModel.deleteLecture(lecture))
                        .show();
            }

        });
        helper.attachToRecyclerView(rv);
    }


    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab_record);
        fab.setOnClickListener(v -> showRecordOptions());
    }

    private void observeData() {
        viewModel.getLectures().observe(this, lectures -> {
            if (lectures != null) {
                adapter.submitList(lectures);
            }
        });
    }
    private void showRecordOptions() {
        new AlertDialog.Builder(this)
                .setTitle("添加录音")
                .setItems(new String[]{"开始录音", "上传音频文件"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(CourseDetailActivity.this, RecordActivity.class);
                        intent.putExtra("course_id", courseId);
                        intent.putExtra("course_name", courseName);
                        startActivity(intent);
                    } else {
                        pickAudio.launch(new String[]{"audio/*", "video/mp4"});
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void uploadAudioFile(Uri uri) {
        Toast.makeText(this, "已提交解析", Toast.LENGTH_SHORT).show();

        try {
            String safeName = (courseName != null ? courseName : "未命名课程").replaceAll("[\\\\/:*?\"<>|]", "_");
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            String folderName = safeName + "_" + ts;
            File lectureDir = FileUtil.getLectureDir(this, folderName);
            File savedAudio = new File(lectureDir, "audio.m4a");

            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    throw new IOException("无法读取选中文件");
                }
                try (OutputStream out = new FileOutputStream(savedAudio)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }

            android.util.Log.d("CourseDetail", "音频文件已拷贝: " + savedAudio.getAbsolutePath()
                    + " (" + savedAudio.length() + " bytes)");
            if (savedAudio.length() == 0) {
                throw new IOException("拷贝后文件为空");
            }

            String lectureId = java.util.UUID.randomUUID().toString();
            String title = (courseName != null ? courseName : "未命名课程") + " " + ts;
            final String localAudioPath = savedAudio.getAbsolutePath();

            Lecture lecture = new Lecture();
            lecture.setId(lectureId);
            lecture.setCourseId(courseId);
            lecture.setTitle(title);
            lecture.setAudioPath(localAudioPath);
            lecture.setStatus("asr");
            lectureRepo.insertLecture(lecture)
                    .subscribeOn(Schedulers.io())
                    .subscribe();

            RequestBody audioBody = RequestBody.create(savedAudio, MediaType.parse("audio/mp4"));
            MultipartBody.Part audioPart = MultipartBody.Part.createFormData(
                    "audio_file", savedAudio.getName(), audioBody);

            android.util.Log.d("CourseDetail", "开始 ASR 请求... file=" + savedAudio.getName());

            RetrofitClient.getInstance().getApi()
                    .asr(audioPart)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            asrResponse -> {
                                String transcript = asrResponse.getTranscript();
                                android.util.Log.d("CourseDetail", "ASR 完成, 文本长度=" + transcript.length());

                                lectureRepo.saveTranscript(lectureId, transcript, "note_generating")
                                        .subscribeOn(Schedulers.io())
                                        .subscribe();

                                // Step 2: generate note
                                Map<String, String> body = new HashMap<>();
                                body.put("content", transcript);
                                body.put("lecture_title", title);

                                RetrofitClient.getInstance().getApi()
                                        .generateNote(body)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(
                                                noteResponse -> {
                                                    lectureRepo.finishLecture(lectureId, "done",
                                                                    localAudioPath, transcript,
                                                                    noteResponse.getMarkdown(), 0)
                                                            .subscribeOn(Schedulers.io())
                                                            .subscribe();
                                                    Toast.makeText(this, "解析完成", Toast.LENGTH_SHORT).show();
                                                },
                                                noteError -> {
                                                    lectureRepo.finishLecture(lectureId, "error",
                                                                    localAudioPath, transcript, "", 0)
                                                            .subscribeOn(Schedulers.io())
                                                            .subscribe();
                                                    Toast.makeText(this, "整合失败", Toast.LENGTH_SHORT).show();
                                                }
                                        );
                            },
                            asrError -> {
                                android.util.Log.e("CourseDetail", "ASR 请求失败", asrError);
                                lectureRepo.finishLecture(lectureId, "error",
                                                localAudioPath, "", "", 0)
                                        .subscribeOn(Schedulers.io())
                                        .subscribe();
                                Toast.makeText(this, "解析失败", Toast.LENGTH_SHORT).show();
                            }
                    );
        } catch (IOException e) {
            android.util.Log.e("CourseDetail", "文件读取失败", e);
            Toast.makeText(this, "文件读取失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }




    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing() && !isFinishing() && !isDestroyed()) {
            loadingDialog.dismiss();
        }
    }

    private void retryUpload(Lecture lecture) {
        final String transcript = lecture.getTranscript();
        if (transcript == null || transcript.trim().isEmpty()) {
            retryAsr(lecture);
        } else {
            retryGenerateNote(lecture);
        }
    }

    private void retryAsr(Lecture lecture) {
        final String audioPath = lecture.getAudioPath();
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            android.util.Log.e("CourseDetail", "retryAsr: 文件不存在 " + audioPath);
            Toast.makeText(this, "音频文件已丢失，请重新上传", Toast.LENGTH_SHORT).show();
            return;
        }

        lecture.setStatus("asr");
        lectureRepo.insertLecture(lecture)
                .subscribeOn(Schedulers.io())
                .subscribe();
        Toast.makeText(this, "已重新提交ASR", Toast.LENGTH_SHORT).show();

        RequestBody audioBody = RequestBody.create(audioFile, MediaType.parse("audio/mp4"));
        MultipartBody.Part audioPart = MultipartBody.Part.createFormData(
                "audio_file", audioFile.getName(), audioBody);

        RetrofitClient.getInstance().getApi()
                .asr(audioPart)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        asrResponse -> {
                            String transcript = asrResponse.getTranscript();
                            lectureRepo.saveTranscript(lecture.getId(), transcript, "note_generating")
                                    .subscribeOn(Schedulers.io())
                                    .subscribe();

                            Map<String, String> body = new HashMap<>();
                            body.put("content", transcript);
                            body.put("lecture_title", lecture.getTitle());

                            RetrofitClient.getInstance().getApi()
                                    .generateNote(body)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            noteResponse -> {
                                                lectureRepo.finishLecture(lecture.getId(), "done",
                                                                audioPath, transcript,
                                                                noteResponse.getMarkdown(), lecture.getPhotoCount())
                                                        .subscribeOn(Schedulers.io())
                                                        .subscribe();
                                                Toast.makeText(this, "解析完成", Toast.LENGTH_SHORT).show();
                                            },
                                            noteError -> {
                                                lectureRepo.finishLecture(lecture.getId(), "error",
                                                                audioPath, transcript, "", lecture.getPhotoCount())
                                                        .subscribeOn(Schedulers.io())
                                                        .subscribe();
                                                Toast.makeText(this, "整合失败", Toast.LENGTH_SHORT).show();
                                            }
                                    );
                        },
                        asrError -> {
                            lectureRepo.finishLecture(lecture.getId(), "error",
                                            audioPath, "", "", lecture.getPhotoCount())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe();
                            Toast.makeText(this, "解析失败", Toast.LENGTH_SHORT).show();
                        }
                );
    }

    private void retryGenerateNote(Lecture lecture) {
        final String transcript = lecture.getTranscript();
        final String audioPath = lecture.getAudioPath();

        lecture.setStatus("note_generating");
        lectureRepo.insertLecture(lecture)
                .subscribeOn(Schedulers.io())
                .subscribe();
        Toast.makeText(this, "已重新提交笔记生成", Toast.LENGTH_SHORT).show();

        Map<String, String> body = new HashMap<>();
        body.put("content", transcript);
        body.put("lecture_title", lecture.getTitle());

        RetrofitClient.getInstance().getApi()
                .generateNote(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        noteResponse -> {
                            lectureRepo.finishLecture(lecture.getId(), "done",
                                            audioPath, transcript,
                                            noteResponse.getMarkdown(), lecture.getPhotoCount())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe();
                            Toast.makeText(this, "解析完成", Toast.LENGTH_SHORT).show();
                        },
                        noteError -> {
                            lectureRepo.finishLecture(lecture.getId(), "error",
                                            audioPath, transcript, "", lecture.getPhotoCount())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe();
                            Toast.makeText(this, "整合失败", Toast.LENGTH_SHORT).show();
                        }
                );
    }


}

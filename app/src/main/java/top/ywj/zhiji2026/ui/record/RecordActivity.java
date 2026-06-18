package top.ywj.zhiji2026.ui.record;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import top.ywj.zhiji2026.R;
import top.ywj.zhiji2026.ZhiJiApplication;
import top.ywj.zhiji2026.data.repository.CourseRepository;
import top.ywj.zhiji2026.data.repository.LectureRepository;
import top.ywj.zhiji2026.util.FileUtil;
import top.ywj.zhiji2026.util.TimeFormatter;

public class RecordActivity extends AppCompatActivity {

    private static final String EXTRA_LECTURE_ID = "lecture_id";
    private static final String EXTRA_LECTURE_TITLE = "lecture_title";
    private static final String EXTRA_COURSE_ID = "course_id";
    private static final int REQ_PERMISSIONS = 100;

    private RecordViewModel viewModel;
    private ImageCapture imageCapture;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentAudioPath;
    private TextView tvTimer;
    private MaterialButton btnRecord;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startTime;
    private Runnable timerRunnable;
    private File lectureDir;


    public static void startForNew(AppCompatActivity context, String courseId, String courseName) {
        Intent intent = new Intent(context, RecordActivity.class);
        intent.putExtra(EXTRA_COURSE_ID, courseId);
        intent.putExtra("course_name", courseName);
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        LectureRepository lectureRepo = new LectureRepository(ZhiJiApplication.getDatabase());
        CourseRepository courseRepo = new CourseRepository(ZhiJiApplication.getDatabase());

        String lectureId = getIntent().getStringExtra(EXTRA_LECTURE_ID);
        String courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        String courseName = getIntent().getStringExtra("course_name");
        String safeName = (courseName != null ? courseName : "未命名课程").replaceAll("[\\\\/:*?\"<>|]", "_");
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        String folderName = safeName + "_" + ts;
        lectureDir = FileUtil.getLectureDir(this, folderName);

        viewModel = new ViewModelProvider(this,
                new RecordViewModel.Factory(lectureRepo, courseRepo, lectureId, courseId, courseName))
                .get(RecordViewModel.class);

        String mode = getIntent().getStringExtra("mode");
        if ("upload".equals(mode)) {
            String audioUri = getIntent().getStringExtra("audio_uri");
            if (audioUri != null) {
                copyAudioFromUri(Uri.parse(audioUri));
                viewModel.startAsr(currentAudioPath, getIntent().getStringExtra("course_name"));
                Toast.makeText(this, "正在解析，处理中...", Toast.LENGTH_LONG).show();
                new Handler().postDelayed(this::finish, 2000);
            }
            return;  // 不继续往下走录制 UI
        }

        tvTimer = findViewById(R.id.tv_timer);
        btnRecord = findViewById(R.id.btn_record);

        btnRecord.setOnClickListener(v -> toggleRecording());

        // 替换 findViewById(R.id.btn_stop) 整段 onClickListener：
        findViewById(R.id.btn_stop).setOnClickListener(v -> {
            stopRecording();
            if (currentAudioPath != null) {
                String title = getIntent().getStringExtra("course_name");
                viewModel.startAsr(currentAudioPath, title);
                Toast.makeText(this, "录制结束，后台处理中...", Toast.LENGTH_SHORT).show();
            }
            finish();
        });

        findViewById(R.id.btn_capture).setOnClickListener(v -> capturePhoto());

        if (checkPermissions()) {
            startCamera();
        }
    }

    private void copyAudioFromUri(Uri uri) {
        File dest = new File(lectureDir, "audio_uploaded.m4a");
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new java.io.FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            currentAudioPath = dest.getAbsolutePath();
        } catch (IOException e) {
            Toast.makeText(this, "文件读取失败", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean checkPermissions() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        boolean allGranted = true;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, perms, REQ_PERMISSIONS);
        }
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) allGranted = false;
            }
            if (allGranted) {
                startCamera();
            } else {
                Toast.makeText(this, "需要相机和录音权限", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                // Activity 已销毁则跳过
                if (isFinishing() || isDestroyed()) return;

                Preview preview = new Preview.Builder().build();
                PreviewView previewView = findViewById(R.id.viewfinder);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector;
                if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                } else {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                }

                provider.unbindAll();
                provider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException | CameraInfoUnavailableException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }



    private void toggleRecording() {
        if (isRecording) {
            pauseRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        currentAudioPath = new File(lectureDir, "audio.m4a").getAbsolutePath();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(currentAudioPath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            btnRecord.setIconResource(R.drawable.ic_pause);
            btnRecord.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.recording_red));
            startTimer();
            Toast.makeText(this, "录音开始", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "录音启动失败", Toast.LENGTH_SHORT).show();
        }
    }


    private void pauseRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        isRecording = false;
        btnRecord.setIconResource(R.drawable.ic_mic);
        btnRecord.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
        stopTimer();
        viewModel.setAudioPath(currentAudioPath);
        Toast.makeText(this, "录音已暂停", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        if (isRecording) {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            isRecording = false;
            stopTimer();
            viewModel.setAudioPath(currentAudioPath);
        }
    }


    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsed / 1000) % 60;
                int minutes = (int) (elapsed / 60000);
                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
                handler.postDelayed(this, 200);
            }
        };
        handler.post(timerRunnable);
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable);
    }

    private void capturePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(lectureDir,
                "IMG_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        viewModel.addPhoto(photoFile.getAbsolutePath());
                        Toast.makeText(RecordActivity.this, "拍照成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(RecordActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }
}

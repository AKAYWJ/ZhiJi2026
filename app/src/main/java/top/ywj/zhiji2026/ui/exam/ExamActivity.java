package top.ywj.zhiji2026.ui.exam;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import top.ywj.zhiji2026.R;
import top.ywj.zhiji2026.ZhiJiApplication;
import top.ywj.zhiji2026.data.repository.NoteRepository;

public class ExamActivity extends AppCompatActivity {

    private ExamViewModel viewModel;
    private EditText etScope, etFocus;
    private MaterialButton btnGenerate;
    private WebView webviewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        String courseId = getIntent().getStringExtra("course_id");
        String courseName = getIntent().getStringExtra("course_name");
        toolbar.setTitle(courseName != null ? courseName + " - 复习提纲" : "复习提纲");


        NoteRepository noteRepo = new NoteRepository(ZhiJiApplication.getDatabase());
        viewModel = new ViewModelProvider(this,
                new ExamViewModel.Factory(noteRepo, courseId)).get(ExamViewModel.class);


        etScope = findViewById(R.id.et_scope);
        etFocus = findViewById(R.id.et_focus);
        btnGenerate = findViewById(R.id.btn_generate);
        webviewResult = findViewById(R.id.webview_result);

        webviewResult.getSettings().setJavaScriptEnabled(true);
        webviewResult.setWebViewClient(new WebViewClient());

        btnGenerate.setOnClickListener(v -> {
            String scope = etScope.getText().toString().trim();
            String focus = etFocus.getText().toString().trim();
            if (scope.isEmpty()) {
                Toast.makeText(this, "请填写考试范围", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.generateReview(scope, focus);
        });

        viewModel.getReviewHtml().observe(this, html -> {
            if (html != null) {
                webviewResult.setVisibility(android.view.View.VISIBLE);
                webviewResult.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

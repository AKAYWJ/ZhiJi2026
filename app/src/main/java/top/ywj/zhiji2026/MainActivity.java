package top.ywj.zhiji2026;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import top.ywj.zhiji2026.data.model.Course;
import top.ywj.zhiji2026.data.repository.CourseRepository;
import top.ywj.zhiji2026.ui.exam.ExamActivity;
import top.ywj.zhiji2026.ui.home.CourseAdapter;
import top.ywj.zhiji2026.ui.home.HomeViewModel;
import top.ywj.zhiji2026.ui.course.CourseDetailActivity;
import top.ywj.zhiji2026.ui.manual.ManualActivity;

import android.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;


public class MainActivity extends AppCompatActivity {

    private HomeViewModel viewModel;
    private CourseAdapter adapter;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CourseRepository courseRepo = new CourseRepository(ZhiJiApplication.getDatabase());
        viewModel = new ViewModelProvider(this,
                new HomeViewModel.Factory(courseRepo)).get(HomeViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setupRecyclerView();
        setupFab();
        observeData();
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_exam) {
                startActivity(new Intent(this, ExamActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_manual) {
                startActivity(new Intent(this, ManualActivity.class));
                return true;
            }
            return false;
        });


    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rv_courses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(course -> {
            Intent intent = new Intent(this, CourseDetailActivity.class);
            intent.putExtra("course_id", course.getId());
            intent.putExtra("course_name", course.getName());
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        // 左滑删除课程
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                Course course = adapter.getCurrentList().get(pos);
                adapter.notifyItemChanged(pos); // 恢复显示，等用户确认

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
                androidx.appcompat.app.AlertDialog dialog = builder
                        .setTitle("删除课程")
                        .setMessage("确定删除「" + course.getName() + "」吗？\n将同时删除该课程下所有录音、笔记和文件，且不可恢复。")
                        .setNegativeButton("取消", (d, w) -> adapter.notifyItemChanged(pos))
                        .setPositiveButton("删除", (d, w) -> viewModel.deleteCourse(course))
                        .show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(getColor(R.color.recording_red));
            }
        });
        helper.attachToRecyclerView(rv);
    }


    private void setupFab() {
        fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> showCreateDialog());
    }

    private void observeData() {
        viewModel.getCourses().observe(this, courses -> {
            if (courses != null) {
                adapter.submitList(courses);
            }
        });
    }

    private void showCreateDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_course, null);
        EditText etName = dialogView.findViewById(R.id.et_course_name);
        EditText etSemester = dialogView.findViewById(R.id.et_semester);

        new MaterialAlertDialogBuilder(this)
                .setTitle("新建课程")
                .setView(dialogView)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String semester = etSemester.getText().toString().trim();
                    if (!name.isEmpty()) {
                        viewModel.createCourse(name, semester);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}

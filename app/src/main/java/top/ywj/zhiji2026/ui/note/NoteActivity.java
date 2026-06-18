package top.ywj.zhiji2026.ui.note;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import top.ywj.zhiji2026.R;

public class NoteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        String lectureId = getIntent().getStringExtra("lecture_id");
        String lectureTitle = getIntent().getStringExtra("lecture_title");

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(lectureTitle != null ? lectureTitle : "笔记");
        toolbar.setNavigationOnClickListener(v -> finish());

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        viewPager.setUserInputEnabled(false);

        TabLayout tabLayout = findViewById(R.id.tab_layout);

        NotePagerAdapter adapter = new NotePagerAdapter(this, lectureId);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("笔记"); break;
                case 1: tab.setText("思维导图"); break;
                case 2: tab.setText("知识图谱"); break;
            }
        }).attach();
    }
}

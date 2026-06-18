package top.ywj.zhiji2026.ui.manual;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import top.ywj.zhiji2026.R;

public class ManualActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvContent = findViewById(R.id.tv_content);
        tvContent.setText(buildManual());
    }

    private SpannableString buildManual() {
        String text =
            "创建课程\n点击主页右下角「+」按钮，输入课程名称和学期，即可创建一门新课程。\n\n" +
            "添加录音\n进入课程详情页，点击右下角录制按钮：\n  • 开始录音：直接录制课堂音频，录制过程中可拍照标记重点\n  • 上传音频文件：从手机中选择已有的录音文件\n\n" +
            "录音处理\n录音提交后，系统自动进行：\n  • 解析中：语音识别（ASR），将音频转为文字\n  • 整合中：AI 整理，生成结构化 Markdown 笔记\n  • 已处理：完成，可查看思维导图、知识图谱等\n\n" +
            "查看笔记\n录音处理完成后，进入讲座详情可查看：\n  • 课堂笔记：AI 整理的结构化笔记\n  • 思维导图：知识点可视化导图\n  • 知识图谱：知识点之间的关联关系\n\n" +
            "删除内容\n在主页左滑课程卡片，或在课程详情页左滑讲座，确认后即可删除。删除后不可恢复。\n\n" +
            "考前突击\n点击主页右上角第一个按钮，输入考试范围，AI 根据已学内容生成复习提纲。";

        SpannableString ss = new SpannableString(text);
        boldTitle(ss, text, "创建课程");
        boldTitle(ss, text, "添加录音");
        boldTitle(ss, text, "录音处理");
        boldTitle(ss, text, "查看笔记");
        boldTitle(ss, text, "删除内容");
        boldTitle(ss, text, "考前突击");
        return ss;
    }

    private void boldTitle(SpannableString ss, String text, String keyword) {
        int start = text.indexOf(keyword);
        if (start < 0) return;
        int end = start + keyword.length();
        ss.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new AbsoluteSizeSpan(18, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}

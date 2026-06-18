package top.ywj.zhiji2026.ui.note;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import top.ywj.zhiji2026.ZhiJiApplication;
import top.ywj.zhiji2026.data.repository.LectureRepository;

public class NoteFragment extends Fragment {

    private static final String ARG_LECTURE_ID = "lecture_id";
    private final CompositeDisposable disposables = new CompositeDisposable();

    public static NoteFragment newInstance(String lectureId) {
        NoteFragment f = new NoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LECTURE_ID, lectureId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        WebView webView = new WebView(requireContext());
        webView.getSettings().setJavaScriptEnabled(false);
        webView.setWebViewClient(new WebViewClient());

        String lectureId = getArguments() != null ? getArguments().getString(ARG_LECTURE_ID) : null;
        if (lectureId == null) {
            webView.loadData("NULL", "text/html", "UTF-8");
            return webView;
        }

        LectureRepository repo = new LectureRepository(ZhiJiApplication.getDatabase());

        disposables.add(
                repo.getLectureById(lectureId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                lecture -> {
                                    String md = lecture.getMarkdown();
                                    String bodyHtml;
                                    if (md != null && !md.isEmpty()) {
                                        android.util.Log.d("NoteFragment", "markdown长度=" + md.length());
                                        MutableDataSet options = new MutableDataSet();
                                        Parser parser = Parser.builder(options).build();
                                        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
                                        bodyHtml = renderer.render(parser.parse(md));
                                    } else {
                                        android.util.Log.d("NoteFragment", "markdown为空");
                                        bodyHtml = "<p style='color:#999;text-align:center;margin-top:80px'>暂无笔记内容</p>";
                                    }
                                    webView.loadDataWithBaseURL(null, buildPage(bodyHtml),
                                            "text/html", "UTF-8", null);
                                },
                                error -> {
                                    android.util.Log.e("NoteFragment", "加载失败", error);
                                    Toast.makeText(getContext(), "加载笔记失败", Toast.LENGTH_SHORT).show();
                                    webView.loadDataWithBaseURL(null,
                                            buildPage("<p style='color:#999;text-align:center;margin-top:80px'>加载失败</p>"),
                                            "text/html", "UTF-8", null);
                                }
                        )
        );


        return webView;
    }

    private String buildPage(String bodyHtml) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "body{font-family:-apple-system,sans-serif;padding:20px;line-height:1.8;color:#333;max-width:800px;margin:0 auto}"
                + "h1{color:#1976D2;border-bottom:2px solid #1976D2;padding-bottom:8px}"
                + "h2{color:#FF5722;margin-top:24px}"
                + "h3{color:#333;margin-top:20px}"
                + "ul,ol{padding-left:20px}li{margin:6px 0}"
                + "strong{color:#1976D2}"
                + "blockquote{background:#F5F5F5;padding:12px 16px;border-left:4px solid #1976D2;margin:16px 0;color:#555}"
                + "code{background:#f4f4f4;padding:2px 6px;border-radius:4px;font-size:14px}"
                + "pre{background:#f4f4f4;padding:12px;border-radius:6px;overflow-x:auto}"
                + "table{border-collapse:collapse;width:100%;margin:16px 0}"
                + "th,td{border:1px solid #ddd;padding:8px 12px;text-align:left}"
                + "th{background:#E3F2FD}"
                + "</style></head><body>" + bodyHtml + "</body></html>";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}

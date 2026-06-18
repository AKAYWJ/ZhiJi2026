package top.ywj.zhiji2026.ui.note;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import top.ywj.zhiji2026.ZhiJiApplication;
import top.ywj.zhiji2026.data.repository.LectureRepository;

public class MindMapFragment extends Fragment {

    private static final String ARG_LECTURE_ID = "lecture_id";
    private final CompositeDisposable disposables = new CompositeDisposable();

    public static MindMapFragment newInstance(String lectureId) {
        MindMapFragment f = new MindMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LECTURE_ID, lectureId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(requireContext());

        WebView webView = new WebView(requireContext());
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        ProgressBar progressBar = new ProgressBar(requireContext());
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        root.addView(webView);
        root.addView(progressBar);

        String lectureId = getArguments() != null ? getArguments().getString(ARG_LECTURE_ID) : null;
        if (lectureId == null) {
            return root;
        }

        LectureRepository repo = new LectureRepository(ZhiJiApplication.getDatabase());

        disposables.add(
                repo.getLectureById(lectureId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                lecture -> {
                                    String md = lecture.getMarkdown();
                                    if (md == null || md.isEmpty()) {
                                        showEmpty(webView, progressBar);
                                        return;
                                    }
                                    String html = buildMarkmapHtml(md);
                                    webView.loadDataWithBaseURL(null, html,
                                            "text/html", "UTF-8", null);
                                },
                                error -> {
                                    Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                                    showEmpty(webView, progressBar);
                                }
                        )
        );

        return root;
    }

    private String buildMarkmapHtml(String markdown) {
        String safe = markdown
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "*{margin:0;padding:0}"
                + "#mindmap{width:100vw;height:100vh}"
                + ".markmap-foreign{font-size:14px;font-family:-apple-system,sans-serif}"
                + "</style>"
                + "<script src='https://cdn.jsdelivr.net/npm/d3@7'></script>"
                + "<script src='https://cdn.jsdelivr.net/npm/markmap-view@0.15'></script>"
                + "<script src='https://cdn.jsdelivr.net/npm/markmap-lib@0.15'></script>"
                + "</head><body>"
                + "<script id='markmap-data' type='text/markdown'>" + safe + "</script>"
                + "<svg id='mindmap'></svg>"
                + "<div id='err' style='color:red;padding:40px;font-family:monospace;white-space:pre-wrap;display:none'></div>"
                + "<script>"
                + "try{"
                + "var md=document.getElementById('markmap-data').textContent;"
                + "var t=new markmap.Transformer();"
                + "var result=t.transform(md);"
                + "markmap.Markmap.create("
                + "'#mindmap',"
                + "{autoFit:!0,fitRatio:0.95,maxWidth:280,colorFreezeLevel:2,initialExpandLevel:2},"
                + "result.root"
                + ");"
                + "}catch(e){"
                + "var d=document.getElementById('err');"
                + "d.style.display='block';"
                + "d.textContent='渲染失败: '+e.message;"
                + "}"
                + "</script>"
                + "</body></html>";
    }

    private void showEmpty(WebView webView, ProgressBar progressBar) {
        progressBar.setVisibility(View.GONE);
        webView.loadData("<p style='color:#999;text-align:center;margin-top:80px'>暂无内容</p>",
                "text/html", "UTF-8");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}

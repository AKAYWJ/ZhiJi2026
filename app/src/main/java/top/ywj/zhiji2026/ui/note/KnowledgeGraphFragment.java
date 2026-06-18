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

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import top.ywj.zhiji2026.ZhiJiApplication;
import top.ywj.zhiji2026.data.remote.RetrofitClient;
import top.ywj.zhiji2026.data.remote.dto.KnowledgeGraphResponse;
import top.ywj.zhiji2026.data.repository.LectureRepository;

public class KnowledgeGraphFragment extends Fragment {

    private static final String ARG_LECTURE_ID = "lecture_id";
    private final CompositeDisposable disposables = new CompositeDisposable();

    private static final String[] GROUP_COLORS = {
            "#1976D2", "#FF5722", "#4CAF50", "#9C27B0"
    };

    public static KnowledgeGraphFragment newInstance(String lectureId) {
        KnowledgeGraphFragment f = new KnowledgeGraphFragment();
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
                        .flatMap(lecture -> {
                            // 有缓存直接用
                            String cachedJson = lecture.getGraphData();
                            if (cachedJson != null && !cachedJson.isEmpty()) {
                                android.util.Log.d("KnowledgeGraphFragment", "使用缓存");
                                KnowledgeGraphResponse cached = new Gson().fromJson(
                                        cachedJson, KnowledgeGraphResponse.class);
                                return io.reactivex.rxjava3.core.Single.just(cached);
                            }
                            // 没缓存调后端
                            String md = lecture.getMarkdown();
                            if (md == null || md.isEmpty()) {
                                return io.reactivex.rxjava3.core.Single.error(
                                        new RuntimeException("markdown为空"));
                            }
                            android.util.Log.d("KnowledgeGraphFragment",
                                    "请求后端生成, markdown长度=" + md.length());
                            Map<String, String> body = new HashMap<>();
                            body.put("note_content", md);
                            return RetrofitClient.getInstance().getApi()
                                    .generateKnowledgeGraph(body)
                                    .flatMap(resp -> {
                                        android.util.Log.d("KnowledgeGraphFragment",
                                                "后端返回, nodes=" + resp.getNodes().size()
                                                        + ", edges=" + resp.getEdges().size());
                                        lecture.setGraphData(new Gson().toJson(resp));
                                        return repo.updateLecture(lecture)
                                                .andThen(io.reactivex.rxjava3.core.Single.just(resp));
                                    });
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                resp -> {
                                    int nodeCount = resp.getNodes() != null ? resp.getNodes().size() : 0;
                                    int edgeCount = resp.getEdges() != null ? resp.getEdges().size() : 0;
                                    android.util.Log.d("KnowledgeGraphFragment",
                                            "[知识图谱] nodes=" + nodeCount + ", edges=" + edgeCount);
                                    if (resp.getEdges() != null) {
                                        for (KnowledgeGraphResponse.Edge e : resp.getEdges()) {
                                            android.util.Log.d("KnowledgeGraphFragment",
                                                    "[边] " + e.getSource() + " -> " + e.getTarget() + "  label=" + e.getLabel());
                                        }
                                    }

                                    if (nodeCount == 0) {
                                        webView.loadData("<p style='color:#999;text-align:center;margin-top:80px'>暂无内容</p>",
                                                "text/html", "UTF-8");
                                        progressBar.setVisibility(View.GONE);
                                        return;
                                    }

                                    String optionJson = buildEChartsOption(resp);
                                    String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                                            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                                            + "<script src='https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js'></script>"
                                            + "<style>body{margin:0;padding:0}#chart{width:100vw;height:100vh}</style>"
                                            + "</head><body><div id='chart'></div>"
                                            + "<script>"
                                            + "var chart=echarts.init(document.getElementById('chart'));"
                                            + "chart.setOption(" + optionJson + ");"
                                            + "window.addEventListener('resize',function(){chart.resize()});"
                                            + "</script></body></html>";
                                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                                },

                                error -> {
                                    android.util.Log.e("KnowledgeGraphFragment", "生成失败", error);
                                    Toast.makeText(getContext(), "生成知识图谱失败", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                    webView.loadData("<p style='color:#999;text-align:center;margin-top:80px'>暂无内容</p>",
                                            "text/html", "UTF-8");
                                }
                        )
        );

        return root;
    }

    private String buildEChartsOption(KnowledgeGraphResponse resp) {
        // categories 配色数组
        String[] catColors = {"#1976D2", "#FF5722", "#4CAF50", "#9C27B0"};

        StringBuilder categories = new StringBuilder("[");
        for (int i = 0; i < 4; i++) {
            if (i > 0) categories.append(",");
            categories.append("{name:'group").append(i).append("',itemStyle:{color:'")
                    .append(catColors[i]).append("'}}");
        }
        categories.append("]");

        StringBuilder data = new StringBuilder("[");
        for (int i = 0; i < resp.getNodes().size(); i++) {
            KnowledgeGraphResponse.Node n = resp.getNodes().get(i);
            if (i > 0) data.append(",");
            int group = Math.min(n.getGroup(), 3);
            int size = group == 0 ? 40 : group == 1 ? 30 : group == 2 ? 22 : 18;
            String name = escapeJs(trim(n.getLabel()));
            data.append("{name:'").append(name).append("'")
                    .append(",symbolSize:").append(size)
                    .append(",category:").append(group)
                    .append("}");
        }
        data.append("]");

        StringBuilder links = new StringBuilder("[");
        if (resp.getEdges() != null) {
            for (int i = 0; i < resp.getEdges().size(); i++) {
                KnowledgeGraphResponse.Edge e = resp.getEdges().get(i);
                if (i > 0) links.append(",");
                String src = escapeJs(trim(e.getSource()));
                String tgt = escapeJs(trim(e.getTarget()));
                String lbl = escapeJs(trim(e.getLabel()));
                links.append("{source:'").append(src).append("'")
                        .append(",target:'").append(tgt).append("'")
                        .append(",label:{show:true,formatter:'").append(lbl)
                        .append("',fontSize:10,color:'#888'}");
                links.append("}");
            }
        }
        links.append("]");

        return "{tooltip:{},"
                + "series:[{type:'graph',layout:'force',roam:true,"
                + "label:{show:true,fontSize:13},"
                + "edgeSymbol:['none','arrow'],"
                + "force:{repulsion:300,edgeLength:[80,200]},"
                + "categories:" + categories + ","
                + "data:" + data + ","
                + "links:" + links + "}]}";
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }


    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}

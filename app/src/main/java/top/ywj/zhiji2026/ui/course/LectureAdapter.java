package top.ywj.zhiji2026.ui.course;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import top.ywj.zhiji2026.R;
import top.ywj.zhiji2026.data.model.Lecture;

public class LectureAdapter extends ListAdapter<Lecture, LectureAdapter.ViewHolder> {

    private final OnLectureClickListener listener;

    public LectureAdapter(OnLectureClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lecture, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Lecture lecture = getItem(position);
        holder.tvTitle.setText(lecture.getTitle());
        holder.tvMeta.setText(getStatusText(lecture));
        int statusBgRes;
        String statusText;
        if ("done".equals(lecture.getStatus())) {
            statusText = "已处理";
            statusBgRes = R.drawable.bg_status_done;
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_done_text));
            holder.progressProcessing.setVisibility(View.GONE);
        } else if ("asr".equals(lecture.getStatus())) {
            statusText = "解析中";
            statusBgRes = R.drawable.bg_status_processing;
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_processing_text));
            holder.progressProcessing.setVisibility(View.VISIBLE);
        } else if ("note_generating".equals(lecture.getStatus())) {
            statusText = "整合中";
            statusBgRes = R.drawable.bg_status_note_generating;
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_note_gen_text));
            holder.progressProcessing.setVisibility(View.VISIBLE);
        } else if ("error".equals(lecture.getStatus())) {
            statusText = "解析失败";
            statusBgRes = R.drawable.bg_status_error;
            holder.progressProcessing.setVisibility(View.GONE);
        } else {
            statusText = "待录制";
            statusBgRes = R.drawable.bg_status_pending;
            holder.progressProcessing.setVisibility(View.GONE);
        }


        holder.tvStatus.setText(statusText);
        holder.tvStatus.setBackgroundResource(statusBgRes);
        holder.itemView.setOnClickListener(v -> listener.onClick(lecture));
    }

    private String getStatusText(Lecture lecture) {
        if (lecture.getCreatedAt() > 0) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date(lecture.getCreatedAt()));
        }
        return "";
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta, tvStatus;
        ProgressBar progressProcessing;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_lecture_title);
            tvMeta = itemView.findViewById(R.id.tv_lecture_meta);
            tvStatus = itemView.findViewById(R.id.tv_status);
            progressProcessing = itemView.findViewById(R.id.progress_processing);
        }
    }


    public interface OnLectureClickListener {
        void onClick(Lecture lecture);
    }

    private static final DiffUtil.ItemCallback<Lecture> DIFF_CALLBACK = new DiffUtil.ItemCallback<Lecture>() {
        @Override
        public boolean areItemsTheSame(@NonNull Lecture oldItem, @NonNull Lecture newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Lecture oldItem, @NonNull Lecture newItem) {
            return oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getCreatedAt() == newItem.getCreatedAt()
                    && java.util.Objects.equals(oldItem.getStatus(), newItem.getStatus());


        }
    };
}

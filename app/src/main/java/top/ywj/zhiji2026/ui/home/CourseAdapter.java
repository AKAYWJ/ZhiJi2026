package top.ywj.zhiji2026.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import top.ywj.zhiji2026.R;
import top.ywj.zhiji2026.data.model.Course;

public class CourseAdapter extends ListAdapter<Course, CourseAdapter.ViewHolder> {

    private final OnCourseClickListener listener;

    public CourseAdapter(OnCourseClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Course course = getItem(position);
        holder.tvName.setText(course.getName());
        holder.tvInfo.setText(course.getSemester());
        holder.itemView.setOnClickListener(v -> listener.onClick(course));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_course_name);
            tvInfo = itemView.findViewById(R.id.tv_course_info);
        }
    }

    public interface OnCourseClickListener {
        void onClick(Course course);
    }

    private static final DiffUtil.ItemCallback<Course> DIFF_CALLBACK = new DiffUtil.ItemCallback<Course>() {
        @Override
        public boolean areItemsTheSame(@NonNull Course oldItem, @NonNull Course newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Course oldItem, @NonNull Course newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getSemester().equals(newItem.getSemester());
        }
    };
}

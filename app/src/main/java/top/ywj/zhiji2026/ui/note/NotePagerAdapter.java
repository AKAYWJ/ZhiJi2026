package top.ywj.zhiji2026.ui.note;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class NotePagerAdapter extends FragmentStateAdapter {

    private final String lectureId;

    public NotePagerAdapter(@NonNull FragmentActivity activity, String lectureId) {
        super(activity);
        this.lectureId = lectureId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return NoteFragment.newInstance(lectureId);
            case 1: return MindMapFragment.newInstance(lectureId);
            case 2: return KnowledgeGraphFragment.newInstance(lectureId);
            default: return NoteFragment.newInstance(lectureId);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}

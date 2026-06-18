package top.ywj.zhiji2026.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.TextView;

import top.ywj.zhiji2026.R;

public class LoadingDialog {

    private final Dialog dialog;
    private final TextView tvMessage;

    public LoadingDialog(Context context) {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_loading);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        tvMessage = dialog.findViewById(R.id.tv_loading_message);
    }

    public void show(String message) {
        tvMessage.setText(message);
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }
}

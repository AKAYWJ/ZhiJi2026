package top.ywj.zhiji2026.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class FileUtil {

    public static String getAudioFilePath(Context context) {
        File dir = new File(context.getExternalCacheDir(), "audio");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "lecture_" + System.currentTimeMillis() + ".m4a").getAbsolutePath();
    }

    public static String getPhotoFilePath(Context context) {
        File dir = new File(context.getExternalCacheDir(), "photos");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "photo_" + System.currentTimeMillis() + ".jpg").getAbsolutePath();
    }
    public static File getPhotosDir(Context context) {
        File dir = new File(context.getExternalCacheDir(), "photos");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }


    public static File getLectureDir(Context context, String folderName) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "ZhiJi/Lectures/" + folderName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }


}

package top.ywj.zhiji2026.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeFormatter {

    public static String formatSeconds(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Generate sanitized lecture folder name.
     */
    public static String generateLectureFolderName(String courseName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        // Sanitize: replace spaces and special chars with underscore
        String safeName = courseName.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        return safeName + "_" + timestamp;
    }

}

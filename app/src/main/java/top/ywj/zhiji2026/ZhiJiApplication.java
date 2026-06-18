package top.ywj.zhiji2026;

import android.app.Application;

import top.ywj.zhiji2026.data.local.AppDatabase;

public class ZhiJiApplication extends Application {

    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.getInstance(this);
    }

    public static AppDatabase getDatabase() {
        return database;
    }
}

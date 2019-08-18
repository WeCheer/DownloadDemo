package com.wyc.download.util;

import android.os.Environment;

import com.wyc.download.App;


public class Constants {
    public final static String APP_DOWNLOAD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/" + App.context().getPackageName() + "/download/";

    public static final String BASE_URL = "http://dldir1.qq.com/";

    public static class Extras {
        public static final String DOWNLOAD_URL = "download_url";
        public static final String DOWNLOAD_ID = "download_id";
        public static final String DOWNLOAD_FILE = "download_file";
    }
}

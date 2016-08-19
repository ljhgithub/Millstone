package com.pysun.library.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by ljh on 2016/8/19.
 */
public class StorageUtils {

    public static String getDiskCacheFileDirPath(Context context, String uniqueName) {
        String state = Environment.getExternalStorageState();
        final String cachePath = (Environment.MEDIA_MOUNTED.equals(state) || !Environment.isExternalStorageRemovable()) ? context.getExternalCacheDir().getPath() : context.getCacheDir().getPath();
        Log.d("tag", cachePath);
        File dir = new File(cachePath + File.separator + uniqueName);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir.getPath();
    }
}

package com.pysun.library.util;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by ljh on 2016/8/10.
 */
public class ScreenUtils  {

    private static int screenWidth;
    private static int screenHeight;
    public static void init(Context context) {
        if (null == context) {
            return;
        }
        DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
    }

    public static int getScreenWidth(){
        return screenWidth;
    }

    public static int getScreenHeight() {
        return screenHeight;
    }
}

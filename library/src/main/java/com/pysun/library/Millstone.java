package com.pysun.library;

import android.content.Context;
import com.pysun.library.util.ScreenUtils;

import java.io.File;
/**
 * Created by ljh on 2016/8/9.
 */
public class Millstone {

    private static final String TAG = Millstone.class.getSimpleName();
    private static volatile Millstone millstone;

    public static Millstone get(Context context){
        if (null==millstone){
            synchronized (Millstone.class){
                if (null==millstone){
                    millstone=new Millstone(context);
                }

            }
        }
        return millstone;
    }


    public Millstone(Context context){
        ScreenUtils.init(context);
    }


    public MillstoneManager load(String srcFilePath) {
        return new MillstoneManager(srcFilePath);
    }



}

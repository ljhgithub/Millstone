package com.pysun.library.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import com.pysun.library.MillstoneManager;

import java.io.File;
import java.io.IOException;

/**
 * Created by Administrator on 2017/7/4.
 */

public class ImageScaleUtils {

    private final static String TAG=ImageScaleUtils.class.getSimpleName();

    public static int[] decodeBounds(String pathName) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        return new int[]{options.outWidth, options.outHeight};
    }


    /**
     * 计算 InSampleSize
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

            long totalPixels = width * height / inSampleSize;

            if ((Math.max(width, height) / Math.min(width, height)) > 16 / 9) {
                final long totalReqPixelsCap = reqWidth * reqHeight * 2;
                while (totalPixels > totalReqPixelsCap) {
                    inSampleSize *= 2;
                    totalPixels /= 2;
                }
            }


        }
        return inSampleSize;
    }

    /**
     * 获取图片的缩放比
     *
     * @return
     */
    public static float getImageScale(int srcWidth, int srcHeight, int reqWidth, int reqHeight) {
        if (srcWidth == 0 || srcHeight == 0 || reqWidth <= 0 || reqHeight <= 0) return 1;
        if (srcWidth * srcHeight > reqWidth * reqHeight) {
            float widthScale = (float) srcWidth / reqWidth;
            float heightScale = (float) srcHeight / reqHeight;
            return 1 / Math.max(widthScale, heightScale);
        }
        return 1;
    }

    /**
     * 获取图片的旋转角度
     *
     * @return
     */
    public static float getImageRotate(int rotate) {
        float f;
        if (rotate == 6) {
            f = 90.0F;
        } else if (rotate == 3) {
            f = 180.0F;
        } else if (rotate == 8) {
            f = 270.0F;
        } else {
            f = 0.0F;
        }

        return f;
    }

    // Common display resolutions for wiki "screen resolution"
    public static int[] buildDisplayResolution(int width, int height) {
        float ratio = (float) width / height;
        int bounds[] = new int[2];// bounds[0]为宽   bounds[1]为高
        if (ratio >= 3.2f / 4) {                        //5:4 1280*1024
            bounds[0] = 1280;
            bounds[1] = 1280;
            Log.d(TAG, " 5:4 1280*1024 ");
        } else if (ratio >= 11.f / 16 && ratio < 3.2f / 4) {//4:3 1024*768 1280*920
            if (height > 1280) {
                int remainder1280 = height % 1280;
                int remainder1024 = height % 1024;
                if (remainder1280 > remainder1024) {
                    bounds[0] = 1024;
                    bounds[1] = 1024;
                } else {
                    bounds[0] = 1280;
                    bounds[1] = 1280;
                }
            } else {
                bounds[0] = 1024;
                bounds[1] = 1024;
            }
            Log.d(TAG, " 4:3 1024*768 1280*920 ");
        } else if (ratio >= 19.f / 32 && ratio < 11.f / 16) {// 16:10 1280*800 1440*900
            if (height > 1440) {
                int remainder1440 = height % 1440;
                int remainder1280 = height % 1280;
                if (remainder1440 > remainder1280) {
                    bounds[0] = 1280;
                    bounds[1] = 1280;
                } else {
                    bounds[0] = 1440;
                    bounds[1] = 1440;
                }
            } else {
                bounds[0] = 1280;
                bounds[1] = 1280;
            }
            Log.d(TAG, "16:10 1280*800 1440*900");
        } else if (ratio >= 0.546 && ratio < 19.f / 32) {// 16:9 1920*1080 1280*720
            if (height > 1920) {
                int remainder1920 = height % 1920;
                int remainder1280 = height % 1280;
                if (remainder1920 > remainder1280) {
                    bounds[0] = 1280;
                    bounds[1] = 1280;
                } else {
                    bounds[0] = 1920;
                    bounds[1] = 1920;
                }
            } else {
                bounds[0] = 1280;
                bounds[1] = 1280;
            }
            bounds[0] = 1280;
            bounds[1] = 1280;
            Log.d(TAG, "  16:9 1920*1080 1280*720");
        } else {
            bounds[0] = 0;
            bounds[1] = 0;
            Log.d(TAG, " 0*0");
        }
        return bounds;

    }
    public static Bitmap decodeSampleBitmapFormFile(String filename, int reqWidth, int reqHeight) {
        //First decode with inJustDecodeBounds=true to checkout dimensions
        File srcFile = new File(filename);

        if (!srcFile.exists() || !srcFile.isFile()) return null;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);
        //Calculate InSampleSize
        int inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        Bitmap firstCompressBitmap = BitmapFactory.decodeFile(filename, options);
        if (null == firstCompressBitmap) return null;

        float scale = getImageScale(firstCompressBitmap.getWidth(), firstCompressBitmap.getHeight(), reqWidth, reqHeight);
        try {
            ExifInterface exifInterface = new ExifInterface(srcFile.getAbsolutePath());
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            float rotate = getImageRotate(orientation);
            if (scale >= 1 && rotate == 0) {//图片已符合要求，不需要进一步处理
                return firstCompressBitmap;
            } else {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);
                matrix.postScale(scale, scale);
                return Bitmap.createBitmap(firstCompressBitmap, 0, 0, firstCompressBitmap.getWidth(), firstCompressBitmap.getHeight(), matrix, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return firstCompressBitmap;
        }
    }
}

package com.pysun.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import com.pysun.library.util.ScreenUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by ljh on 2016/8/9.
 */
public class Millstone {

    private String mSrcFilePath;
    private File mCompressedFile;
    private File mCacheFileDir;
    private Context mContext;
    private int mReqWidth;
    private int mReqHeight;
    private CompressListener mCompressListener;

    public static Millstone getInstance() {
        return InstanceHolder.instance;
    }

    public Millstone with(Context context) {
        mContext = context;
        ScreenUtils.init(context);
        return getInstance();
    }

    public Millstone load(String srcFilePath) {
        mSrcFilePath = srcFilePath;
        return getInstance();
    }

    public Millstone diskCache(File cacheFile) {
        mCacheFileDir = cacheFile;
        return getInstance();
    }

    public Millstone setCompressListener(CompressListener compressListener) {
        this.mCompressListener = compressListener;
        return getInstance();
    }

    public Millstone intoFile(File file) {
        mCompressedFile = file;
        return getInstance();

    }

    public void compress(int reqWidth, int reqHeight) {
        mCompressedFile = null;
        mReqWidth = reqWidth;
        mReqHeight = reqHeight;
        if (0 == reqWidth * reqHeight || Math.min(reqHeight, reqWidth) < 0) {
            mReqWidth = ScreenUtils.getScreenWidth();
            mReqHeight = ScreenUtils.getScreenHeight();
            Log.d("tag", "request width=" + mReqWidth + "request height=" + mReqHeight);
        }
        Observable.just(mSrcFilePath)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        Log.d("tag", "doOnSubscribe " + Thread.currentThread().getName());
                        if (null != mCompressListener) {
                            mCompressListener.doStart();
                        }
                    }
                })
                .observeOn(Schedulers.io())//指定后面map的工作线程
                .map(new Func1<String, Bitmap>() {
                    @Override
                    public Bitmap call(String s) {
                        Log.d("tag", "map " + Thread.currentThread().getName());
                        return decodeSampleBitmapFormFile(mSrcFilePath, mReqWidth, mReqHeight);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d("tag", "doOnError" + throwable.getMessage());
                        if (null != mCompressListener) {
                            mCompressListener.doError(throwable);
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())//指定后面subscribe的工作线程
                .subscribe(new Action1<Bitmap>() {
                    @Override
                    public void call(Bitmap bitmap) {
                        Log.d("tag", Thread.currentThread().getName() + "result width=" + bitmap.getWidth() + "height=" + bitmap.getHeight() + "  " + bitmap.getByteCount());
                        if (null != mCompressListener) {
                            mCompressListener.doCompleted(bitmap);
                        }
                        mCompressedFile = new File(getDiskCacheFileDirPath("image") + "/tempeee.jpg");
                        if (null != mCompressedFile) {
                            Log.d("tag", "result mCompressedFile=");
                            saveImage(mCompressedFile, bitmap, 1);
                        }
                    }
                });
    }

    public void compress() {
        int bounds[] = decodeBounds(mSrcFilePath);
        Log.d("tag", "bounds " + bounds[0] + " " + bounds[1]);
        int width = Math.min(bounds[0], bounds[1]);
        int height = Math.max(bounds[0], bounds[1]);
        int[] reqBounds = new int[2];
        if (0 != width * height) {
            reqBounds = buildDisplayResolution(width, height);
        }
        Log.d("tag", "bounds " + reqBounds[0] + " " + reqBounds[1]);
        compress(reqBounds[0], reqBounds[1]);
    }

    private String getDiskCacheFileDirPath(String uniqueName) {
        String state = Environment.getExternalStorageState();
        final String cachePath = (Environment.MEDIA_MOUNTED.equals(state) || !Environment.isExternalStorageRemovable()) ? mContext.getExternalCacheDir().getPath() : mContext.getCacheDir().getPath();
        Log.d("tag", cachePath);
        File dir = new File(cachePath + File.separator + uniqueName);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir.getPath();
    }

    private Bitmap decodeSampleBitmapFormFile(String filename, int reqWidth, int reqHeight) {
        //First decode with inJustDecodeBounds=true to checkout dimensions
        File srcFile = new File(filename);

        if (!srcFile.exists() || !srcFile.isFile()) return null;
        Log.d("tag", "srcFile length: " + srcFile.length() + "reqWidth" + reqWidth + "reqHeight" + reqHeight);
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
            Log.d("tag", "rotate:" + rotate + "scale:" + scale + "  " + firstCompressBitmap.getByteCount());
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


    private int[] decodeBounds(String pathName) {
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

        Log.d("tag", "width=" + width + "height=" + height);
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

            long totalPixels = width * height / inSampleSize;

            Log.d("tag", "inSample" + inSampleSize);
            if ((Math.max(width, height) / Math.min(width, height)) > 16 / 9) {
                final long totalReqPixelsCap = reqWidth * reqHeight * 2;
                while (totalPixels > totalReqPixelsCap) {
                    inSampleSize *= 2;
                    totalPixels /= 2;
                }
            }


        }
        Log.d("tag", "inSample " + inSampleSize);
        return inSampleSize;
    }

    /**
     * 获取图片的缩放比
     *
     * @return
     */
    private float getImageScale(int srcWidth, int srcHeight, int reqWidth, int reqHeight) {
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
    private float getImageRotate(int rotate) {
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


    private void saveImage(File file, Bitmap bitmap, long size) {
        try {
            if (null != file) {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bos);
                Log.d("tag", "saveImage"+file.length());
                bos.flush();
                bos.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d("tag", "1  " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("tag", e.getMessage());
        }
    }

    static class InstanceHolder {
        final static Millstone instance = new Millstone();
    }

    // Common display resolutions for wiki "screen resolution"
    private int[] buildDisplayResolution(int width, int height) {
        float ratio = (float) width / height;
        Log.d("tag", "ratio " + ratio);
        int bounds[] = new int[2];// bounds[0]为宽   bounds[1]为高
        if (ratio >= 3.2f / 4) {                        //5:4 1280*1024
            bounds[0] = 1280;
            bounds[1] = 1280;
            Log.d("tag", " 5:4 1280*1024 ");
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
            Log.d("tag", " 4:3 1024*768 1280*920 ");
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
            Log.d("tag", "16:10 1280*800 1440*900");
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
            Log.d("tag", "  16:9 1920*1080 1280*720");
        } else {
            bounds[0] = 0;
            bounds[1] = 0;
            Log.d("tag", " 0*0");
        }
        return bounds;

    }

    public interface CompressListener {
        void doStart();

        void doCompleted(Bitmap bitmap);

        void doError(Throwable throwable);
    }
}

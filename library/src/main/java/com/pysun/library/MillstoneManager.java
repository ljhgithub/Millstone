package com.pysun.library;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import com.pysun.library.util.ImageScaleUtils;
import com.pysun.library.util.ScreenUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Administrator on 2017/7/3.
 */

public class MillstoneManager {

    private final static String TAG=MillstoneManager.class.getSimpleName();
    private CompressListener mCompressListener;
    private File mCompressedFile;
    private int mReqWidth;
    private int mReqHeight;
    private String mSrcFilePath;
    public MillstoneManager(String mSrcFilePath) {
        this.mSrcFilePath=mSrcFilePath;
    }


    public MillstoneManager setCompressListener(CompressListener compressListener) {
        this.mCompressListener = compressListener;
        return this;
    }
    public MillstoneManager intoFile(File file){
        mCompressedFile=file;
        return this;
    }
    public void compress(int reqWidth, int reqHeight) {
        mReqWidth = reqWidth;
        mReqHeight = reqHeight;
        if (0 == reqWidth * reqHeight || Math.min(reqHeight, reqWidth) < 0) {
            mReqWidth = ScreenUtils.getScreenWidth();
            mReqHeight = ScreenUtils.getScreenHeight();
        }
        Observable.just(mSrcFilePath)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        if (null != mCompressListener) {
                            mCompressListener.doStart();
                        }
                    }
                })
                .observeOn(Schedulers.io())//指定后面map的工作线程
                .map(new Function<String, Bitmap>() {
                    @Override
                    public Bitmap apply(String s) {
                        Bitmap bitmap = ImageScaleUtils.decodeSampleBitmapFormFile(mSrcFilePath, mReqWidth, mReqHeight);
                        if (null != mCompressedFile) {
                            saveImage(mCompressedFile, bitmap, 1);
                        }
                        return bitmap;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())//指定后面subscribe的工作线程
                .subscribe(new Consumer<Bitmap>() {
                    @Override
                    public void accept(Bitmap bitmap) throws Exception {

                        if (null != mCompressListener) {
                            mCompressListener.doCompleted(bitmap);
                        }

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (null != mCompressListener) {
                            mCompressListener.doError(throwable);
                        }
                    }
                });
    }

    public void compress() {
        int bounds[] = ImageScaleUtils.decodeBounds(mSrcFilePath);
        int width = Math.min(bounds[0], bounds[1]);
        int height = Math.max(bounds[0], bounds[1]);
        int[] reqBounds = new int[2];
        if (0 != width * height) {
            reqBounds = ImageScaleUtils.buildDisplayResolution(width, height);
        }
        compress(reqBounds[0], reqBounds[1]);
    }


    private void saveImage(File file, Bitmap bitmap, long size) {
        try {
            if (null != file) {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bos);
                bos.flush();
                bos.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public interface CompressListener {
        void doStart();

        void doCompleted(Bitmap bitmap);

        void doError(Throwable throwable);
    }
}

package com.pysun.millstone;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.pysun.library.Millstone;
import com.pysun.library.MillstoneManager;
import com.pysun.library.util.StorageUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView imageView = (ImageView) findViewById(R.id.imageview);
        File file = new File(StorageUtils.getDiskCacheFileDirPath(this, "image"), "temp.jpg");
        // "/storage/emulated/0/Download/wx.jpg"
        Millstone.get(this).load(StorageUtils.getDiskCacheFileDirPath(this, "image") + File.separator + "wx.jpg").intoFile(file).setCompressListener(new MillstoneManager.CompressListener() {
            @Override
            public void doStart() {
                Log.d("tag", "doStart");
            }

            @Override
            public void doCompleted(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
                Log.d("tag", "doCompleted");
            }

            @Override
            public void doError(Throwable throwable) {
                Log.d("tag", "doError" + throwable.getMessage());
            }
        }).compress();

    }
}

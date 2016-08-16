package com.pysun.millstone;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.pysun.library.Millstone;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       final ImageView imageView= (ImageView) findViewById(R.id.imageview);
        Millstone.getInstance().with(this).load("/storage/emulated/0/DCIM/Camera/map.jpg").setCompressListener(new Millstone.CompressListener() {
            @Override
            public void doStart() {

            }

            @Override
            public void doCompleted(Bitmap bitmap) {
                imageView.setImageBitmap( bitmap);
            }

            @Override
            public void doError(Throwable throwable) {

            }
        }).compress();

    }
}

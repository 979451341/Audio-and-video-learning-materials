package com.example.zth.seven;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    Button button;
    String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,Manifest.permission.CAMERA}, 5);
        }
        button = (Button) findViewById(R.id.btn_record_video);
        File fpath = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/pauseRecordDemo/video");
        if (!fpath.exists()) {
            fpath.mkdirs();
        }
        videoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/pauseRecordDemo/video/";
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent intent = VedioRecordActivity.startRecordActivity(videoPath,MainActivity.this);
                startActivity(intent);
            }
        });
    }
}

package com.example.zth.two.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.example.zth.two.R;
import com.example.zth.two.record.AudioRecorder;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements View.OnClickListener {
    Button start;
    Button pause;
    Button pcmList;
    Button wavList;

    AudioRecorder audioRecorder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO,Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS}, 5);
        }

        init();
        addListener();
    }

    private void addListener() {
        start.setOnClickListener(this);
        pause.setOnClickListener(this);
        pcmList.setOnClickListener(this);
        wavList.setOnClickListener(this);
    }

    private void init() {
        start = (Button) findViewById(R.id.start);
        pause = (Button) findViewById(R.id.pause);
        pcmList = (Button) findViewById(R.id.pcmList);
        wavList = (Button) findViewById(R.id.wavList);
        pause.setVisibility(View.GONE);
        audioRecorder = AudioRecorder.getInstance();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                try {
                    if (audioRecorder.getStatus() == AudioRecorder.Status.STATUS_NO_READY) {
                        //初始化录音
                        String fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
                        audioRecorder.createDefaultAudio(fileName);
                        audioRecorder.startRecord(null);

                        start.setText("停止录音");

                        pause.setVisibility(View.VISIBLE);

                    } else {
                        //停止录音
                        audioRecorder.stopRecord();
                        start.setText("开始录音");
                        pause.setText("暂停录音");
                        pause.setVisibility(View.GONE);
                    }

                } catch (IllegalStateException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.pause:
                try {
                    if (audioRecorder.getStatus() == AudioRecorder.Status.STATUS_START) {
                        //暂停录音
                        audioRecorder.pauseRecord();
                        pause.setText("继续录音");
                        break;

                    } else {
                        audioRecorder.startRecord(null);
                        pause.setText("暂停录音");
                    }
                } catch (IllegalStateException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.pcmList:
                Intent showPcmList = new Intent(MainActivity.this, ListActivity.class);
                showPcmList.putExtra("type", "pcm");
                startActivity(showPcmList);
                break;

            case R.id.wavList:
                Intent showWavList = new Intent(MainActivity.this, ListActivity.class);
                showWavList.putExtra("type", "wav");
                startActivity(showWavList);
                break;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (audioRecorder.getStatus() == AudioRecorder.Status.STATUS_START) {
            audioRecorder.pauseRecord();
            pause.setText("继续录音");
        }

    }

    @Override
    protected void onDestroy() {
        audioRecorder.release();
        super.onDestroy();

    }
}

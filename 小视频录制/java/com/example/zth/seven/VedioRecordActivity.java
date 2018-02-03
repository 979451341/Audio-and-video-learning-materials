package com.example.zth.seven;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class VedioRecordActivity extends AppCompatActivity implements VideoRecordSurface.OnRecordListener{

    //开始按钮
    protected Button btnStart;
    protected FrameLayout frameLayout;
    //播放进度
    protected VideoProgressView videoProgressView;
    //按钮提示
    protected TextView tvTips;
    private int iTime;
    private VideoRecordSurface videoRecordSurface;
    private String videoSavePath;
    public static final String kVideoSavePath = "videoSavePath";

    private OrientationSensorListener listener;
    private SensorManager sm;
    private Sensor sensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vedio_record);

        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        listener = new OrientationSensorListener();
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);

        //必须传递的值
        videoSavePath = getIntent().getStringExtra(kVideoSavePath);
        File file = new File(videoSavePath);
        if (!file.exists()) {
            file.mkdir();
        }
        initView();

        videoRecordSurface = new VideoRecordSurface(this, videoSavePath);
        frameLayout.addView(videoRecordSurface);
        btnStart.setOnTouchListener(new View.OnTouchListener() {
            private float moveY;
            private float moveX;
            Rect rect = new Rect();
            boolean isInner = true;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        //按住事件发生后执行代码的区域
                        tvTips.setVisibility(View.VISIBLE);
                        videoRecordSurface.record(VedioRecordActivity.this,listener.getOrientationHintDegrees());
                        videoProgressView.startProgress(videoRecordSurface.mRecordMaxTime);
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        //移动事件发生后执行代码的区域
                        if (rect.right == 0 && rect.bottom == 0) {
                            btnStart.getFocusedRect(rect);
                        }
                        moveX = event.getX();
                        moveY = event.getY();
                        if (moveY > 0 && moveX > 0 && moveX <= rect.right && moveY <= rect.bottom) {
                            //内
                            isInner = true;
                            if (!"移开取消".equals(tvTips.getText().toString().trim())) {
                                tvTips.setBackgroundColor(Color.TRANSPARENT);
                                tvTips.setTextColor(getResources().getColor(R.color.video_green));
                                tvTips.setText("移开取消");
                            }
                            btnStart.setVisibility(View.INVISIBLE);
                        } else {
                            //外
                            isInner = false;
                            if (!"松开取消".equals(tvTips.getText().toString().trim())) {
                                tvTips.setBackgroundColor(Color.RED);//getResources().getColor(android.R.color.holo_red_dark));
                                tvTips.setTextColor(Color.WHITE);
                                tvTips.setText("松开取消");
                            }
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        //松开事件发生后执行代码的区域
                        tvTips.setVisibility(View.INVISIBLE);
                        videoProgressView.stopProgress();
                        if (iTime <= videoRecordSurface.mRecordMiniTime || (iTime < videoRecordSurface.mRecordMaxTime && !isInner)) {
                            if (isInner) {
                                Toast.makeText(VedioRecordActivity.this, "录制时间太短", Toast.LENGTH_SHORT).show();
                            } else {
                                //
                            }
                            videoRecordSurface.stopRecord();
                            videoRecordSurface.repCamera();
                            btnStart.setVisibility(View.VISIBLE);
                        } else if(iTime < videoRecordSurface.mRecordMaxTime){
                            videoRecordSurface.stop();
                        }
                        break;
                    }
                    default:
                        break;
                }
                return false;
            }
        });

    }

    private void initView() {
        btnStart = (Button) findViewById(R.id.libVideoRecorder_btn_start);
        frameLayout = (FrameLayout) findViewById(R.id.libVideoRecorder_fl);
        videoProgressView = (VideoProgressView) findViewById(R.id.libVideoRecorder_progress);
        tvTips = (TextView) findViewById(R.id.libVideoRecorder_tv_tips);
    }

    @Override
    protected void onStop() {
        super.onStop();
        videoRecordSurface.stopRecord();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onRecordFinish() {
        if (videoProgressView != null) {
            videoProgressView.stopProgress();
        }
        String recordThumbDir = videoRecordSurface.getRecordThumbDir(); //视频首张照片
        String recordMp4Dir = videoRecordSurface.getRecordDir(); //视频播放地址
        startActivity(new Intent(this,VideoPlayActivity.class).putExtra(kVideoSavePath, recordMp4Dir));
    }

    @Override
    protected void onPause() {
        sm.unregisterListener(listener);
        super.onPause();
    }

    @Override
    protected void onResume() {
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    @Override
    public void onRecordProgress(int progress) {
        iTime = progress;
    }


    /**
     * 开启录制
     *
     * @param videoPath 小视频录制后存储位置
     */
    public static Intent startRecordActivity(@NonNull String videoPath, Activity activity) {
        Intent intent = new Intent(activity,VedioRecordActivity.class);
        intent.putExtra(kVideoSavePath, videoPath);
        return intent;
    }

}

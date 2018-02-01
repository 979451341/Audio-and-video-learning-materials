package com.example.jni.muxer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.example.jni.R;

import java.io.IOException;

/**
 * 音视频混合界面
 */
public class MediaMuxerActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    SurfaceView surfaceView;
    Button startStopButton;

    Camera camera;
    SurfaceHolder surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_media_muxer);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "申请权限", Toast.LENGTH_SHORT).show();
            // 申请 相机 麦克风权限
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }

        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        startStopButton = (Button) findViewById(R.id.startStop);

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getTag().toString().equalsIgnoreCase("stop")) {
                    view.setTag("start");
                    ((TextView) view).setText("开始");
                    MediaMuxerThread.stopMuxer();
                    stopCamera();
                    finish();
                } else {
                    startCamera();
                    view.setTag("stop");
                    ((TextView) view).setText("停止");
                    MediaMuxerThread.startMuxer();
                }
            }
        });

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.w("MainActivity", "enter surfaceCreated method");
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.w("MainActivity", "enter surfaceChanged method");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.w("MainActivity", "enter surfaceDestroyed method");
        MediaMuxerThread.stopMuxer();
        stopCamera();

    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        MediaMuxerThread.addVideoFrameData(bytes);
    }

    //----------------------- 摄像头操作相关 --------------------------------------

    /**
     * 打开摄像头
     */
    private void startCamera() {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);

        // 这个宽高的设置必须和后面编解码的设置一样，否则不能正常处理
        parameters.setPreviewSize(1920, 1080);

        try {
            camera.setParameters(parameters);
            camera.setPreviewDisplay(surfaceHolder);
            camera.setPreviewCallback(MediaMuxerActivity.this);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭摄像头
     */
    private void stopCamera() {
        // 停止预览并释放资源
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera = null;
        }
    }


}
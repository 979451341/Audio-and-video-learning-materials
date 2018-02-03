package com.example.zth.seven;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

public class VideoPlayActivity extends AppCompatActivity {

    TextView libPlayVideo_tv_title;
    VideoView videoView;
    private boolean isFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        libPlayVideo_tv_title = (TextView) findViewById(R.id.libPlayVideo_tv_title);
        libPlayVideo_tv_title.setText("视频播放");
        videoView = (VideoView) findViewById(R.id.libPlayVideo_videoView);
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (isFirst) {
                    isFirst = false;
                    Toast.makeText(VideoPlayActivity.this, "播放该视频异常", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        String mVideoPath = getIntent().getStringExtra(VedioRecordActivity.kVideoSavePath);
        File file = new File(mVideoPath);
        if (file.exists()) {
            videoView.setVideoPath(file.getAbsolutePath());
            videoView.start();
            setLoop(file.getAbsolutePath());
        } else {
            Log.e("tag","not found video " + mVideoPath);
        }
    }

    public void setLoop(final String videoPath) {
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                mp.setLooping(true);

            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.setVideoPath(videoPath);
                videoView.start();
            }
        });
    }

    public void videoPlayClick(View view){
        switch (view.getId()){
            case R.id.libPlayVideo_tv_cancel:
                finish();
                break;
            case R.id.libPlayVideo_tv_more:
                Toast.makeText(this, "更多", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}

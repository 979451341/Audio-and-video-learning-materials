package com.example.zth.myapplication.player;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoPlayer {
    private static final String TAG = "TAG";
    private static final long TIMEOUT_US = 10000;
    private IPlayerCallBack callBack;
    private VideoThread videoThread;
    private AudioThread audioThread;
    private boolean isPlaying;
    private String filePath;
    private Surface surface;

    public VideoPlayer(Surface surface, String filePath) {
        this.surface = surface;
        this.filePath = filePath;
    }

    public void setCallBack(IPlayerCallBack callBack) {
        this.callBack = callBack;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void play() {
        isPlaying = true;
        if (videoThread == null) {
            videoThread = new VideoThread();
            videoThread.start();
        }
        if (audioThread == null) {
            audioThread = new AudioThread();
            audioThread.start();
        }
    }

    public void stop() {
        isPlaying = false;
    }

    public void destroy() {
        stop();
        if (audioThread != null) audioThread.interrupt();
        if (videoThread != null) videoThread.interrupt();
    }

    /*将缓冲区传递至解码器
    * 如果到了文件末尾，返回true;否则返回false
    */
    private boolean putBufferToCoder(MediaExtractor extractor, MediaCodec decoder, ByteBuffer[] inputBuffers) {
        boolean isMediaEOS = false;
        int inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            int sampleSize = extractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                isMediaEOS = true;
                Log.v(TAG, "media eos");
            } else {
                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                extractor.advance();
            }
        }
        return isMediaEOS;
    }

    //获取指定类型媒体文件所在轨道
    private int getMediaTrackIndex(MediaExtractor videoExtractor, String MEDIA_TYPE) {
        int trackIndex = -1;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {          
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(MEDIA_TYPE)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }

    //延迟渲染
    private void sleepRender(MediaCodec.BufferInfo audioBufferInfo, long startMs) {
        while (audioBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private class VideoThread extends Thread {
        @Override
        public void run() {
            MediaExtractor videoExtractor = new MediaExtractor();
            MediaCodec videoCodec = null;
            try {
                videoExtractor.setDataSource(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int videoTrackIndex;
            //获取视频所在轨道
            videoTrackIndex = getMediaTrackIndex(videoExtractor, "video/");
            if (videoTrackIndex >= 0) {
                MediaFormat mediaFormat = videoExtractor.getTrackFormat(videoTrackIndex);
                int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                float time = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000000;
                callBack.videoAspect(width, height, time);
                videoExtractor.selectTrack(videoTrackIndex);
                try {
                    videoCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
                    videoCodec.configure(mediaFormat, surface, null, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (videoCodec == null) {
                Log.v(TAG, "MediaCodec null");
                return;
            }
            videoCodec.start();

            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = videoCodec.getInputBuffers();
//            ByteBuffer[] outputBuffers = videoCodec.getOutputBuffers();
            boolean isVideoEOS = false;

            long startMs = System.currentTimeMillis();
            while (!Thread.interrupted()) {
                if (!isPlaying) {
                    continue;
                }
                //将资源传递到解码器
                if (!isVideoEOS) {
                    isVideoEOS = putBufferToCoder(videoExtractor, videoCodec, inputBuffers);
                }
                int outputBufferIndex = videoCodec.dequeueOutputBuffer(videoBufferInfo, TIMEOUT_US);
                switch (outputBufferIndex) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.v(TAG, "format changed");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.v(TAG, "超时");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        //outputBuffers = videoCodec.getOutputBuffers();
                        Log.v(TAG, "output buffers changed");
                        break;
                    default:
                        //直接渲染到Surface时使用不到outputBuffer
                        //ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        //延时操作
                        //如果缓冲区里的可展示时间>当前视频播放的进度，就休眠一下
                        sleepRender(videoBufferInfo, startMs);
                        //渲染
                        videoCodec.releaseOutputBuffer(outputBufferIndex, true);
                        break;
                }

                if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.v(TAG, "buffer stream end");
                    break;
                }
            }//end while
            videoCodec.stop();
            videoCodec.release();
            videoExtractor.release();
        }
    }

    private class AudioThread extends Thread {
        private int audioInputBufferSize;

        private AudioTrack audioTrack;

        @Override
        public void run() {
            MediaExtractor audioExtractor = new MediaExtractor();
            MediaCodec audioCodec = null;
            try {
                audioExtractor.setDataSource(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = audioExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i);
                    int audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int audioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate,
                            (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT);
                    int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    audioInputBufferSize = minBufferSize > 0 ? minBufferSize * 4 : maxInputSize;
                    int frameSizeInBytes = audioChannels * 2;
                    audioInputBufferSize = (audioInputBufferSize / frameSizeInBytes) * frameSizeInBytes;
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            audioSampleRate,
                            (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT,
                            audioInputBufferSize,
                            AudioTrack.MODE_STREAM);
                    audioTrack.play();
                    Log.v(TAG, "audio play");
                    //
                    try {
                        audioCodec = MediaCodec.createDecoderByType(mime);
                        audioCodec.configure(mediaFormat, null, null, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            if (audioCodec == null) {
                Log.v(TAG, "audio decoder null");
                return;
            }
            audioCodec.start();
            //
            final ByteBuffer[] buffers = audioCodec.getOutputBuffers();
            int sz = buffers[0].capacity();
            if (sz <= 0)
                sz = audioInputBufferSize;
            byte[] mAudioOutTempBuf = new byte[sz];

            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = audioCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = audioCodec.getOutputBuffers();
            boolean isAudioEOS = false;
            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()) {
                if (!isPlaying) {
                    continue;
                }
                if (!isAudioEOS) {
                    isAudioEOS = putBufferToCoder(audioExtractor, audioCodec, inputBuffers);
                }
                //
                int outputBufferIndex = audioCodec.dequeueOutputBuffer(audioBufferInfo, TIMEOUT_US);
                switch (outputBufferIndex) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.v(TAG, "format changed");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.v(TAG, "超时");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        outputBuffers = audioCodec.getOutputBuffers();
                        Log.v(TAG, "output buffers changed");
                        break;
                    default:
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        //延时操作
                        //如果缓冲区里的可展示时间>当前视频播放的进度，就休眠一下
                        sleepRender(audioBufferInfo, startMs);
                        if (audioBufferInfo.size > 0) {
                            if (mAudioOutTempBuf.length < audioBufferInfo.size) {
                                mAudioOutTempBuf = new byte[audioBufferInfo.size];
                            }
                            outputBuffer.position(0);
                            outputBuffer.get(mAudioOutTempBuf, 0, audioBufferInfo.size);
                            outputBuffer.clear();
                            if (audioTrack != null)
                                audioTrack.write(mAudioOutTempBuf, 0, audioBufferInfo.size);
                        }
                        //
                        audioCodec.releaseOutputBuffer(outputBufferIndex, false);
                        break;
                }

                if ((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.v(TAG, "buffer stream end");
                    break;
                }
            }//end while
            audioCodec.stop();
            audioCodec.release();
            audioExtractor.release();
            audioTrack.stop();
            audioTrack.release();
        }

    }
}

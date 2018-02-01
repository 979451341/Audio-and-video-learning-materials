package com.example.zth.two.record;

/**
 * Created by HXL on 16/8/11.
 * 获取录音的音频流,用于拓展的处理
 */
public interface RecordStreamListener {
    void recordOfByte(byte[] data, int begin, int end);
}

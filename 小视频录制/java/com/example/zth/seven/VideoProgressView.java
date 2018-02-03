package com.example.zth.seven;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;


/**
 * Created by zwj on 2016/7/11.
 * 自定义视频录制进度条
 */
public class VideoProgressView extends View {
    private Paint mProgressPaint;//进度条的画笔
    private int mProgress = 0;//进度条
    private int mHeight;
    private int mWidth;
    private Handler mHandler;

    public VideoProgressView(Context context) {
        this(context, null);
    }

    public VideoProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public VideoProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.TRANSPARENT);
        mHandler = new Handler(Looper.getMainLooper());
    }


    private Paint getDefaultPaint() {
        if (mProgressPaint == null) {
            mProgressPaint = new Paint();
            mProgressPaint.setAntiAlias(true);
            mProgressPaint.setColor(Color.GREEN);
            mProgressPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mProgressPaint.setStrokeWidth(mHeight * 2);
        }
        return mProgressPaint;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = measureWidth(widthMeasureSpec);
        mHeight = measureHeight(heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    private int measureWidth(int widthMeasureSpec) {
        int width;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            width = widthSize;
        } else {
            width = 200;
            if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(width, widthSize);
            }
        }
        return width;
    }

    private int measureHeight(int heightMeasureSpec) {
        int height;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            height = heightSize;
        } else {
            height = 50;
            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(heightMeasureSpec, height);
            }
        }
        return height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float startX = mProgress;
        float startY = mHeight;
        float stopX = mWidth - mProgress;
        float stopY = mHeight;
        canvas.drawLine(startX, startY, stopX, stopY, getDefaultPaint());
    }

    /**
     * 设置进度条
     *
     * @param second 秒
     */
    public void startProgress(final int second) {
        isProgress = true;
        setVisibility(VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = mWidth / 2;
                int sleepTime = second * 1000 / count;
                for (int i = 0; i < count; i++) {

                    if ((i == count - 1) || !isProgress) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setVisibility(INVISIBLE);
                            }
                        });
                        break;
                    } else {
                        mProgress = i;
                        postInvalidate();
                        SystemClock.sleep(sleepTime);
                    }


                }
            }
        }).start();
    }

    private boolean isProgress;

    public void stopProgress() {
        isProgress = false;
    }

}
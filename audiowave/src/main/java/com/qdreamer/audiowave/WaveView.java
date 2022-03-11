package com.qdreamer.audiowave;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;

public class WaveView extends LinearLayout {

    // 一个屏幕宽度绘制波形数量
    public static final int WAVE_COUNT = 640;
    public static final int WAVE_COLOR = Color.parseColor("#FF00BCD4");

    private final RecyclerView mRcyWave;
    private final WaveAdapter mWaveAdapter;

    private boolean enableScroll = false;

    @SuppressLint("ClickableViewAccessibility")
    public WaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        int screenWidth = getScreenWidth(context);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveView);
        int waveColor = typedArray.getColor(R.styleable.WaveView_waveColor, WAVE_COLOR);
        int waveCount = typedArray.getInt(R.styleable.WaveView_waveCount, WAVE_COUNT);
        if (waveCount < screenWidth / 10) {
            waveCount = WAVE_COUNT;
        }
        typedArray.recycle();

        View convertView = View.inflate(context, R.layout.layout_wave, null);

        mRcyWave = convertView.findViewById(R.id.rcyWave);
        mRcyWave.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        mWaveAdapter = new WaveAdapter(screenWidth, waveCount, waveColor);
        mWaveAdapter.bindToRecyclerView(mRcyWave);
        mWaveAdapter.setNewData(null);
        mRcyWave.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return !enableScroll;
            }
        });

        // 添加 footer view 用于绘制波形的时候 scrollToPosition 滚动到最后一条波形
        View footerView = new View(getContext());
        ViewGroup.LayoutParams paramsFooter = new ViewGroup.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT);
        footerView.setLayoutParams(paramsFooter);
        mWaveAdapter.setFooterView(footerView, 0, HORIZONTAL);

        addView(convertView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void stop() {
        mRcyWave.postDelayed(new Runnable() {
            @Override
            public void run() {
                enableScroll = true;
                mRcyWave.scrollToPosition(0);
            }
        }, 100);
    }

    public void clear() {
        mWaveAdapter.setNewData(null);
        mRcyWave.scrollToPosition(0);
    }

    private float lastVolume = 0;

    public void feedAudioData(byte[] data) {
        if (data == null || data.length <= 0) return;
        if (enableScroll) { // 绘制时不让拖动波形
            enableScroll = false;
        }
        float volume = (float) calculateVolume(data);
        if (volume > lastVolume) {
            Log.i("WaveView", "feedAudioData: " + volume);
            lastVolume = volume;
        }
        int index;
        WaveItemModel model;
        if (mWaveAdapter.getData().isEmpty() || mWaveAdapter.needToNext()) {
            index = mWaveAdapter.getData().size();
            model = new WaveItemModel();
            mWaveAdapter.addData(model);
        } else {
            index = mWaveAdapter.getData().size() - 1;
            model = mWaveAdapter.getData().get(index);
        }
        model.mCharList.add(volume);
        mWaveAdapter.setData(index, model);
        mRcyWave.scrollToPosition(mWaveAdapter.getItemCount() - 1);
    }

    /**
     * 用于 buffer 传的是奇数时，与下一个 buffer 的第一位匹配，因为 16k/16bit/单通道的数据中，每个采样点是 2 个字节
     */
    private Byte lastVoice = null;

    private double calculateVolume(byte[] buffer) {
        double volume = 0.0;
        DecimalFormat df = new DecimalFormat("######0");
        int i = 0;
        while (i < buffer.length) {
            int v1;
            int v2;
            if (lastVoice == null) {
                if (i + 1 == buffer.length) {
                    lastVoice = buffer[buffer.length - 1];
                    break;
                }
                v1 = buffer[i] & 0xFF;
                v2 = buffer[i + 1] & 0xFF;
                i += 2;
            } else {
                v1 = lastVoice;
                v2 = buffer[i] & 0xFF;
                i += 1;
                lastVoice = null;
            }
            int temp = v1 + (v2 << 8);  // 小端
            if (temp >= 0x8000) {
                temp = 0xffff - temp;
            }
            volume += Math.abs(temp);
        }
        volume /= 20 * 16;
        volume /= buffer.length / 160 + 1;
//        volume = Math.min(volume, 1000.0);
        try {
            return Double.parseDouble(df.format(volume));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int getScreenWidth(Context context) {
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        ((Activity)(context)).getWindowManager().getDefaultDisplay().getMetrics(localDisplayMetrics);
        return localDisplayMetrics.widthPixels;
    }

}

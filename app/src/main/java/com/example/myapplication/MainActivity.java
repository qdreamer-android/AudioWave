package com.example.myapplication;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.qdreamer.audiowave.WaveView;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private static final int MESSAGE_AUDIO = 0x01;

    private WaveView waveView;
    private Button btnRecording;

    private WaveHandler mHandler;

    private int bufferSize;
    private AudioRecord mAudioRecord;

    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        waveView = findViewById(R.id.waveView);
        btnRecording = findViewById(R.id.btnRecording);
        btnRecording.setOnClickListener(v -> {
            if (isRecording) {  // 停止录音
                MainActivityPermissionsDispatcher.stopRecordingWithPermissionCheck(this);
            } else {    // 开始录音
                waveView.clear();   // 清除所有波形数据
                MainActivityPermissionsDispatcher.startRecordingWithPermissionCheck(this);
            }
        });

        mHandler = new WaveHandler(this);
        MainActivityPermissionsDispatcher.initAudioRecordWithPermissionCheck(this);
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    public void initAudioRecord() {
        int sample = 16000;
        int channel = AudioFormat.CHANNEL_IN_MONO;
        int format = AudioFormat.ENCODING_PCM_16BIT;
        bufferSize = AudioRecord.getMinBufferSize(sample, channel, format);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sample, channel, format, bufferSize);
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    public void startRecording() {
        if (mAudioRecord != null && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                mAudioRecord.startRecording();
                isRecording = true;
                btnRecording.setText(getString(R.string.stop_recording));

                readRecording();
            }
        }
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    public void stopRecording() {
        if (mAudioRecord != null && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                mAudioRecord.stop();
                isRecording = false;
                btnRecording.setText(getString(R.string.start_recording));

                waveView.stop();    // 停止将波形移动到最前面，并且可以拖动
            }
        }
    }

    private void readRecording() {
        Thread thread = new Thread(() -> {
            while (isRecording && mAudioRecord != null && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    byte[] buffer = new byte[bufferSize];
                    int len = mAudioRecord.read(buffer, 0, bufferSize);
                    if (len > 0) {
                        if (len == bufferSize) {
                            mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_AUDIO, buffer));
                        } else {
                            mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_AUDIO, Arrays.copyOfRange(buffer, 0, len)));
                        }
                    }
                }
            }
        });
        thread.start();
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    public void onDeniedStoragePermission() {
        Toast.makeText(this, "没有录音权限", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        stopRecording();
        if (mAudioRecord != null) {
            mAudioRecord.release();
        }
        super.onDestroy();
    }

    private static class WaveHandler extends Handler {

        private final WeakReference<MainActivity> weakReference;

        public WaveHandler(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (weakReference.get() == null) {
                return;
            }
            if (msg.what == MESSAGE_AUDIO) {
                // 送入音频数据绘制波形，一次 feed 对应一条波形
                weakReference.get().waveView.feedAudioData((byte[]) msg.obj);
            }
        }
    }
}
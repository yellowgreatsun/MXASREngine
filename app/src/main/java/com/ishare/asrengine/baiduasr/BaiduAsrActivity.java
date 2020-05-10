package com.ishare.asrengine.baiduasr;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.ishare.asrengine.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaiduAsrActivity extends Activity {

    private static final String TAG = "HYY ";

    private Button btnRecognize;
    private Button btnStop;

    private EventManager asrManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initView();
        initRecognizer();
        loadOfflineEngine();
        initListener();
    }

    private void initView() {

        btnRecognize = findViewById(R.id.btn_recognize);
        btnStop = findViewById(R.id.btn_stop);
    }

    private void initListener() {

        btnRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListen();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopListen();
            }
        });
    }

    private void initRecognizer() {

        asrManager = EventManagerFactory.create(this, "asr");
        asrManager.registerListener(eventListener);
    }

    private void releaseRecognizer() {

        cancelListen();
        unloadOfflineEngine();
        asrManager.unregisterListener(eventListener);
    }

    private void loadOfflineEngine() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(SpeechConstant.DECODER, 2);
        params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets://baidu_speech_grammar.bsg");
        asrManager.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, new JSONObject(params).toString(), null, 0, 0);
    }

    private void unloadOfflineEngine() {
        asrManager.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE, null, null, 0, 0);
    }

    @Override
    protected void onDestroy() {
        releaseRecognizer();
        super.onDestroy();
    }

    private void startListen(){

        JSONObject params=new JSONObject();
        try {
            params.put(SpeechConstant.PID, 1536);  //语言、模型、是否需要在线语义
            params.put(SpeechConstant.DECODER, 2);  //0，在线；2，离线
            params.put(SpeechConstant.VAD, SpeechConstant.VAD_TOUCH);  //dnn，开启vad；touch，不开启vad，但会60s限制
            //params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 0);  //静音超时断句及长语音
            //params.put(SpeechConstant.IN_FILE, "");  //输入文件路径，文件长度不超过3分钟
            //params.put(SpeechConstant.OUT_FILE, 0);  //识别过程产生的录音文件, 该参数需要开启ACCEPT_AUDIO_DATA后生效
            params.put(SpeechConstant.ACCEPT_AUDIO_DATA, false);  //是否需要语音音频数据回调，开启后有CALLBACK_EVENT_ASR_AUDIO事件
            params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
            params.put(SpeechConstant.NLU, "enable");  //本地语义解析设置
            params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets://baidu_speech_grammar.bsg");  //本地语义解析文件路径
            //params.put(SpeechConstant.DISABLE_PUNCTUATION, false);  //在选择PID为长句（输入法模式）的时候，是否禁用标点符号
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, " params=" + params.toString());
        asrManager.send(SpeechConstant.ASR_START, params.toString(), null, 0, 0);
    }

    private void stopListen(){
        asrManager.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0);
    }

    private void cancelListen(){
        asrManager.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
    }

    EventListener eventListener = new EventListener() {

        @Override
        public void onEvent(String name, String params, byte[] bytes, int offset, int length) {
            if (length > 0)
                Log.i(TAG, " name=" + name + " \nparams=" + params + "\ndata=" + new String(bytes, offset, length) + " \noffset=" + offset + " \nlength=" + length);
            else
                Log.i(TAG, " name=" + name + " \nparams=" + params + " \noffset=" + offset + " \nlength=" + length);
        }
    };
}

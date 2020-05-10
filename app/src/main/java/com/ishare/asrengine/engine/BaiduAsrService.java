package com.ishare.asrengine.engine;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class BaiduAsrService extends RecognitionService {

    private static final String TAG = "BaiduAsrService ";

    private EventManager asrManager;

    @Override
    public void onCreate() {
        super.onCreate();
        initRecognizer();
        loadOfflineEngine();
    }

    @Override
    public void onDestroy() {
        // 在onDestroy还是onUnbind待思考
        releaseRecognizer();
        super.onDestroy();
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

    RecognitionService.Callback mRecognitionListener;

    @Override
    protected void onStartListening(Intent recognizerIntent, Callback listener) {
        this.mRecognitionListener=listener;
        startListen();
    }

    @Override
    protected void onCancel(Callback listener) {
        cancelListen();
    }

    @Override
    protected void onStopListening(Callback listener) {
        stopListen();
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
            params.put(SpeechConstant.ACCEPT_AUDIO_DATA, true);  //是否需要语音音频数据回调，开启后有CALLBACK_EVENT_ASR_AUDIO事件
            params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, true);
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
        public void onEvent(String name, String params, byte[] data, int offset, int length) {

            Log.i(TAG, " name=" + name + " \nparams=" + params + " \noffset=" + offset + " \nlength=" + length);

            switch (name) {
                // 引擎准备就绪，可以开始说话
                case SpeechConstant.CALLBACK_EVENT_ASR_READY:
                    try {
                        mRecognitionListener.readyForSpeech(new Bundle());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;

                // 检测到说话开始
                case SpeechConstant.CALLBACK_EVENT_ASR_BEGIN:
                    try {
                        mRecognitionListener.beginningOfSpeech();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;

                // 检测到说话结束
                case SpeechConstant.CALLBACK_EVENT_ASR_END:
                    try {
                        mRecognitionListener.endOfSpeech();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;

                // 识别结果
                case SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL:
                    try {
                        JSONObject paramsJson = new JSONObject(params);
                        String best_result = paramsJson.getString("best_result");
                        String result_type = paramsJson.getString("result_type");

                        Bundle result = new Bundle();
                        ArrayList<String> resultList = new ArrayList<>();
                        resultList.add(best_result);
                        result.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, resultList);
                        if ("final_result".equals(result_type)) {
                            if(paramsJson.has("results_nlu")){
                                result.putString("results_nlu",paramsJson.getString("results_nlu"));
                            }
                            mRecognitionListener.results(result);
                        } else if ("partial_result".equals(result_type)) {
                            mRecognitionListener.partialResults(result);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                // 识别结束（可能含有错误信息）。最终识别的文字结果在ASR_PARTIAL事件中
                case SpeechConstant.CALLBACK_EVENT_ASR_FINISH:
                    try {
                        JSONObject paramsJson = new JSONObject(params);
                        int error = paramsJson.getInt("error");
                        if(error!=0){
                            mRecognitionListener.error(error);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                // PCM音频片段回调。必须输入ACCEPT_AUDIO_DATA 参数激活
                case SpeechConstant.CALLBACK_EVENT_ASR_AUDIO:
                    try {
                        mRecognitionListener.bufferReceived(data);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;

                // 当前音量回调。必须输入ACCEPT_AUDIO_VOLUME参数激活
                case SpeechConstant.CALLBACK_EVENT_ASR_VOLUME:
                    break;

                // 识别结束，资源释放
                case SpeechConstant.CALLBACK_EVENT_ASR_EXIT:
                    break;

                // 离线模型加载成功回调
                case SpeechConstant.CALLBACK_EVENT_ASR_LOADED:
                    break;

                // 离线模型卸载成功回调
                case SpeechConstant.CALLBACK_EVENT_ASR_UNLOADED:
                    break;
            }
        }
    };
}

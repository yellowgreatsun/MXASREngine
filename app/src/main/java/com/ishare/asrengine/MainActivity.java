package com.ishare.asrengine;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends Activity implements RecognitionListener {

    private static final String TAG = "HYY ";

    private SpeechRecognizer mSpeechRecognizer;

    private Button btnRecognize;
    private Button btnStop;
    private Intent recognizerIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initRecognizer();
        initListener();
    }

    private void initView() {

        btnRecognize = findViewById(R.id.btn_recognize);
        btnStop = findViewById(R.id.btn_stop);
    }

    private void initRecognizer() {

        List<ResolveInfo> list = this.getPackageManager().queryIntentServices(
                new Intent(RecognitionService.SERVICE_INTERFACE), 0);
        Log.i(TAG, " list.size=" + list.size());
//        Toast.makeText(this, "Recognition Available"+ list.size(), Toast.LENGTH_SHORT).show();

        for (ResolveInfo resolveInfo : list) {
//            Toast.makeText(this, " resolveInfo=" + resolveInfo.toString(), Toast.LENGTH_SHORT).show();
            Log.i(TAG, " resolveInfo=" + resolveInfo.toString());
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this,
                    new ComponentName("com.xxun.watch.xunbrain.y1", "com.xxun.watch.xunbrain.service.AsrEngineService"));
            mSpeechRecognizer.setRecognitionListener(this);
        } else {
            Toast.makeText(this, "Recognition UnAvailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void initListener() {

        btnRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpeechRecognizer == null) {
                    Toast.makeText(MainActivity.this, "Recognition UnAvailable", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startRecognize();
                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpeechRecognizer == null) {
                    Toast.makeText(MainActivity.this, "Recognition UnAvailable", Toast.LENGTH_SHORT).show();
                    return;
                }
                mSpeechRecognizer.stopListening();
            }
        });
    }

    private void startRecognize() {

        initRecognizerIntent();
        mSpeechRecognizer.startListening(recognizerIntent);
    }

    @Override
    protected void onDestroy() {
        mSpeechRecognizer.destroy();
        super.onDestroy();
    }

    /*---------------------------------- Permission回调 start ------------------------------------*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PERMISSION_GRANTED) {
                startRecognize();
            }
        }
    }

    /*--------------------------------- Permission回调 start -----------------------------------*/


    /*----------------------------- RecognitionListener回调 start -------------------------------*/

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.i(TAG, " onReadyForSpeech params=" + params.toString());
    }

    @Override
    public void onBeginningOfSpeech() {
        Toast.makeText(this, "onBeginningOfSpeech", Toast.LENGTH_SHORT).show();

        Log.i(TAG, " onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        Log.i(TAG, " onRmsChanged rmsdB=" + rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
//        Log.i(TAG, " onBufferReceived buffer.length=" + buffer.length);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(TAG, " onEndOfSpeech ");
    }

    @Override
    public void onError(int error) {
        Log.i(TAG, " onError error=" + error);
    }

    @Override
    public void onResults(Bundle results) {
//        Log.i(TAG, " onResults results=" + results.toString());

        List<String> asrResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (asrResults != null) {
            for (String asrResult : asrResults) {
                Toast.makeText(this, "onResults asrResult=" + asrResult, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "onResults asrResult=" + asrResult);
            }
        }

        String nluResults=results.getString("results_nlu");
        Log.i(TAG, "onResults results_nlu=" + nluResults);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
//        Log.i(TAG, " onPartialResults partialResults=" + partialResults.toString());

        List<String> asrPartialResults = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (asrPartialResults != null) {
            for (String asrPartialResult : asrPartialResults) {
                Log.i(TAG, "onPartialResults asrPartialResults=" + asrPartialResult);
            }
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.i(TAG, " onEvent eventType=" + eventType + " params=" + params.toString());
    }

    /*----------------------------- RecognitionListener回调 end -------------------------------*/

    /*--------------------------------------- test -----------------------------------------*/
    private void initRecognizerIntent() {
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }
}

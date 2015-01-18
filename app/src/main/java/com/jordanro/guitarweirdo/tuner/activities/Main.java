package com.jordanro.guitarweirdo.tuner.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import com.jordanro.guitarweirdo.tuner.R;
import com.jordanro.guitarweirdo.tuner.api.ServerApi;
import com.jordanro.guitarweirdo.tuner.uiUtil.AnimationFactory;
import com.jordanro.guitarweirdo.tuner.audioUtil.TunerEngine;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import android.text.format.Time;
import android.widget.Toast;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main extends Activity {
    private static final double[] FREQUENCIES = { 261.63, 277.18, 293.66, 311.13, 329.63, 349.23, 369.99, 392.00, 415.30, 440.00, 466.16, 493.88, 523.25, 554.37, 587.33, 622.25, 659.25, 698.46, 739.99, 783.99, 830.61, 880.00, 932.33, 987.77, 1046.50, 1108.73, 1174.66, 1244.51, 1318.51, 1396.91, 1479.98, 1567.98, 1661.22, 1760, 1864.66, 1975.53, 2093};
    private static final String[] NAME        = {"","C",    "C#",   "D",    "D#",   "E",    "F",    "F#",    "G",   "G#"   , "A"  ,  "A#",   "B",   "C",    "C#",    "D",   "D#",   "E",    "F",   "F#",    "G",   "G#",    "A",    "A#",   "B",    "C",    "C#",     "D",     "D#",    "E",     "F",     "F#",    "G",     "G#",   "A",   "A#",    "B",    "C", ""};
    ArrayList<Integer> noteslist = new ArrayList<Integer>();

    TunerEngine tuner;
    final Handler mHandler = new Handler();
    final Runnable callback = new Runnable() {
        public void run() {
            updateUI(tuner.currentFrequency);
        }
    };

    TextView prev_note, current_note, next_note;
	View animator;
    TextView leftV,centerV,rightV;
    boolean firstUpdate;
    public static final int DEFAULT_TRANSFORM_DURATION = 150;
    public static final int DEFAULT_ALPHA_DURATION = 70;

    ImageView toggleTuner;
    boolean firstTime = true;

    private SpeechRecognizer sr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getMusic();

        animator = findViewById(R.id.gauge_background);

		leftV = (TextView) findViewById(R.id.left_note);
		centerV = (TextView) findViewById(R.id.center_note);
		rightV = (TextView) findViewById(R.id.right_note);
        defaultColor = rightV.getCurrentTextColor();

        prev_note = (TextView) findViewById(R.id.prev_note);
        current_note = (TextView) findViewById(R.id.current_note);
        next_note = (TextView) findViewById(R.id.next_note);

        initSpeechRecognition();
    }

    public void onStart(){
        super.onStart();
        initLayoutState();
    }

    @Override
    public void onPause(){
        if(tuner != null && tuner.isAlive()){
            tuner.close();
        }
        super.onPause();
    }

    private void getMusic() {
        pullfromServer(0);
    }

    int defaultColor;
    private void initLayoutState() {
        animator.getBackground().setAlpha(127);
        firstUpdate = true;
    }

    private void initTuner() {
        toggleTuner = (ImageView)findViewById(R.id.toggle_tuner);
        toggleTuner.setOnClickListener(new View.OnClickListener() {
            boolean start = true;
            public void onClick(View view) {
                toggleTunerState(start);
                start = !start;

                if (firstTime) {
                    TextView instructions = (TextView) findViewById(R.id.instructions);
                    instructions.setVisibility(View.GONE);
                    current_note.setText(NAME[noteslist.get(0) + 1]);
                    next_note.setText(NAME[noteslist.get(1)+1]);
                    prev_note.setText("");
                }
            }
        });

    }

    public void initSpeechRecognition() {
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {}

            @Override
            public void onBeginningOfSpeech() {
                System.out.println("SPEECH BEGAN");
            }

            @Override
            public void onRmsChanged(float v) {}

            @Override
            public void onBufferReceived(byte[] bytes) {}

            @Override
            public void onEndOfSpeech() {
                System.out.println("SPEECH ENDED");
            }

            @Override
            public void onError(int i) {}

            @Override
            public void onResults(Bundle bundle) {
                System.out.println("SPEECH RESULTS");
            }

            @Override
            public void onPartialResults(Bundle bundle) {}

            @Override
            public void onEvent(int i, Bundle bundle) {}
        });
        System.out.println("start listening to speech");
    }

    public void toggleTunerState(boolean start){
        if(start){
            try {
                tuner = new TunerEngine(mHandler,callback);
                tuner.start();
                toggleTuner.setImageResource(R.drawable.stop);
                animator.startAnimation(fadeIn50);
                animator.getBackground().setAlpha(255);
                if(currentLayer != null){
                    currentLayer.setTextColor(0XFFFFCC33);
                }
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                sr.startListening(intent);
                System.out.println("started listening to speech");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            tuner.close();
            animator.startAnimation(fadeOut50);
            animator.getBackground().setAlpha(156);
            if(currentLayer != null){
                currentLayer.setTextColor(0XCCFFCC33);
            }
            toggleTuner.setImageResource(R.drawable.start);
            sr.cancel();
            System.out.println("stop listening to speech");
        }

    }
    Animation fadeIn  = AnimationFactory.getAnimation(AnimationFactory.FADE_IN,DEFAULT_ALPHA_DURATION);
    Animation fadeIn50  = AnimationFactory.getAnimation(AnimationFactory.FADE_IN_50,300);
    Animation fadeOut  = AnimationFactory.getAnimation(AnimationFactory.FADE_OUT,10);
    Animation fadeOut50  = AnimationFactory.getAnimation(AnimationFactory.FADE_OUT_50,300);
    int previousOffset = 0;
    int currentFrameIndex = 1;

    // get music and to populate noteslists
    private void pullfromServer(int position) {
        ServerApi.get("compose/" + position, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                // pull in notes from the server
                try {
                    JSONArray measures = response.getJSONArray("tab");
                    for (int i = 0; i < measures.length(); i++) {
                        JSONArray measure = measures.getJSONArray(i);
                        for (int j = 0; j < measure.length(); j++) {
                            JSONObject note = measure.getJSONObject(j);
                            noteslist.add((int)note.get("note"));
                        }
                    }
                    System.out.println(noteslist.toString());
                    initTuner();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
                super.onFailure(statusCode, headers, errorResponse, e);
                System.out.println("SERVER ERROR" + errorResponse);
            }
        });

    }

    // Send index + new note- both from myo motion (delete all) and the android app
    private void sendtoServer(String index) {
        RequestParams params = new RequestParams();
        params.put("index", index); //note: this needs to be the first note of a measure
        ServerApi.post("restart/", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                // pull in notes from the server
                System.out.println(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
                super.onFailure(statusCode, headers, errorResponse, e);
                System.out.println("SERVER ERROR" + errorResponse);
            }
        });

    }

    public void updateUI(double frequency){
        if (firstUpdate) {
            leftV.setVisibility(View.VISIBLE);
            centerV.setVisibility(View.VISIBLE);
            rightV.setVisibility(View.VISIBLE);
            firstUpdate = false;
        }

        // show tuner thing
        int note = closestNote(frequency);
        double matchFreq = FREQUENCIES[note];
        int offset = 0;
        if (frequency < matchFreq) {
            double prevFreq = (note == 0) ? 185 : FREQUENCIES[note - 1];
            offset = (int) (-(frequency - matchFreq) / (prevFreq - matchFreq) / 0.2);
        } else {
            double nextFreq = (note == FREQUENCIES.length - 1) ? 1662 : FREQUENCIES[note + 1];
            offset = (int) ((frequency - matchFreq) / (nextFreq - matchFreq) / 0.2);
        }
        int frameShift = note - currentFrameIndex;
        if (note > currentFrameIndex) {
            currentFrameIndex = note;
        } else if (note < currentFrameIndex) {
            currentFrameIndex = note;
        }

        // Check if the note played is correct
        double ideal_frequency = FREQUENCIES[noteslist.get(0)];
        if (frequency < ideal_frequency + 25 || frequency < ideal_frequency - 25) {
            if (prev_note.getText() != "") { noteslist.remove(0); }
            // switch notes
            next_note.setText(NAME[noteslist.get(2)+1]);
            prev_note.setText(NAME[noteslist.get(0)+1]);
            current_note.setText(NAME[noteslist.get(1) + 1]);
        }

        // check for pause and new note
        if (frequency >= 180 && frequency <= 1600 && (frequency < ideal_frequency + 200 || frequency > ideal_frequency - 200)) {
            // sendToServer(index, new_note);
            current_note.setTextColor(getResources().getColor(R.color.red));
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){
                public void run() {
                    current_note.setTextColor(getResources().getColor(R.color.white));
                    System.out.println(NAME[currentFrameIndex+1]);
                    //current_note.setText();
                }
            }, 500);
        }


        moveGauge(frameShift, offset);
    }

    TextView currentLayer;

    public void moveGauge(int frameShift,int offset){
        boolean stillTuned = frameShift == 0 && offset == 0;
        if(currentLayer != null && !stillTuned){
            currentLayer.setTextColor(defaultColor);
            currentLayer = null;
        }
        if(frameShift != 0){
            leftV.setText(NAME[currentFrameIndex]);
            centerV.setText(NAME[currentFrameIndex+1]);
            rightV.setText(NAME[currentFrameIndex+2]);


        }
        int currentOffset = offset*15;
        if(currentOffset == 0 && (frameShift != 0 || currentLayer == null)){
            currentLayer = centerV;
            currentLayer.setTextColor(0XFFFFCC33);
            currentLayer.startAnimation(fadeIn);
        }

        animator.scrollBy(-1*previousOffset,0);
        animator.scrollBy(currentOffset,0);
        previousOffset = currentOffset;
    }

    private static int closestNote(double hz) {
        double minDist = Double.MAX_VALUE;
        int minFreq = -1;
        for ( int i = 0; i < FREQUENCIES.length; i++ ) {
            double dist = Math.abs(FREQUENCIES[i]-hz);
            if ( dist < minDist ) {
                minDist=dist;
                minFreq=i;
            }
        }
//        minFreq = minFreq == 13 ? 1 : minFreq;
        return minFreq;
    }

    // arpeggio, delete
    private void myoMotion() {
        ServerApi.requestArpeggio();
    }

}
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
import android.util.Log;
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
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.scanner.ScanActivity;

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
    ArrayList<ArrayList<Integer>> noteslist = new ArrayList<ArrayList<Integer>>();

    TunerEngine tuner;
    final Handler mHandler = new Handler();
    final Runnable callback = new Runnable() {
        public void run() {
            updateUI(tuner.currentFrequency);
            System.out.println(tuner.currentFrequency);
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

    public int current_measure_index;
    public int current_note_index;

    Hub hub;

    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getMusic();
        sendtoServer(0,0);

        animator = findViewById(R.id.gauge_background);

		leftV = (TextView) findViewById(R.id.left_note);
		centerV = (TextView) findViewById(R.id.center_note);
		rightV = (TextView) findViewById(R.id.right_note);
        defaultColor = rightV.getCurrentTextColor();

        prev_note = (TextView) findViewById(R.id.prev_note);
        current_note = (TextView) findViewById(R.id.current_note);
        next_note = (TextView) findViewById(R.id.next_note);

        current_measure_index = 0;
        current_note_index = 0;

        //Myo
        hub = Hub.getInstance();
        if (!hub.init(this)) {
            Log.e("MYO", "Could not initialize the Hub.");
            finish();
            return;
        }
        hub.attachToAdjacentMyo();
        hub.setLockingPolicy(Hub.LockingPolicy.STANDARD);
        hub.addListener(mListener);
    }

    private DeviceListener mListener = new AbstractDeviceListener() {
        @Override
        public void onConnect(Myo myo, long timestamp) {
            Toast.makeText(getApplicationContext(), "Myo Connected!", Toast.LENGTH_SHORT).show();
            Log.i("Myo", "Myo Connected");
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            Toast.makeText(getApplicationContext(), "Myo Disconnected!", Toast.LENGTH_SHORT).show();
            Log.i("Myo", "Myo Disconnected");
        }

        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            Toast.makeText(getApplicationContext(), "Pose: " + pose, Toast.LENGTH_SHORT).show();
            Log.i("Myo", "Pose");

            switch (pose) {
                case UNKNOWN:
                    break;
                case REST:
                    break;
                case DOUBLE_TAP:
                    break;
                case FIST:
                    requestArpeggio();
                    break;
                case WAVE_IN:
                    sendtoServer(0,0);
                    break;
                case WAVE_OUT:
                    sendtoServer(0,0);
                    break;
                case FINGERS_SPREAD:
                    myo.unlock(Myo.UnlockType.HOLD);
                    break;
            }
        }
    };

    public void onStart(){
        super.onStart();
        initLayoutState();
    }

    @Override
    public void onPause(){
        if(tuner != null && tuner.isAlive()){
            tuner.close();
        }
        hub.removeListener(mListener);
        super.onPause();
    }

    // listen for first note
    private void getMusic() {
        System.out.println("getting music");
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
                    current_note.setText(NAME[noteslist.get(0).get(0) + 1]);
                    next_note.setText(NAME[noteslist.get(0).get(1)+1]);
                    prev_note.setText("");
                }
            }
        });

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
                        ArrayList<Integer> measure_list = new ArrayList<Integer>();
                        for (int j = 0; j < measure.length(); j++) {
                            JSONObject note = measure.getJSONObject(j);
                            measure_list.add((int)note.get("note"));
                        }
                        noteslist.add(measure_list);
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
    private void sendtoServer(int index, int new_note) {
        System.out.println("myo sending to server");
        RequestParams params = new RequestParams();
        params.put("index", index); //note: this needs to be the first note of a measure
        params.put("note", new_note);
        ServerApi.post("restart", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                // pull in notes from the server
                System.out.println(response);
                /*
                // pull in notes from the server
                try {
                    JSONArray measures = response.getJSONArray("tab");
                    for (int i = 0; i < measures.length(); i++) {
                        JSONArray measure = measures.getJSONArray(i);
                        ArrayList<Integer> measure_list = new ArrayList<Integer>();
                        for (int j = 0; j < measure.length(); j++) {
                            JSONObject note = measure.getJSONObject(j);
                            measure_list.add((int)note.get("note"));
                        }
                        noteslist.add(measure_list);
                    }
                    initTuner();
                } catch (JSONException e) {
                    e.printStackTrace();
                }*/
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
        double ideal_frequency = FREQUENCIES[noteslist.get(current_measure_index).get(current_note_index)];
        if (frequency < ideal_frequency + 25 || frequency < ideal_frequency - 25) {
            // switch notes in views
            prev_note.setText(NAME[noteslist.get(current_measure_index).get(current_note_index)+1]);
            // update current_measure_index and current_note_index
            if (noteslist.get(current_measure_index).size() == current_note_index + 1) {
                current_note_index = 0;
                current_measure_index += 1;
                System.out.println("current measure: " + current_measure_index);
            }
            next_note.setText(NAME[noteslist.get(current_measure_index).get(current_note_index+1)]);
            current_note.setText(NAME[noteslist.get(current_measure_index).get(current_note_index)]);
        }

        // check for pause and new note
        if (frequency >= 180 && frequency <= 1600 && (frequency < ideal_frequency + 200 || frequency > ideal_frequency - 200)) {
            current_note.setTextColor(getResources().getColor(R.color.red));
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){
                public void run() {
                    current_note.setTextColor(getResources().getColor(R.color.white));
                    //sendtoServer(current_measure_index, noteslist.get(current_measure_index).get(current_note_index));
                    System.out.println(NAME[currentFrameIndex+1]);
                    current_note.setText(NAME[currentFrameIndex+1]);
                }
            }, 1000);
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

    // myo fist
    public static void requestArpeggio() {
        System.out.println("myo requesting arpeggio");
        RequestParams params = new RequestParams();
        ServerApi.post("arpeggio", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.i("myo post request", response.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
                super.onFailure(statusCode, headers, errorResponse, e);
                System.out.println("SERVER ERROR" + errorResponse);
            }
        });
    }
}
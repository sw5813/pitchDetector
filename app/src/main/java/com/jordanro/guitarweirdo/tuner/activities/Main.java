package com.jordanro.guitarweirdo.tuner.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.media.MediaPlayer;
import com.jordanro.guitarweirdo.tuner.R;
import com.jordanro.guitarweirdo.tuner.uiUtil.AnimationFactory;
import com.jordanro.guitarweirdo.tuner.audioUtil.TunerEngine;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import android.text.format.Time;
import android.widget.Toast;

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
    private static final double[] FREQUENCIES = { 196, 207.65, 220, 233.08, 246.94, 261.63, 277.18, 293.66, 311.13, 349.23, 369.99, 392.00, 415.30, 440.00, 466.16, 493.88, 523.25, 554.37, 587.33, 622.25, 659.25, 698.46, 739.99, 783.99, 830.61, 880.00, 932.33, 987.77, 1046.50, 1108.73, 1174.66, 1244.51, 1318.51, 1396.91, 1479.98, 1567.98};
    private static final String[] NAME        = {"","G",  "G#", "A",  "A#"  , "B" ,   "C",    "C#",    "D#",  "D#",   "F",    "F#",    "G",   "G#"   , "A"  ,  "A#",   "B",   "C",    "C#",    "D",   "D#",    "E",    "F",   "F#",    "G",   "G#",   "A",    "A#",    "B",     "C",    "C#",    "D",     "D#",    "E",     "F",     "F#",   "G",""};
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        noteslist.add(5);
        noteslist.add(6);
        noteslist.add(7);
        noteslist.add(8);
        noteslist.add(9);
        noteslist.add(10);
        noteslist.add(11);

        animator = findViewById(R.id.gauge_background);

		leftV = (TextView) findViewById(R.id.left_note);
		centerV = (TextView) findViewById(R.id.center_note);
		rightV = (TextView) findViewById(R.id.right_note);
        defaultColor = rightV.getCurrentTextColor();

        prev_note = (TextView) findViewById(R.id.prev_note);
        current_note = (TextView) findViewById(R.id.current_note);
        next_note = (TextView) findViewById(R.id.next_note);

		toggleTuner = (ImageView)findViewById(R.id.toggle_tuner);
		toggleTuner.setOnClickListener(new View.OnClickListener() {
            boolean start = true;
            public void onClick(View view) {
                toggleTunerState(start);
                start = !start;
            }
		});
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

    int defaultColor;
    private void initLayoutState() {
        animator.getBackground().setAlpha(127);
        firstUpdate = true;
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

    private void sendToServer() {

        // Send index + new note
        /*
        RequestParams en_params = new RequestParams();
        en_params.put("note", note);
        ServerApi.post("song/search", en_params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                // extract names and artists from echonest
                try {
                    JSONArray songs = response.getJSONObject("response").getJSONArray("songs");
                    for (int i = 0; i < songs.length(); i++) {
                        JSONObject song = songs.getJSONObject(i);
                        String[] potential_song = {song.get("title").toString(), song.get("artist_name").toString()};
                        en_potential_songs.add(potential_song);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Determine which of those songs are available on Spotify
                for (int i = 0; i < en_potential_songs.size(); i++) {
                    String title = en_potential_songs.get(i)[0];
                    String artist = en_potential_songs.get(i)[1];
                    checkSpotifyAvailability(title, artist);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
                super.onFailure(statusCode, headers, errorResponse, e);
                System.out.println("ECHONEST ERROR" + errorResponse);
            }
        });
*/

    }

    private void pullfromServer() {

    }


    public void updateUI(double frequency){
        if (firstUpdate) {
            leftV.setVisibility(View.VISIBLE);
            centerV.setVisibility(View.VISIBLE);
            rightV.setVisibility(View.VISIBLE);
            firstUpdate = false;

            current_note.setText(NAME[noteslist.get(0)+1]);
            next_note.setText(NAME[noteslist.get(1)+1]);
            prev_note.setText("");
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
        if (frequency < ideal_frequency + 50 || frequency < ideal_frequency - 50) {
            if (prev_note.getText() != "") { noteslist.remove(0); }
            // switch notes
            next_note.setText(NAME[noteslist.get(2)+1]);
            prev_note.setText(NAME[noteslist.get(0)+1]);
            current_note.setText(NAME[noteslist.get(1)+1]);
        }

        // check for pause and new note
        if (frequency < ideal_frequency + 200 || frequency > ideal_frequency - 200) {
            // sendToServer(index, new_note);
            current_note.setTextColor(getResources().getColor(R.color.red));
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){
                public void run() {
                    current_note.setTextColor(getResources().getColor(R.color.white));
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

}
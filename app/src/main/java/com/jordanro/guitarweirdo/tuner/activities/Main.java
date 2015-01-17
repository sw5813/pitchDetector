package com.jordanro.guitarweirdo.tuner.activities;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
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
import java.util.List;

/**
 * User: Yarden
 * Date: Dec 24, 2009
 * Time: 6:27:14 PM
 */
public class Main extends Activity {
    private static final double[] FREQUENCIES = { 77.78, 82.41, 87.31, 92.50, 98.00, 103.83, 110.00, 116.54, 123.47, 130.81, 138.59, 146.83, 155.56, 164.81 ,174.61};
    private static final String[] NAME        = {  "D#",  "E",   "F",   "F#"  , "G" ,  "G#",   "A",    "A#",   "B",   "C",     "C#",   "D",   "D#"   ,"E"  ,   "F" };

    TunerEngine tuner;
    final Handler mHandler = new Handler();
    final Runnable callback = new Runnable() {
        public void run() {
            updateUI(tuner.currentFrequency);
//            System.out.println("tuner.currentFrequency = " + tuner.currentFrequency);
        }
    };

	View animator;
    TextView leftV,centerV,rightV;
    boolean firstUpdate;
    public static final int DEFAULT_TRANSFORM_DURATION = 150;
    public static final int DEFAULT_ALPHA_DURATION = 70;

    Button toggleTuner;
    String tuner_on,tuner_off;

    ArrayList<String> noteslist = new ArrayList<String>();

    // Volume Detection
    public static final int SAMPLE_RATE = 16000;

    private AudioRecord mRecorder;
    private File mRecording;
    private short[] mBuffer;
    private boolean mIsRecording = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		animator = findViewById(R.id.gauge_background);

		leftV = (TextView) findViewById(R.id.left_note);
		centerV = (TextView) findViewById(R.id.center_note);
		rightV = (TextView) findViewById(R.id.right_note);
        defaultColor = rightV.getCurrentTextColor();

		toggleTuner = (Button)findViewById(R.id.toggle_tuner);
        tuner_on = getResources().getString(R.string.tuner_on);
        tuner_off = getResources().getString(R.string.tuner_off);
//        createLayout();

		toggleTuner.setOnClickListener(new View.OnClickListener() {
            boolean start = true;
            public void onClick(View view) {
                toggleTunerState(start);
                start = !start;
                mIsRecording = !mIsRecording;
                mRecorder.startRecording();
                mRecording = getFile("raw");
                startBufferedWrite(mRecording);
            }
		});

        // Volume Detection
        initRecorder();

	}

    public void onStart(){
        super.onStart();
        playIntro(this);
        initLayoutState();
    }

    @Override
    public void onPause(){
        if(tuner != null && tuner.isAlive()){
            tuner.close();
        }
        super.onPause();
    }

    private void initRecorder() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    int defaultColor;
    private void initLayoutState(){
        animator.getBackground().setAlpha(127);
        firstUpdate = true;
    }


    public void toggleTunerState(boolean start){
        if(start){
            try {
                tuner = new TunerEngine(mHandler,callback);
                tuner.start();
                toggleTuner.setText(tuner_off);
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
            toggleTuner.setText(tuner_on);
        }

    }
    Animation fadeIn  = AnimationFactory.getAnimation(AnimationFactory.FADE_IN,DEFAULT_ALPHA_DURATION);
    Animation fadeIn50  = AnimationFactory.getAnimation(AnimationFactory.FADE_IN_50,300);
    Animation fadeOut  = AnimationFactory.getAnimation(AnimationFactory.FADE_OUT,10);
    Animation fadeOut50  = AnimationFactory.getAnimation(AnimationFactory.FADE_OUT_50,300);
    int previousOffset = 0;
    int currentFrameIndex = 1;
    int numFrames = 12;

    private void sendToServer(String note) {
        noteslist.add(note);
        TextView notes = (TextView)findViewById(R.id.notes);
        notes.setText(noteslist.toString());
        // Create playlist with EchoNest Api
        /*
        RequestParams en_params = new RequestParams();
        en_params.put("note", note);
        EchoNestApi.get("song/search", en_params, new JsonHttpResponseHandler() {
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

    public void updateUI(double frequency){

        if(firstUpdate){
            leftV.setVisibility(View.VISIBLE);
            centerV.setVisibility(View.VISIBLE);
            rightV.setVisibility(View.VISIBLE);
            firstUpdate = false;
        }

        frequency = normaliseFreq(frequency);
        int note = closestNote(frequency);
        double matchFreq = FREQUENCIES[note];
        int offset = 0;

        if ( frequency < matchFreq ) {
            double prevFreq = FREQUENCIES[note-1];
            offset = (int)(-(frequency-matchFreq)/(prevFreq-matchFreq)/0.2);
        }
        else {
            double nextFreq = FREQUENCIES[note+1];
            offset = (int)((frequency-matchFreq)/(nextFreq-matchFreq)/0.2);
        }
        int frameShift = note - currentFrameIndex;
        if(note > currentFrameIndex){
            currentFrameIndex = note;
        }
        else if(note < currentFrameIndex){
            currentFrameIndex = note;
        }

        sendToServer(NAME[currentFrameIndex]);
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
            leftV.setText(NAME[currentFrameIndex-1]);
            centerV.setText(NAME[currentFrameIndex]);
            rightV.setText(NAME[currentFrameIndex+1]);
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

    private static double normaliseFreq(double hz) {
        // get hz into a standard range to make things easier to deal with
        while ( hz < 82.41 ) {
            hz = 2*hz;
        }
        while ( hz > 164.81 ) {
            hz = 0.5*hz;
        }
        return hz;
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

    private static void playIntro(android.content.Context context){
        MediaPlayer.create(context, R.raw.subbacultcha).start();
    }

    private void startBufferedWrite(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                    while (mIsRecording) {
                        double sum = 0;
                        int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);
                        for (int i = 0; i < readSize; i++) {
                            output.writeShort(mBuffer[i]);
                            sum += mBuffer[i] * mBuffer[i];
                        }
                        if (readSize > 0) {
                            final double amplitude = sum / readSize;
                            if (amplitude > 3000000) {
                                System.out.println(amplitude);
                            }
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(Main.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    System.out.println(0);
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Toast.makeText(Main.this, e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Toast.makeText(Main.this, e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private File getFile(final String suffix) {
        Time time = new Time();
        time.setToNow();
        return new File(Environment.getExternalStorageDirectory(), time.format("%Y%m%d%H%M%S") + "." + suffix);
    }
}

package com.jordanro.guitarweirdo.tuner.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Button;
import android.media.MediaPlayer;
import com.jordanro.guitarweirdo.tuner.R;
import com.jordanro.guitarweirdo.tuner.uiUtil.AnimationFactory;
import com.jordanro.guitarweirdo.tuner.audioUtil.TunerEngine;

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
    public static final int DEAFULT_TRANSFORM_DURATION = 150;
    public static final int DEAFULT_ALPHA_DURATION = 70;

    Button toggleTuner;
    String tuner_on,tuner_off;


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
            }


		});

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
    Animation fadeIn  = AnimationFactory.getAnimation(AnimationFactory.FADE_IN,DEAFULT_ALPHA_DURATION);
    Animation fadeIn50  = AnimationFactory.getAnimation(AnimationFactory.FADE_IN_50,300);
    Animation fadeOut  = AnimationFactory.getAnimation(AnimationFactory.FADE_OUT,10);
    Animation fadeOut50  = AnimationFactory.getAnimation(AnimationFactory.FADE_OUT_50,300);
    int previousOffset = 0;
    int currentFrameIndex = 1;
    int numFrames = 12;

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
        moveGauge(frameShift,offset);
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
}

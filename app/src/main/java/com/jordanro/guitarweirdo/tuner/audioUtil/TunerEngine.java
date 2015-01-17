package com.jordanro.guitarweirdo.tuner.audioUtil;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;

/**
 * User: Yarden
 * Date: Oct 19, 2009
 * Time: 10:48:47 PM
 */
public class TunerEngine extends Thread{

    static{
        System.loadLibrary("FFT");
    }

    public native double processSampleData(byte[] sample,int sampleRate);

    private static final int[] OPT_SAMPLE_RATES           = { 11025 , 8000   ,  22050 , 44100  };
    private static final int[] BUFFERSIZE_PER_SAMPLE_RATE = { 8*1024, 4*1024 , 16*1024, 32*1024};

    public double currentFrequency = 0.0;
    public double currentVolume = 0.0;

    int SAMPLE_RATE = 44100;
    int READ_BUFFERSIZE = 32*1024;

    private short[] mBuffer;

    AudioRecord targetDataLine_;

    final Handler mHandler;
    Runnable callback;

    public TunerEngine(Handler mHandler,Runnable callback) {
        this.mHandler = mHandler;
        this.callback = callback;
        initAudioRecord();
    }

    private void initAudioRecord(){
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];
        targetDataLine_ = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        /*
        int counter = 0;
        for(int sampleRate : OPT_SAMPLE_RATES){ 
            initAudioRecord(sampleRate);
            if(targetDataLine_.getState() == AudioRecord.STATE_INITIALIZED ){
                SAMPLE_RATE = sampleRate;
                READ_BUFFERSIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                break;
            }
            counter++;
        }*/
    }
    //unused
    private void initAudioRecord(int sampleRate){
        targetDataLine_ =  new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT ,
                sampleRate*6
        );
    }

    byte[] bufferRead;
//    long l;
    public void run() {

        targetDataLine_.startRecording();
        bufferRead = new byte[READ_BUFFERSIZE];
        int n = -1;
        while ( (n = targetDataLine_.read(bufferRead, 0, READ_BUFFERSIZE)) > 0 ) {
//            l = System.currentTimeMillis();
            currentFrequency = processSampleData(bufferRead,SAMPLE_RATE)/2;
            System.out.println("frequency: " + currentFrequency);

            double sum = 0;
            int readSize = targetDataLine_.read(mBuffer, 0, mBuffer.length);
            for (int i = 0; i < readSize; i++) {
                sum += mBuffer[i] * mBuffer[i];
            }
            if (readSize > 0) {
                currentVolume = sum/readSize;
                System.out.println("amplitude: " + currentVolume);
            }
//            System.out.println("process time  = " + (System.currentTimeMillis() - l));
            if(currentFrequency >= 180 && currentFrequency <= 1600 && currentVolume > 15000000) {
                mHandler.post(callback);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
        }

    }

    public void close(){
        //targetDataLine_.stop();
        targetDataLine_.release();
    }

}

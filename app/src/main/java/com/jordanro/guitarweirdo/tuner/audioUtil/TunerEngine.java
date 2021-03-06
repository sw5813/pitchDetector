package com.jordanro.guitarweirdo.tuner.audioUtil;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;

public class TunerEngine extends Thread{

    static{
        System.loadLibrary("FFT");
    }

    public native double processSampleData(byte[] sample, int sampleRate);

    public double currentFrequency = 0.0;
    public double currentVolume = 0.0;
    public double ambience = 70000000;
    boolean firstTime = true;

    int SAMPLE_RATE = 44100;
    int READ_BUFFERSIZE = 32*1024;

    private short[] mBuffer;

    AudioRecord targetDataLine_;

    final Handler mHandler;
    Runnable callback;

    public void setAmbience(double number) { ambience = number; }

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
//            System.out.println("frequency: " + currentFrequency);

            double sum = 0;
            int readSize = targetDataLine_.read(mBuffer, 0, mBuffer.length);
            for (int i = 0; i < readSize; i++) {
                sum += mBuffer[i] * mBuffer[i];
            }
            if (readSize > 0) {
                currentVolume = sum/readSize;
   //             System.out.println("amplitude: " + currentVolume);
            }

            // set
            if (firstTime) {
                setAmbience(currentVolume + 6000000);
                firstTime = false;
            }

//            System.out.println("process time  = " + (System.currentTimeMillis() - l));
            if(currentFrequency >= 180 && currentFrequency <= 1600 && currentVolume > ambience) {
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

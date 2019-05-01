package com.example.narwalk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mapbox.core.exceptions.ServicesException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Detection extends AppCompatActivity {
    private static String TAG = "AudioClient";

    // the server information
    // the audio recording options
    private static final int SAMPLE_RATE = 44100;
    private static final int STARTFREQ = 17000;
    private static final int ENDFREQ = 20000;
    private static final double DURATION_TO_SEND = 0.02; // seconds
    private static final double DURATION=0.005;
    private static final int TOTAL_SAMPLES = 1794;
    private static final String SERVER = "10.140.143.49";
    private static final String SERVER_HOME ="192.168.1.142";
    private static final int PORT = 9800;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private Socket socket;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    int bufferSize = Math.max(SAMPLE_RATE / 2,
            AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT));
    short[] buffer = new short[bufferSize]; // use short to hold 16-bit PCM encoding
    private double sample[] = new double[100];
    private byte generatedSnd[] = new byte[2 * 100];
    private double hanning[] = new double[10];
    // the button the user presses to send the audio stream to the server
    private Button sendAudioButton;
    public ArrayList<Short> all_data = new ArrayList<Short>();
    // the audio recorder
    private static AudioRecord recorder;

    private boolean isHanning = false;
    // the minimum buffer size needed for audio recording
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL, FORMAT);
    // are we currently sending audio data
    private boolean currentlySendingAudio = false;

    private TextToSpeech tts;
    private FloatingActionButton fab;
    protected static final int RESULT_SPEECH = 1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                }
            }
        });
        tts.setPitch(.8f);
        tts.setSpeechRate(.9f);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak("Select Mode",TextToSpeech.QUEUE_FLUSH, null);
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(),
                            "This device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        Log.i(TAG, "Creating the Audio Client with minimum buffer of "
                + BUFFER_SIZE + " bytes");

        requestRecordAudioPermission();

    }

    public void startStreamingAudio(View v) {

        Log.i(TAG, "Starting the audio stream");
        currentlySendingAudio = true;
        startStreaming();
    }

    public void stopStreamingAudio(View v) {

        Log.i(TAG, "Stopping the audio stream");
        currentlySendingAudio = false;
        recorder.release();
    }

    // Callback for the activity run
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // If result is speech, try to convert to endpoints for navigation
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (text.size() > 0) {
                        try {

                            if (text.get(0).equalsIgnoreCase("Detect")) {
                                playSound(null);
                            }
                            else if (text.get(0).equalsIgnoreCase("Initialize")) {
                                sendSignalToMatlab(null);
                            }
                            else {
                                tts.speak("Unrecognized Event,    please try again",TextToSpeech.QUEUE_FLUSH, null);
                            }
                        } catch (ServicesException e) {
                            tts.speak("Error,    please try again",TextToSpeech.QUEUE_FLUSH, null);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "We had a failure to communicate.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
        }
    }
    private void startStreaming() {

        Log.i(TAG, "Starting the background thread to stream the audio data");
        all_data =  new ArrayList<Short>();
        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    Log.d(TAG, "Creating the datagram socket");
//                    DatagramSocket socket = new DatagramSocket();

                    Log.d(TAG, "Creating the buffer of size " + BUFFER_SIZE);
//                    float[] buffer1 = new float[buffer.length];
                    short[] buffer1 = new short[buffer.length];
//                    ByteBuffer bytebuf = ByteBuffer.allocate(2*buffer.length);

                    Log.d(TAG, "Connecting to " + SERVER + ":" + PORT);
                    final InetAddress serverAddress = InetAddress.getByName(SERVER);
                    Log.d(TAG, "Connected to " + SERVER + ":" + PORT);

                    Log.d(TAG, "Creating the reuseable DatagramPacket");
                    DatagramPacket packet;

                    Log.d(TAG, "Creating the AudioRecord");
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            SAMPLE_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);


                    Log.d(TAG, "AudioRecord recording...");
                    recorder.startRecording();
                    int read = 0;
//                    while (currentlySendingAudio == true) {
                    while (read <= 0) {
                        // read the data into the buffer
                        read = recorder.read(buffer1, 0, buffer1.length, AudioRecord.READ_BLOCKING);
                        Log.w(TAG, "Total Numebr of Bytes read = " + read + " Buffer length " + buffer.length);
                        for (int i = 0; i < buffer1.length; i++) {
                            all_data.add(buffer1[i]);
//                            Log.d(TAG, "In the adding thing");
//                            buffer1[i] = (byte) buffer[i];
//                            bytebuf.putShort(buffer[i]);
                        }
                    }
                    // place    contents of buffer into the packet
//                        packet = new DatagramPacket(buffer1, buffer1.length, serverAddress, PORT);
//                        socket.send(packet);
//                        break;
//                        // send the packet
//
//                    }
                    boolean isNonZero = false;
                    boolean isSocketInitiated = false;
                    int counter = 0;
                    Log.i(TAG, "IN NEW THREAD");
                    try {
                        initSocket(SERVER);
                        isSocketInitiated = true;
                    }
                    catch (IOException e) {
                        Log.i(TAG, e.getMessage());
                    }
                    for (int i = 0; i < buffer1.length; i ++) {
                        if(Math.abs(buffer1[i]) > 0){
                            isNonZero = true;
                            counter = i - 10;
                            Log.i(TAG, "The counter value is : " + counter);
                            break;
                        }
//                            Log.i(TAG, "WRITING TO SOCKET");
//                            Thread.sleep(1);
                    }
                    Log.i(TAG, "Sending Data Now");
                    try {
                        if (isSocketInitiated) {
                            for (int i = counter; i < counter + TOTAL_SAMPLES; i++) {
                                writeToSocket(buffer1[i]);
                            }
                        }
                    }
                    catch (IOException e) {
                        Log.i(TAG, e.getMessage());
                    }
//                    }
//                    } catch (InterruptedException e) {
//
//                    }
                    Log.i(TAG, "AudioRecord finished recording");
                    recorder.release();
                } catch (Exception e) {
                    Log.i(TAG, "Exception: " + e);
                }
            }
        });

        // start the thread
        streamThread.start();
    }

    private void initSocket(String ip) throws IOException {
        Log.i(TAG, "Initiating Socket");
        socket = new Socket(ip, 3000, null, 0);
        if (socket.isConnected()) {
            Log.i(TAG, "SUCCESSFULLY CONNECTED");
        }
        else {
            Log.i(TAG, "NOT CONNECTED");
        }
    }

    private void writeToSocket(short val) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeShort(val);
//        dataOutputStream.writeFloat(val);
    }

    private void writeToSocket(double val) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeDouble(val);
//        dataOutputStream.writeFloat(val);
    }

    private void startStreamingTest() {

        Log.i(TAG, "Starting the background thread to stream the audio data");

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    Log.i(TAG, "Creating the datagram socket");
                    DatagramSocket socket = new DatagramSocket();

                    Log.i(TAG, "Creating the buffer of size " + BUFFER_SIZE);
                    byte[] buffer1 = new byte[BUFFER_SIZE];

                    Log.i(TAG, "Connecting to " + SERVER + ":" + PORT);
                    final InetAddress serverAddress = InetAddress.getByName(SERVER);
                    Log.i(TAG, "Connected to " + SERVER + ":" + PORT);

                    Log.i(TAG, "Creating the reuseable DatagramPacket");
                    DatagramPacket packet = new DatagramPacket(buffer1, buffer1.length, serverAddress, PORT);
//                    DatagramPacket packet = new DatagramPacket(buffer1, buffer1.length, serverAddress, PORT);

                    Log.i(TAG, "Creating the AudioRecord");
                    recorder = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                            SAMPLE_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);

                    Log.i(TAG, "AudioRecord recording...");
                    recorder.startRecording();
                    int read = 0;
//                    while (currentlySendingAudio == true) {
                    while(true){

                        // read the data into the buffer
                        read = recorder.read(buffer1, 0, buffer1.length);

//                        for (int i = 0; i < read; i++) {
//                            all_data.add(buffer[i]);
//                            Log.d(TAG, "In the adding thing");
//                        }
                        // place    contents of buffer into the packet
                        packet = new DatagramPacket(buffer1, read, serverAddress, PORT);
                        Log.i(TAG, "Total Numebr of Bytes read = " + read);
                        // send the packet
                        break;

                    }
////                    socket.send(packet);
//                    buffer1[0] = (byte)(float)100.1;
//                    buffer1[1] = (byte)106.1;
//                    buffer1[2] = (byte)105.1;
//                    buffer1[3] = (byte)103.1;
//                    buffer1[4] = (byte)102.1;
//                    buffer1[5] = (byte)101.1;
//                    Log.i(TAG, "Since wave data generated");
//                        // place    contents of buffer into the packet
//                        packet = new DatagramPacket(buffer1, buffer1.length, serverAddress, PORT);
////                        break;
//                        // send the packet
////                        socket.send(packet);
////                    }
//
//                    Log.d(TAG, "AudioRecord finished recording");
//
//
//                    Log.i(TAG, "Sending Packet");
//                    socket.send(packet);

                    Log.i(TAG, "Receiving Packet");
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    Log.i(TAG, received);
                    Log.i(TAG, "Done");


                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e);
                }
            }
        });

        // start the thread
        streamThread.start();
    }

    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("Activity", "Granted!");

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Activity", "Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void plotData(View view) {

        Intent newIntent = new Intent(Detection.this, plottingChart.class);
        newIntent.putExtra("item", all_data);
        Log.d(TAG, "Starting the new activity");
        this.startActivity(newIntent);

    }

//    public void playSound(View view) {
//        EditText text = (EditText)findViewById(R.id.inputFreq);
//        String t = text.getText().toString();
//        int frequency = 1500;
//        try {
//            frequency = Integer.parseInt(t);
//        }
//        catch (NumberFormatException e){
//            frequency = 1500;
//        }
//        playSoundHelper(frequency, 44100);
//    }

    public void playSound(View view) {
//        genTone(4000,8000,0.001);
//        if (isHanning == false){
//            Log.i(TAG, "In the Hanning Window Function");
//            getHanningWindow(DURATION);
//        }
//        getWindowedSignal();
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
//        startStreamingSound();
        startStreaming();
        Log.i(TAG, "Playing the Audio");
        audioTrack.play();
//        recorder.release();

    }

    private void getWindowedSignal(){
        int numSamples = (int)Math.floor(DURATION*SAMPLE_RATE);
        for (int i = 0; i < numSamples; i++){
            sample[i] = sample[i]*hanning[i];
        }
    }

    private void playSoundHelper(double frequency, int duration) {
        // AudioTrack definition
        int mBufferSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_8BIT);

        AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                mBufferSize, AudioTrack.MODE_STREAM);

        // Sine wave
        double[] mSound = new double[4410];
        short[] mBuffer = new short[duration];
        for (int i = 0; i < mSound.length; i++) {
            mSound[i] = Math.sin((2.0*Math.PI * i/(44100/frequency)));
            mBuffer[i] = (short) (mSound[i]*Short.MAX_VALUE);
        }

        mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
        mAudioTrack.play();

        mAudioTrack.write(mBuffer, 0, mSound.length);
        mAudioTrack.stop();
        mAudioTrack.release();

    }

    private double getChirpRate(double f1, double f2, double duration){
        return (double)(f2-f1)/duration;
    }

    private double getSineArg(double startFreq, double chirpRate, double phase, int index){
        double res = 0.0;
        double time = (double)index*1.0/SAMPLE_RATE;
        res = phase + 2*(Math.PI)*(startFreq*time + (chirpRate/2)*time*time);
        return res;
    }

    private void genTone(double startFreq, double endFreq, double dur) {
        // Duration is in seconds. So for 500 ms, use 0.5
        int numSamples = (int)java.lang.Math.floor(dur * SAMPLE_RATE);
        double chirpRate = getChirpRate(startFreq,endFreq,dur);
        double arg = 0.0;
        double phase = 0.0;
        sample = new double[numSamples];
        for (int i = 0; i < numSamples; i = i + 1) {
            arg = getSineArg(startFreq,chirpRate,phase,i);
            sample[i] = Math.sin(arg);
        }
        convertToPCM(numSamples);
    }

    private void genToneBackup(double startFreq, double endFreq, double dur) {
        // Duration is in seconds. So for 500 ms, use 0.5
        int numSamples = (int)java.lang.Math.floor(dur * SAMPLE_RATE);
        double chirpRate = getChirpRate(startFreq,endFreq,dur);
        sample = new double[numSamples];
        double currentFreq = 0, numerator;
        for (int i = 0; i < numSamples; ++i) {
            numerator = (double) i / (double) numSamples;
            currentFreq = startFreq + (numerator * (endFreq - startFreq))/2;
            if ((i % 1000) == 0) {
                Log.d(TAG, String.format("Freq is:  %f at loop %d of %d", currentFreq, i, numSamples));
            }
            sample[i] = Math.sin(2 * Math.PI * i / (SAMPLE_RATE / currentFreq));
        }
        convertToPCM(numSamples);
    }

    private void getHanningWindow(double dur){
        int numSamples = (int)java.lang.Math.floor(dur * SAMPLE_RATE);
        hanning = new double[numSamples];
        double arg = 0.0;
        for (int i= 0; i < numSamples; i++){
            arg = 2*Math.PI*i/(numSamples - 1);
            hanning[i] = 0.5*(1-Math.cos(arg));
        }
        isHanning = true;
    }


    private void convertToPCM(int numSamples) {
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        generatedSnd = new byte[2 * numSamples];
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    private List<Double> peakDetect(double[] sig, double threshold)
    {
        List peak_arr = new ArrayList<Double>();
        for(int i=0;i<sig.length;i++)
        {
            if(i==0)
            {

            }
            else if(i==sig.length-1)
            {

            }
            else
            {
                if((sig[i]>sig[i-1])&&(sig[i]>sig[i+1]))
                {
                    if(sig[i]>=threshold)
                    {
                        peak_arr.add(sig[i]);
                    }
                }
            }
        }
        return peak_arr;
    }
    private void startStreamingSound() {

        Log.i(TAG, "Starting the background thread to stream the audio data");

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Log.i(TAG, "Sending This many bytes " + sample.length);
                    Log.i(TAG, "IN NEW THREAD");
                    try {
                        initSocket(SERVER);
                        Log.i(TAG, "Socket Initiated");
                        for (int i = 0; i < sample.length; i ++) {
                            writeToSocket(sample[i]);
//                            Log.i(TAG, "WRITING TO SOCKET");
//                            Thread.sleep(1);
                        }
                    } catch (IOException e) {
                        Log.i(TAG, e.getMessage());
                    }
                    Log.i(TAG, "Finished sending data: ");
                } catch (Exception e) {
                    Log.i(TAG, "Exception: " + e);
                }
            }
        });

        // start the thread
        streamThread.start();
    }

    public void sendSignalToMatlab(View view) {
        genTone(STARTFREQ,ENDFREQ,DURATION);
        if (isHanning == false){
            Log.i(TAG, "In the Hanning Window Function");
            getHanningWindow(DURATION);
        }
        getWindowedSignal();
        Log.i(TAG, "Now streaming to Matlab");
        startStreamingSound();
    }
}

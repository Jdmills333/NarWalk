package com.example.narwalk;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {
    
    //step length stuff
    private float[] coef;
    private float average_step_length;
    private float height;
    private float last_step_time;
    ArrayList<Float> accels;
    ArrayList<Float> steps;

    HandlerThread sensorThread;
    Handler sensorHandler;

    private String[] tensStrings;
    private String[] teenStrings;
    private String[] onesStrings;
    //end step length stuff


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize step length stuff
        coef = new float[4];
        coef[0] = 0.087135f;
        coef[1] = 078120f;
        coef[2] = 0.411146f;
        coef[3] = -0.339232f;
        average_step_length = 0.0f;
        height = 1.75f;
        last_step_time = SystemClock.elapsedRealtime();
        accels = new ArrayList<Float>();
        steps = new ArrayList<Float>();

        tensStrings = new String[] {null, null, "twenty", "thirty", "fifty", "sixty", "seventy", "eighty", "ninety"};
        teenStrings = new String[] {"ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
                                    "eighteen", "nineteen"};
        onesStrings = new String[] {null, "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};

        sensorThread = new HandlerThread("sensorThread");
        sensorThread.start();
        sensorHandler = new Handler(sensorThread.getLooper());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_FASTEST, sensorHandler);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST, sensorHandler);
        //end step length stuff
    }

    public void onSensorChanged(SensorEvent event) {
        //step length sensors
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            getStepLength();
            stepCount++;
            accels.clear();
        }

        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float X = event.values[0]; //assume for now that this is vertical
            accels.add(X);
        }
        //end step length sensors
    }    

    public void startDetection(View v) {
        Intent detectIntent = new Intent(MainActivity.this, Detection.class);
        MainActivity.this.startActivity(detectIntent);
    }

    public void startNavigation(View v) {
        Intent navIntent = new Intent(MainActivity.this, Navigation.class);
        MainActivity.this.startActivity(navIntent);
    }

    public void startNavigationAndDetection(View v) {
        Intent navDetectIntent = new Intent(MainActivity.this, DetectionAndNavigation.class);
        MainActivity.this.startActivity(navDetectIntent);
    }

    //step length functions
    private float getMean(ArrayList<Float> vals) {
        int size = vals.size();
        float sum = 0f;
        for (Float val : vals) sum += val;
        return sum / (float) size;
    }

    private float getVariance(ArrayList<Float> vals) {
        int size = vals.size();
        float mean = getMean(vals);
        float var = 0f;
        for (float val : vals) {
            float diff = val - mean;
            var += diff * diff;
        }
        var = var / (size - 1);
        return var;
    }

    private void getStepLength() {
        float newTime = SystemClock.elapsedRealtime();
        float time = newTime - last_step_time;
        float freq = 1f / time;
        float var = getVariance(accels);
        float length =  coef[0] * height * freq +
                        coef[1] * height * var +
                        coef[2] * height +
                        coef[3];
        steps.add(length);
        if (steps.size() > 3) steps.remove(0);
        average_step_length = getMean(steps);
        last_step_time = newTime;
    }

    public String sendOutputText(float upcomingDist, String command) {
        if (upcomingDist >= 30f) Log.w("OUTPUT STRING", "INCOMING FLOAT TOO BIG");
        float stepsToTarget = upcomingDist / average_step_length;
        if (stepsToTarget < 1) stepsToTarget = 1;
        int tens = (int) stepsToTarget / 10;
        int ones = (int) stepsToTarget % 10;
        String tensPlace = null;
        String onesPlace = null;
        if (tens == 1) {
            onesPlace = teenStrings[ones];
        }
        else if (tens > 1) {
            tensPlace = tensStrings[tens];
        }
        onesPlace = onesStrings[ones];

        String output = "In ";
        if (!tensPlace.equals(null)) {
            output += tensPlace + " ";
        }
        if (!onesPlace.equals(null)) {
            output += onesPlace + " ";
        }

        output += "steps";

        if (command.equals(null)) {
            output = "Object " + output;
        }
        else {
            output += ", " + command;
        }

        return output;
    }
    //end step length functions
}

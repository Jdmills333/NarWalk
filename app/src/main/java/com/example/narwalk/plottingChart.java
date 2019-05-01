package com.example.narwalk;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class plottingChart extends AppCompatActivity {

    private static final int SAMPLE_RATE = 11025;
    private float arg;
    private float value;
    public ArrayList<Short> all_data = new ArrayList<Short>();
    private static String TAG = "AudioClient";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if(intent!=null){
            all_data = (ArrayList<Short>)getIntent().getSerializableExtra("item");
        }
        Log.d(TAG,"Got the data from the other activity");
        setContentView(R.layout.charting_plot);
        // refresh
    }

    public void plotSine(View view) {
        LineChart chart = (LineChart) findViewById(R.id.chart);
        List<Entry> entries = new ArrayList<Entry>();
        arg = (float)0.0;
        value = (float)0.0;
        for (int x = 0; x <= 720; x++){
            arg = (float)(0.5*x);
            value = (float)(java.lang.Math.sin(java.lang.Math.toRadians(arg)));
            // turn your data into Entry objects
            entries.add(new Entry(arg, value));
        }
        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setColor(3);
        dataSet.setValueTextColor(5); // styling, ...

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }


    public void plotSound(View view) {
        LineChart chart = (LineChart) findViewById(R.id.chart);
        List<Entry> entries = new ArrayList<Entry>();
        int count = 0;
        for(Short val : all_data){
            entries.add(new Entry(count,val.floatValue()));
            count = count + 1;
        }
        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setColor(3);
        dataSet.setValueTextColor(5); // styling, ...

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();



    }
}
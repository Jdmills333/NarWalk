package com.example.narwalk;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
}

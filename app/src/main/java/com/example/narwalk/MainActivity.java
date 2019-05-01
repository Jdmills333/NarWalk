package com.example.narwalk;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.mapbox.core.exceptions.ServicesException;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private FloatingActionButton fab;
    protected static final int RESULT_SPEECH = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

                            if (text.get(0).equalsIgnoreCase("Navigation Demo")) {
                                startNavigation(null);
                                tts.speak("Starting Navigation Demo",TextToSpeech.QUEUE_FLUSH, null);
                            }
                            else if (text.get(0).equalsIgnoreCase("Detection Demo")) {
                                startDetection(null);
                                tts.speak("Starting Detection Demo",TextToSpeech.QUEUE_FLUSH, null);
                            }
                            else if (text.get(0).equalsIgnoreCase("Full Mode")) {
                                startNavigationAndDetection(null);
                                tts.speak("Starting Full Mode",TextToSpeech.QUEUE_FLUSH, null);
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

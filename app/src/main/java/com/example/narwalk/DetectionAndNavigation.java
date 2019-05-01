package com.example.narwalk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.os.Handler;
import android.os.HandlerThread;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

public class DetectionAndNavigation extends AppCompatActivity implements MapboxMap.OnMapClickListener, OnMapReadyCallback, PermissionsListener,
        ProgressChangeListener, MilestoneEventListener,  OnNavigationReadyCallback, SpeechAnnouncementListener, BannerInstructionsListener,
        SensorEventListener, NavigationListener {

    // variables to initialize map
    private MapView mapView;
    private MapboxMap mapboxMap;

    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;

    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;

    // variables needed to initialize navigation
    private Button button;
    private FloatingActionButton fab;
    private NavigationView navigationView;

    // initialization variables
    private boolean isNavigating = false;
    private Point origin = Point.fromLngLat(-77.03194990754128, 38.909664963450105);
    private static final int INITIAL_ZOOM = 16;
    protected static final int RESULT_SPEECH = 1;
    private TextToSpeech tts;
    private String routeAttempt;
    private Point destinationPoint;

    //step length variables
    private float[] coef;
    private int stepCount = -1;
    private float average_step_length;
    private float height;
    private float last_step_time;
    ArrayList<Float> accels;
    ArrayList<Float> steps;
    HandlerThread sensorThread;
    Handler sensorHandler;
    private SensorManager sensorManager;
    private float progLen = 0f;

    // boolean to simulate the navigation
    private boolean simulateRoute = false;

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add Mapbox auth key and params for map initialization
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_detection_and_navigation);
        navigationView = findViewById(R.id.navigationView);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // Set initial position of view, might not be needed in future
        CameraPosition initialPosition = new CameraPosition.Builder()
                .target(new LatLng(origin.latitude(), origin.longitude()))
                .zoom(INITIAL_ZOOM)
                .build();
        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this, initialPosition);
        mapView.getMapAsync(this);

        // set up text-to-speech with params
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

        requestRecordAudioPermission();

        // initialize step length coefficients
        coef = new float[4];
        coef[0] = 0.087135f;
        coef[1] = .078120f;
        coef[2] = 0.411146f;
        coef[3] = -0.339232f;
        average_step_length = .6f;
        height = 1.75f;
        last_step_time = SystemClock.elapsedRealtime();
        accels = new ArrayList<Float>();
        steps = new ArrayList<Float>();

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

    }

    // If a sensor has been changed, pass in the event that has done so
    public void onSensorChanged(SensorEvent event) {
        //step length sensors, update steps if in Navigation mode
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            if (stepCount >= 0 && isNavigating) {
                getStepLength();
            }
            accels.clear();
            stepCount++;
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float X = event.values[0]; //assume for now that this is vertical
            accels.add(X);
        }
    }

    // When the map has initialized and loaded with correct auth key, return the map object
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                // Ask for location privileges and add style
                enableLocationComponent(style);
                addDestinationIconSymbolLayer(style);
                mapboxMap.addOnMapClickListener(DetectionAndNavigation.this);

                // Begin navigation button onClick
                button = findViewById(R.id.startButton);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isNavigating) {
                            // Additional Milestone with trigger to add to defaults
                            List<Milestone> milestones = new ArrayList<>();
                            milestones.add(new StepMilestone.Builder()
                                    .setIdentifier(50)
                                    .setTrigger(
                                            Trigger.all(
                                                    Trigger.lt(TriggerProperty.STEP_DISTANCE_REMAINING_METERS, 13.048)
                                            )
                                    )
                                    .build());

                            // Add options for navigation with change listeners
                            boolean simulateRoute = true;
                            NavigationViewOptions navOptions = NavigationViewOptions.builder()
                                    .speechAnnouncementListener(DetectionAndNavigation.this::willVoice)
                                    .progressChangeListener(DetectionAndNavigation.this::onProgressChange)
                                    .milestoneEventListener(DetectionAndNavigation.this::onMilestoneEvent)
                                    .bannerInstructionsListener(DetectionAndNavigation.this::willDisplay)
                                    .navigationListener(DetectionAndNavigation.this)
                                    .milestones(milestones)
                                    .directionsRoute(currentRoute)
                                    .shouldSimulateRoute(simulateRoute)
                                    .build();

                            // Call this method with Context from within an Activity and relay success
                            navigationView.startNavigation(navOptions);
                            button.setText("Stop Navigation");
                            isNavigating = true;
                            tts.speak("Starting navigation", TextToSpeech.QUEUE_FLUSH, null);
                        } else {
                            // Stop navigation if currently running
                            navigationView.stopNavigation();
                            getRoute(origin, destinationPoint);
                            button.setText("Start Navigation");
                            isNavigating = false;
                        }
                    }
                });

                // Speech recognition button, when pressed will launch activity and wait for result
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
        });
    }


    // Styling for destination marker
    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    // Used for clicking a point to set a destination for navigation, useful for demo
    @SuppressWarnings({"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        // Convert LatLng coordinates to screen pixel and only query the rendered features.
        destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }
        getRoute(originPoint, destinationPoint);
        button.setEnabled(true);
        button.setBackgroundResource(R.color.mapboxBlue);
        return true;
    }

    // Callback for the activity run to process audio request and act accordingly
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
                            // Activate start and stop button based off voice command
                            button = findViewById(R.id.startButton);
                            if (text.get(0).equalsIgnoreCase("Start")) {
                                if (!isNavigating) {
                                    button.callOnClick();
                                }
                            } else if (text.get(0).equalsIgnoreCase("Stop")) {
                                if (isNavigating) {
                                    button.callOnClick();
                                }
                            } else if (text.get(0).equalsIgnoreCase("Initialize")) {
                                sendSignalToMatlab(null);
                            }
                            else if (text.get(0).equalsIgnoreCase("Detect")) {
                                playSound(null);
                            }
                            else {
                                searchForEndpointCoordinates(text.get(0));
                                routeAttempt = text.get(0);
                            }
                        } catch (ServicesException e) {
                            Log.e(TAG, "Exception While Searching:" + e.getMessage());
                            Toast.makeText(DetectionAndNavigation.this, "Error While Searching For Endpoint", Toast.LENGTH_SHORT).show();
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

    // Use geocoding to turn text into location points for navigation, if then set navigation route accordingly
    @SuppressWarnings({"MissingPermission"})
    private void searchForEndpointCoordinates(final String endpoint) throws ServicesException {

        // Get Coordinates For Endpoint
        MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                .accessToken(getString(R.string.access_token))
                .query(endpoint)
                .build();

        mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                List<CarmenFeature> results = response.body().features();
                CarmenFeature mainResult = results.get(0);
                if (results.size() > 0) {

                    try {
                        LatLng destinationLatLang = new LatLng(mainResult.center().latitude(), mainResult.center().longitude());
                        destinationPoint = Point.fromLngLat(destinationLatLang.getLongitude(), destinationLatLang.getLatitude());
                        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                                locationComponent.getLastKnownLocation().getLatitude());
                        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
                        if (source != null) {
                            source.setGeoJson(Feature.fromGeometry(destinationPoint));
                        }
                        getRoute(originPoint, destinationPoint);
                        button.setEnabled(true);
                        button.setBackgroundResource(R.color.mapboxBlue);

                    } catch (ServicesException e) {
                        Log.e(TAG, "Exception While Searching for Directions: " + e);
                        Toast.makeText(DetectionAndNavigation.this, "Error While Searching For Directions", Toast.LENGTH_SHORT).show();
                    }
                    // Log the first results Point.
                } else {
                    // Notify user endpoint not found if unable to pinpoint
                    tts.speak("Could not find the location: " + endpoint, TextToSpeech.QUEUE_FLUSH, null);
                }
            }

            // handle geocoding failures
            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                Toast.makeText(getApplicationContext(),
                        "Failure occurred while trying to find a coordinate for '" + endpoint + "'",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // set the navigation route with optional parameters, and set current route on completion
    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            tts.speak("No routes found to: " + routeAttempt, TextToSpeech.QUEUE_FLUSH, null);
                            return;
                        } else {
                            currentRoute = response.body().routes().get(0);
                            MapboxGeocoding reverseGeocode = MapboxGeocoding.builder()
                                    .accessToken(getString(R.string.access_token))
                                    .query(destination)
                                    .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                                    .build();
                            reverseGeocode.enqueueCall(new Callback<GeocodingResponse>() {
                                @Override
                                public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

                                    List<CarmenFeature> results = response.body().features();

                                    if (results.size() > 0) {
                                        // Log the first results Point.
                                        tts.speak("Route set to: " + results.get(0).placeName(), TextToSpeech.QUEUE_FLUSH, null);

                                    } else {
                                        // No result for your request were found.
                                        Log.d(TAG, "onResponse: No result found");

                                    }
                                }

                                @Override
                                public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                                    throwable.printStackTrace();
                                }
                            });

                            // Draw the route on the map and remove any old routes
                            if (navigationMapRoute != null) {
                                navigationMapRoute.removeRoute();
                            } else {
                                navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                            }
                            navigationMapRoute.addRoute(currentRoute);
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }

    // Checks if location permissions exist and enables it by the users choice
    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Every time a users progress is updated in navigation, provide the user location and their progress along route
    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        try {
            // set a global var with the current steps progress
            progLen = (float)(routeProgress.currentLegProgress().currentStepProgress().distanceRemaining() / 0.3048);
            // update the current origin point for successive starts and stops
            origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
            Log.d("Real progress", Double.toString(routeProgress.currentLegProgress().currentStepProgress().distanceRemaining() / 0.3048));
            Log.d("Real progress Instr", routeProgress.bannerInstruction().getPrimary().getText());
        } catch (Exception e) {

        }
        locationComponent.forceLocationUpdate(location);
    }

    // Fires when a milestone event has occured and provides the routeprogress, instruction, and specific milestone
    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
        // log the custom milestone
        if (milestone.getIdentifier() == 50) {
            Log.d("Milestone Event", "50");
        }
        try {
            // set global var on milestones also
            progLen = (float)(routeProgress.currentLegProgress().currentStepProgress().distanceRemaining() / 0.3048);
        } catch (Exception e) {

        }
    }

    // Fires before a voice announcement is about to be made with the announcement
    @Override
    public SpeechAnnouncement willVoice(SpeechAnnouncement announcement) {
        String progInst = announcement.announcement();
        // used for if in simulation mode (demo) or regular mode (full application)
        float simulateUse;
        if (simulateRoute) {
            simulateUse = .6f;
        }
        else {
            simulateUse = average_step_length;
        }

        // Convert distance to an amount of steps based on step length
        float stepsTillDirection = .3048f * (progLen / simulateUse);

        // Format into new instruction specifying the steps and rebuild announcment
        String intSteps = Integer.toString((int)(stepsTillDirection));
        String formated = "In " + intSteps.concat("steps, ".concat(progInst));
        String newSSML = "<speak><amazon:effect name=\"drc\"><prosody rate=\"1.08\">" + formated + "</prosody></amazon:effect></speak>";
        SpeechAnnouncement newAnnounce = announcement.toBuilder()
                .announcement(formated)
                .ssmlAnnouncement(newSSML)
                .voiceInstructionMilestone(null)
                .build();

        // Only return new announcement if it is a close one
        if (stepsTillDirection < 50) {
            return newAnnounce;
        }
        // otherwise return the old announcement
        return announcement;
    }

    // Occurs before the instructions banner will be updated, only needed for demo purpose
    @Override
    public BannerInstructions willDisplay(BannerInstructions instructions) {
        return instructions;
    }

    // when navigation is ready to run, return if it is running
    @Override
    public void onNavigationReady(boolean isRunning) {

    }

    // Methods handling navigation lifecycle
    @Override
    public void onCancelNavigation() {

    }
    @Override
    public void onNavigationFinished() {

    }
    @Override
    public void onNavigationRunning() {

    }



    // Notifies user to enable location permission
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    // notifies when a permission is granted or denied
    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // get the mean of step length
    private float getMean(ArrayList<Float> vals) {
        int size = vals.size();
        float sum = 0f;
        for (Float val : vals) sum += val;
        return sum / (float) size;
    }

    // get variance of step length
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

    // get the pure step length
    private void getStepLength() {
        float newTime = SystemClock.elapsedRealtime();
        float time = newTime - last_step_time;
        float freq = 1f / time;
        float var = getVariance(accels);
        float length = coef[0] * height * freq +
                coef[1] * height * var +
                coef[2] * height +
                coef[3];
        steps.add(length);
        if (steps.size() > 3) steps.remove(0);
        average_step_length = getMean(steps);
        last_step_time = newTime;
    }

    // Fires if accuracy of sensors changes
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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


    public void plotData(View view) {

        Intent newIntent = new Intent(DetectionAndNavigation.this, plottingChart.class);
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
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
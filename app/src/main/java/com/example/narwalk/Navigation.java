package com.example.narwalk;

import android.support.v4.util.Pair;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.speech.RecognizerIntent;
import android.widget.Toast;


import com.example.narwalk.R;
import com.google.gson.JsonElement;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import static com.mapbox.core.constants.Constants.PRECISION_6;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationHelper;
import com.mapbox.mapboxsdk.Mapbox;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;

// classes needed to add a marker
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

// classes to calculate a route
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
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

// classes needed to launch navigation UI
import android.view.View;
import android.widget.Button;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class Navigation extends AppCompatActivity implements MapboxMap.OnMapClickListener, OnMapReadyCallback, PermissionsListener,
        ProgressChangeListener, MilestoneEventListener,  OnNavigationReadyCallback {

    // variables to initialize map - TODO: Remove map
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
    private String routeReal;
    private MapboxNavigation mapboxRaw;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_navigation);
        navigationView = findViewById(R.id.navigationView);
        // TODO: Remove mapView
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

    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                // Ask for location privileges - TODO: Make sure everything routes through here
                enableLocationComponent(style);

                addDestinationIconSymbolLayer(style);

                mapboxMap.addOnMapClickListener(Navigation.this);


                // Begin navigation button, not currently needed for speech
                button = findViewById(R.id.startButton);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isNavigating) {
                            boolean simulateRoute = true;
                            NavigationViewOptions navOptions = NavigationViewOptions.builder()
                                    .progressChangeListener(Navigation.this::onProgressChange)
                                    .milestoneEventListener(Navigation.this::onMilestoneEvent)
                                    .directionsRoute(currentRoute)
                                    .shouldSimulateRoute(simulateRoute)
                                    .build();

                            // Call this method with Context from within an Activity
                            mapboxRaw = navigationView.retrieveMapboxNavigation();
                            navigationView.startNavigation(navOptions);
                            button.setText("Stop Navigation");
                            isNavigating = true;
                            tts = new TextToSpeech(Navigation.this, new TextToSpeech.OnInitListener() {

                                @Override
                                public void onInit(int status) {
                                    if (status == TextToSpeech.SUCCESS) {
                                        int result = tts.setLanguage(Locale.US);
                                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                            Log.e("error", "This Language is not supported");
                                        } else {
                                            tts.speak("Starting navigation" , TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                    } else
                                        Log.e("error", "Initilization Failed!");
                                }
                            });

                        }
                        else {
                            navigationView.stopNavigation();
                            button.setText("Start Navigation");
                            isNavigating = false;
                        }

                    }
                });

                // Speech regocnition button
                fab = (FloatingActionButton) findViewById(R.id.fab);
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

    // Used for clicking a point to set a destination for navigation
    @SuppressWarnings( {"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        // Convert LatLng coordinates to screen pixel and only query the rendered features.


        Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
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


    // Callback for the activity run
    // TODO: Handle all edge cases and null situations
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
                            if (text.get(0).equalsIgnoreCase("Start")) {
                                button = findViewById(R.id.startButton);
                                button.callOnClick();
                            }
                            else {
                                searchForEndpointCoordinates(text.get(0));
                                routeAttempt = text.get(0);
                            }
                        } catch (ServicesException e) {
                            Log.e(TAG, "Exception While Searching:" + e.getMessage());
                            Toast.makeText(Navigation.this, "Error While Searching For Endpoint", Toast.LENGTH_SHORT).show();
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
    @SuppressWarnings( {"MissingPermission"})
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
                        Point destinationPoint = Point.fromLngLat(destinationLatLang.getLongitude(), destinationLatLang.getLatitude());
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
                        Log.e(TAG, "Exception While Searching for Directions: "+ e);
                        Toast.makeText(Navigation.this, "Error While Searching For Directions", Toast.LENGTH_SHORT).show();
                    }
                    // Log the first results Point.

                } else {

                    tts=new TextToSpeech(Navigation.this, new TextToSpeech.OnInitListener() {

                        @Override
                        public void onInit(int status) {
                            if(status == TextToSpeech.SUCCESS){
                                int result=tts.setLanguage(Locale.US);
                                if(result==TextToSpeech.LANG_MISSING_DATA || result==TextToSpeech.LANG_NOT_SUPPORTED){
                                    Log.e("error", "This Language is not supported");
                                }
                                else{
                                    tts.speak("Could not find the location: " + endpoint, TextToSpeech.QUEUE_FLUSH, null);
                                }
                            }
                            else
                                Log.e("error", "Initilization Failed!");
                        }
                    });
                }
            }

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
                            tts = new TextToSpeech(Navigation.this, new TextToSpeech.OnInitListener() {

                                @Override
                                public void onInit(int status) {
                                    if (status == TextToSpeech.SUCCESS) {
                                        int result = tts.setLanguage(Locale.US);
                                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                            Log.e("error", "This Language is not supported");
                                        } else {
                                            tts.speak("No routes found to: " + routeAttempt, TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                    } else
                                        Log.e("error", "Initilization Failed!");
                                }
                            });
                            return;
                        } else {
                            currentRoute = response.body().routes().get(0);
                            routeReal = routeAttempt;
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
                                        tts = new TextToSpeech(Navigation.this, new TextToSpeech.OnInitListener() {

                                            @Override
                                            public void onInit(int status) {
                                                if (status == TextToSpeech.SUCCESS) {
                                                    int result = tts.setLanguage(Locale.US);
                                                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                                        Log.e("error", "This Language is not supported");
                                                    } else {
                                                        tts.speak("Route set to: " + results.get(0).placeName(), TextToSpeech.QUEUE_FLUSH, null);
                                                    }
                                                } else
                                                    Log.e("error", "Initilization Failed!");
                                            }
                                        });

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

                            // Draw the route on the map
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

    // Checks if location permissions exist and enables it
    @SuppressWarnings( {"MissingPermission"})
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

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        // TODO: Use this to track intersections

        /*List<LegStep> steps = routeProgress.currentLeg().steps();
        LegStep currentStep = steps.get(routeProgress.currentLegProgress().stepIndex());
        String currentStepGeometry = currentStep.geometry();
        List<Point> currentStepPoints = PolylineUtils.decode(currentStepGeometry, PRECISION_6);
        int upcomingStepIndex = routeProgress.currentLegProgress().stepIndex() + 1;
        LegStep upcomingStep = null;
        if (upcomingStepIndex < steps.size()) {
            upcomingStep = steps.get(upcomingStepIndex);
        }
        List<StepIntersection> intersections = NavigationHelper.createIntersectionsList(currentStep, upcomingStep);
        List<Pair<StepIntersection, Double>> intersectionDistances = NavigationHelper.createDistancesToIntersections(
                currentStepPoints, intersections
        );

        double stepDistanceTraveled = currentStep.distance() - routeProgress.durationRemaining();
        StepIntersection currentIntersection = NavigationHelper.findCurrentIntersection(intersections,
                intersectionDistances, stepDistanceTraveled
        );
        StepIntersection upcomingIntersection = NavigationHelper.findUpcomingIntersection(
                intersections, upcomingStep, currentIntersection
        );
        */

        StepIntersection upcomingIntersection = routeProgress.currentLegProgress().currentStepProgress().upcomingIntersection();
        if (upcomingIntersection != null) {
            LatLng destinationLatLang = new LatLng(upcomingIntersection.location().latitude(), upcomingIntersection.location().longitude());
            Point destinationPoint = Point.fromLngLat(destinationLatLang.getLongitude(), destinationLatLang.getLatitude());
            GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
            if (source != null) {
                source.setGeoJson(Feature.fromGeometry(destinationPoint));
               /* mapboxRaw.addMilestone(new StepMilestone.Builder().setIdentifier(100).setTrigger(Trigger.all(
                        Trigger.lt(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, 1)
                )).build());
                */
            }
        }


    }

    @Override
    public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
        Log.d("instruction", instruction);
    }

    @Override
    public void onNavigationReady(boolean isRunning) {

    }

    // Notifies user to enable location permission
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
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
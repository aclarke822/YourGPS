package net.austinclarke.yourgps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback,
        LocationListener,
        View.OnClickListener,
        View.OnLongClickListener,
        View.OnTouchListener,
        Cloneable,
        Serializable {

    private static final LatLng southWestBounds = new LatLng(-180, -90);
    private static final LatLng northEastBounds = new LatLng(180, 90);
    private static final LatLngBounds latLngBounds = new LatLngBounds(southWestBounds, northEastBounds);
    private static final CharacterStyle STYLE_BOLD = new StyleSpan(Typeface.BOLD);
    private static final Location GOOGLEPLEX = new Location("GOOGLEPLEX");
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] PERMISSIONS_LOCATION = {android.Manifest.permission.ACCESS_FINE_LOCATION};
    private static final String[] MAP_NAMES = {"Normal", "Satellite", "Hybrid", "Terrain", "None"};
    private static final int[] MAP_TYPES = {GoogleMap.MAP_TYPE_NORMAL, GoogleMap.MAP_TYPE_SATELLITE, GoogleMap.MAP_TYPE_HYBRID, GoogleMap.MAP_TYPE_TERRAIN, GoogleMap.MAP_TYPE_NONE};
    private static final int PERMISSIONS_REQUEST_ACCESS_LOCATION = 100;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static MapFragment mapFragment;

    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    ArrayList<ArrayList<Location>> pathList = new ArrayList<>();
    ArrayList<String> pathNameList = new ArrayList<>();
    ArrayList<Integer> pathSpeed = new ArrayList<>();
    ArrayList<Integer> pathWander = new ArrayList<>();
    ArrayList<ArrayList<String>> locationNameList = new ArrayList<>();
    ArrayList<ArrayList<Integer>> locationWaitTime = new ArrayList<>();
    ArrayList<Integer> totalLocationsInPathList = new ArrayList<>();
    ArrayList<Integer> pathUpdateInterval = new ArrayList<>();
    private GoogleApiClient googleApiClient;
    private GoogleMap userMap;
    private LocationRequest locationRequest;
    private Location lastRealUserLocation = new Location("lastRealUserLocation");
    private Location currentRealUserLocation = new Location("currentRealUserLocation");
    private Location lastMockUserLocation = new Location("lastMockUserLocation");
    private Location currentMockUserLocation = new Location("currentMockUserLocation");
    private Location locationToBeMocked = new Location("locationToBeMocked");
    private Location lastMarkerLocation = new Location("lastMarkerLocation");
    private Location currentMarkerLocation = new Location("currentMarkerLocation");
    private Location lastUserLocation = new Location("lastUserLocation");
    private Location currentUserLocation = new Location("currentUserLocation");
    private ImageView customLocationButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationViewLeft;
    private NavigationView navigationViewRight;
    private FloatingSearchView floatingSearchView;
    private CoordinatorLayout coordinatorLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private View autoCompleteFragmentContainer;
    private boolean isToolbarVisible;
    private boolean isUserReady;
    private int mapState;
    private boolean isLocationButtonActive;
    private boolean isMockEnabled;
    private boolean isTrafficEnabled;
    private boolean locationPermissionsGiven;
    private Bundle savedState;
    private String locationTask;
    private String mockTask;
    private String quickMessage;
    private boolean steadyMockState = false;
    private Handler handler = new Handler();
    private Intent intent;
    private SharedPreferences preferences;
    private int pathSelected;
    private int totalPaths;
    private int locationSelected;
    private Snackbar snackbar;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            double adjust;
            locationToBeMocked = currentUserLocation;
            Random r = new Random();
            adjust = (1.01 - 0.99) * r.nextFloat() + 0.99;
            locationToBeMocked.setLatitude(locationToBeMocked.getLatitude() * adjust);
            adjust = (1.01 - 0.99) * r.nextFloat() + 0.99;
            locationToBeMocked.setLongitude(locationToBeMocked.getLongitude() * adjust);
            locationToBeMocked.setTime(new Date().getTime());
            locationToBeMocked.setAccuracy(r.nextFloat());
            locationToBeMocked.setSpeed(r.nextFloat());
            locationToBeMocked.setBearing(360 * r.nextFloat());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                locationToBeMocked.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }
            setMockLocation(locationToBeMocked);
            if (steadyMockState) {
                startRandomMock();
            }
        }
    };
    private ResultCallback<AutocompletePredictionBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<AutocompletePredictionBuffer>() {
        @Override
        public void onResult(AutocompletePredictionBuffer places) {

            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                showShortToast("Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            //java.util.List<? extends com.arlib.floatingsearchview.suggestions.model.SearchSuggestion> search;

            Parcel parcel = Parcel.obtain();
            parcel.writeString("test");
            parcel.setDataPosition(0);

            //ArrayList<Object> typedArrayList = parcel.createTypedArrayList(new ArrayList<SearchSuggestion>);
            ArrayList<SearchSuggestion> search = new ArrayList<SearchSuggestion>() {
            };

            SearchSuggestion element = new SearchSuggestion() {
                @Override
                public int describeContents() {
                    return 0;
                }

                @Override
                public void writeToParcel(Parcel parcel, int i) {

                }

                @Override
                public String getBody() {
                    return null;
                }
            };
            showShortToast(element.getBody());

            //for (int i = 0 ; i < 5;) {}

            floatingSearchView.swapSuggestions(search);
            //showShortToast(Integer.toString(places.getCount()));

            places.release();
        }
    };

    /**
     * Listeners-------------------------------------------------------------------
     **/
    //Activity lifecycle-----------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);
        showShortToast(TAG + " created.");

        GOOGLEPLEX.setLatitude(37.422535);
        GOOGLEPLEX.setLongitude(-122.084804);
        GOOGLEPLEX.setAccuracy(1);

        isUserReady = false;

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            isToolbarVisible = savedInstanceState.getBoolean("isToolbarVisible", true);
            mapState = savedInstanceState.getInt("mapState", 0);
            isLocationButtonActive = savedInstanceState.getBoolean("isLocationButtonActive", false);
            isMockEnabled = savedInstanceState.getBoolean("isMockEnabled", false);
            isTrafficEnabled = savedInstanceState.getBoolean("isTrafficEnabled", false);
            savedState = savedInstanceState;
        } else {
            isToolbarVisible = true;
            mapState = 0;
            isLocationButtonActive = false;
            isMockEnabled = false;
            isTrafficEnabled = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#7dc6c8c9"));
        }

        currentUserLocation = GOOGLEPLEX;
        locationToBeMocked = currentUserLocation;
        intent = new Intent(this, SettingsActivity.class);

        //setupToolbar();
        initViews();
        initLocationServices();
        retrievePreferences();
    }

    @Override
    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        googleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            stopLocationUpdates();
            disableMyLocation();
            if (steadyMockState) {
                stopRandomMock();
            }
            googleApiClient.disconnect();
        }
        savePreferences();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            stopLocationUpdates();
            disableMyLocation();
            if (steadyMockState) {
                stopRandomMock();
            }
            googleApiClient.disconnect();
        }
    }
    //Activity lifecycle-----------------------------------------------

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("isToolbarVisible", isToolbarVisible);
        bundle.putInt("mapState", mapState);
        bundle.putBoolean("isLocationButtonActive", isLocationButtonActive);
        bundle.putBoolean("isMockEnabled", isMockEnabled);
        bundle.putBoolean("isTrafficEnabled", isTrafficEnabled);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        showLongToast("Destroyed.");
        if (googleApiClient != null && googleApiClient.isConnected()) {
            stopLocationUpdates();
            disableMyLocation();
            stopRandomMock();
            googleApiClient.disconnect();
            googleApiClient = null;
        }
        removeListeners();
    }

    //GoogleMaps API services------------------------------------------
    @Override
    public void onConnected(Bundle bundle) {
        showShortToast("Location services connected.");
        initCamera(currentUserLocation);
        if (savedState != null) {
            initPreviousInstance();
        }
        enableMyLocation();
        startLocationUpdates();
    }
    //GoogleMaps API services------------------------------------------

    @Override
    public void onConnectionSuspended(int i) {
        showShortToast("Location services are suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Create Tasks default location if the Google API Client fails. Placing location at Googleplex
        showShortToast("Location services connection failed.");
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                showShortToast("Location services connection failed with resolution.");
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
                showShortToast("Location services connection failed with exception.");
            }
        } else {
            showShortToast("Location services connection failed with error code: " + connectionResult.getErrorCode());
        }
    }

    //GoogleMaps map---------------------------------------------------
    @Override
    public void onMapReady(GoogleMap googleMap) {
        userMap = googleMap;
        getMap().setMapType(MAP_TYPES[mapState]);
        getMap().setTrafficEnabled(false);
        getMap().getUiSettings().setZoomControlsEnabled(true);
        //getMap().getUiSettings().setCompassEnabled(true);
        getMap().getUiSettings().setMyLocationButtonEnabled(false);
        getMap().setPadding(0, 225, 0, 80);
        initListeners();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        toggleToolbarState();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (currentMarkerLocation != null) {
            lastMarkerLocation = currentMarkerLocation;
        }
        currentMarkerLocation = getLocationFromLatLng(latLng);
        locationToBeMocked = currentMarkerLocation;
        createMarker(currentMarkerLocation);

        snackbar = Snackbar.make(coordinatorLayout, getAddressFromLatLng(getLatLngFromLocation(locationToBeMocked)), Snackbar.LENGTH_INDEFINITE);
        if (locationSelected > -1) {
            snackbar.setAction("Set Location", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setPathLocation();

                }
            });
            snackbar.show();
            return;
        }
        if (pathSelected > -1) {
            snackbar.setAction("Add Location to Path", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addNewLocation();
                    locationSelected = -1;
                    createLocationMenu();
                    pathList.get(pathSelected).set(totalLocationsInPathList.get(pathSelected) - 1, currentMarkerLocation);
                    plotPathMarkers();

                }
            });
            snackbar.show();
            return;
        }


    }

    @Override
    public void onLocationChanged(Location location) {
        if (isLocationButtonActive) {
            zoomCamera(location);
        }
        handleNewLocation(location);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
    //GoogleMaps map---------------------------------------------------

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.locationButton: {
                zoomCamera(getLastLocationFromApp());
            }
            break;
        }
    }
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return true;
    }
    */
    //Options menu-----------------------------------------------------

    /**
     * Listeners-------------------------------------------------------------------
     **/

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.locationButton: {
                toggleLocationButtonState();
            }
        }
        return false;
    }

    //Options menu-----------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_drawer, menu);
        return true;
    }

    /**
     * Actions---------------------------------------------------------------------
     **/
    private void toggleTrafficState() {
        isTrafficEnabled = !isTrafficEnabled;
        setTrafficState();
    }

    private void setTrafficState() {
        getMap().setTrafficEnabled(isTrafficEnabled);
        navigationViewLeft.getMenu().findItem(R.id.toggle_traffic).setChecked(isTrafficEnabled);
    }

    private void zoomCamera(Location location) {
        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
        this.userMap.moveCamera(center);
        //CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        //this.userMap.animateCamera(zoom);
    }

    private void setMapType() {
        showShortToast(MAP_NAMES[mapState]);
        getMap().setMapType(MAP_TYPES[mapState]);
        switch (mapState) {
            case 0:
                navigationViewLeft.getMenu().findItem(R.id.satellite).setChecked(false);
                navigationViewLeft.getMenu().findItem(R.id.hybrid).setChecked(false);
                navigationViewLeft.getMenu().findItem(R.id.terrain).setChecked(false);
                break;
            case 1:
                navigationViewLeft.getMenu().findItem(R.id.satellite).setChecked(true);
                navigationViewLeft.getMenu().findItem(R.id.hybrid).setChecked(false);
                navigationViewLeft.getMenu().findItem(R.id.terrain).setChecked(false);
                break;
            case 2:
                navigationViewLeft.getMenu().findItem(R.id.satellite).setChecked(false);
                navigationViewLeft.getMenu().findItem(R.id.hybrid).setChecked(true);
                navigationViewLeft.getMenu().findItem(R.id.terrain).setChecked(false);
                break;
            case 3:
                navigationViewLeft.getMenu().findItem(R.id.satellite).setChecked(false);
                navigationViewLeft.getMenu().findItem(R.id.hybrid).setChecked(false);
                navigationViewLeft.getMenu().findItem(R.id.terrain).setChecked(true);
                break;
        }
    }


    private void pathUpdateIntervalDialog() {
        if (pathSelected > -1 && pathSelected < totalPaths) {
            navigationViewRight.getMenu().findItem(1080).setChecked(true);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Update Interval (ms)");
            builder.setIcon(R.drawable.ic_speed);

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigationViewRight.getMenu().findItem(1080).setChecked(false);
                    String text = input.getText().toString();
                    setUpdateInterval(Integer.parseInt(text));
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigationViewRight.getMenu().findItem(1080).setChecked(false);
                    dialog.cancel();
                }
            });

            builder.show();
        } else {
            showShortToast("No path selected!");
        }
    }

    private void setUpdateInterval(int i) {
        pathUpdateInterval.set(pathSelected, i);
        createEditPathMenu();
    }

    private void locationWaitDialog() {
        if (locationSelected > -1 && locationSelected < totalLocationsInPathList.get(pathSelected)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Wait");
            builder.setIcon(R.drawable.ic_wait);
            navigationViewRight.getMenu().findItem(1160).setChecked(true);

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text

            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("Wait", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = input.getText().toString();
                    setWait(Integer.parseInt(text));
                    navigationViewRight.getMenu().findItem(1160).setChecked(false);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigationViewRight.getMenu().findItem(1160).setChecked(false);
                    dialog.cancel();
                }
            });

            builder.show();
        } else {
            showShortToast("No location selected!");
        }
    }

    private void setWait(int i) {
        locationWaitTime.get(pathSelected).set(locationSelected, i);
        createEditLocationMenu();
    }

    private void pathSpeedDialog() {
        if (pathSelected > -1 && pathSelected < totalPaths) {
            navigationViewRight.getMenu().findItem(1070).setChecked(true);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Speed (m/s)");
            builder.setIcon(R.drawable.ic_speed);

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigationViewRight.getMenu().findItem(1070).setChecked(false);
                    String text = input.getText().toString();
                    setSpeed(Integer.parseInt(text));
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigationViewRight.getMenu().findItem(1070).setChecked(false);
                    dialog.cancel();
                }
            });

            builder.show();
        } else {
            showShortToast("No path selected!");
        }
    }

    private void pathWanderDialog() {
        if (pathSelected > -1 && pathSelected < totalPaths) {
            navigationViewRight.getMenu().findItem(1060).setChecked(true);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Wander (m)");
            builder.setIcon(R.drawable.ic_wander);

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = input.getText().toString();
                    setWander(Integer.parseInt(text));
                    navigationViewRight.getMenu().findItem(1060).setChecked(false);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigationViewRight.getMenu().findItem(1060).setChecked(false);
                    dialog.cancel();
                }
            });

            builder.show();
        } else {
            showShortToast("No path selected!");
        }
    }

    private void setWander(int i) {
        pathWander.set(pathSelected, i);
        createEditPathMenu();
    }

    private void setSpeed(int i) {
        pathSpeed.set(pathSelected, i);
        createEditPathMenu();
    }

    private void renamePathDialog() {
        if (pathSelected > -1 && pathSelected < totalPaths) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Rename Path");
            builder.setIcon(R.drawable.ic_rename);

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = input.getText().toString();
                    renamePath(text);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        } else {
            showShortToast("No path selected!");
        }
    }

    private void renamePath(String text) {
        pathNameList.set(pathSelected, text);
        createPathMenu();
        createEditPathMenu();
    }

    private void renameLocationDialog() {
        if (locationSelected > -1 && locationSelected < totalLocationsInPathList.get(pathSelected)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Rename Location");
            builder.setIcon(R.drawable.ic_rename);
            navigationViewRight.getMenu().findItem(1150).setChecked(true);

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text

            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = input.getText().toString();
                    renameLocation(text);
                    navigationViewRight.getMenu().findItem(1150).setChecked(false);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigationViewRight.getMenu().findItem(1150).setChecked(false);
                    dialog.cancel();
                }
            });

            builder.show();
        } else {
            showShortToast("No location selected!");
        }
    }

    private void renameLocation(String text) {
        locationNameList.get(pathSelected).set(locationSelected, text);
        createLocationMenu();
        createEditLocationMenu();
    }

    private void movePathDown() {
        if ((pathSelected > -1) && pathSelected + 1 < totalPaths) {
            ArrayList<Location> tempPath = pathList.get(pathSelected);
            int tempWander = pathWander.get(pathSelected);
            String tempName = pathNameList.get(pathSelected);

            pathList.set(pathSelected, pathList.get(pathSelected + 1));
            pathList.set(pathSelected + 1, tempPath);

            pathUpdateInterval.set(pathSelected, pathUpdateInterval.get(pathSelected + 1));
            pathUpdateInterval.set(pathSelected + 1, tempWander);

            pathWander.set(pathSelected, pathWander.get(pathSelected + 1));
            pathWander.set(pathSelected + 1, tempWander);

            pathNameList.set(pathSelected, pathNameList.get(pathSelected + 1));
            pathNameList.set(pathSelected + 1, tempName);
            pathSelected++;
            createPathMenu();
            return;
        }
        if (!(totalPaths > 0)) {
            showShortToast("No paths!");
        }
        if (!(pathSelected > -1)) {
            showShortToast("No path selected!");
        } else {
            showShortToast("Already at bottom!");
        }
    }

    private void movePathUp() {
        if ((pathSelected > 0)) {
            ArrayList<Location> tempPath = pathList.get(pathSelected);
            int tempWander = pathWander.get(pathSelected);
            String tempName = pathNameList.get(pathSelected);

            pathList.set(pathSelected, pathList.get(pathSelected - 1));
            pathList.set(pathSelected - 1, tempPath);

            pathUpdateInterval.set(pathSelected, pathUpdateInterval.get(pathSelected - 1));
            pathUpdateInterval.set(pathSelected - 1, tempWander);

            pathWander.set(pathSelected, pathWander.get(pathSelected - 1));
            pathWander.set(pathSelected - 1, tempWander);

            pathNameList.set(pathSelected, pathNameList.get(pathSelected - 1));
            pathNameList.set(pathSelected - 1, tempName);
            pathSelected--;
            createPathMenu();
            return;
        }
        if (!(totalPaths > 0)) {
            showShortToast("No paths!");
        } else {
            showShortToast("Already at top!");
        }
    }

    private void moveLocationDown() {
        if ((locationSelected > -1) && locationSelected + 1 < totalLocationsInPathList.get(pathSelected)) {
            Location tempLocation = pathList.get(pathSelected).get(locationSelected);
            String tempName = locationNameList.get(pathSelected).get(locationSelected);
            int tempWaitTime = locationWaitTime.get(pathSelected).get(locationSelected);


            pathList.get(pathSelected).set(locationSelected, pathList.get(pathSelected).get(locationSelected + 1));
            pathList.get(pathSelected).set(locationSelected + 1, tempLocation);

            locationNameList.get(pathSelected).set(locationSelected, locationNameList.get(pathSelected).get(locationSelected + 1));
            locationNameList.get(pathSelected).set(locationSelected + 1, tempName);

            locationWaitTime.get(pathSelected).set(locationSelected, locationWaitTime.get(pathSelected).get(locationSelected + 1));
            locationWaitTime.get(pathSelected).set(locationSelected + 1, tempWaitTime);

            locationSelected++;
            createLocationMenu();
            return;
        }
        if (!(totalLocationsInPathList.get(pathSelected) > 0)) {
            showShortToast("No paths!");
        }
        if (!(locationSelected > -1)) {
            showShortToast("No path selected!");
        } else {
            showShortToast("Already at bottom!");
        }
    }

    private void moveLocationUp() {
        if ((locationSelected > 0)) {
            Location tempLocation = pathList.get(pathSelected).get(locationSelected);
            String tempName = locationNameList.get(pathSelected).get(locationSelected);
            int tempWaitTime = locationWaitTime.get(pathSelected).get(locationSelected);


            pathList.get(pathSelected).set(locationSelected, pathList.get(pathSelected).get(locationSelected - 1));
            pathList.get(pathSelected).set(locationSelected - 1, tempLocation);

            locationNameList.get(pathSelected).set(locationSelected, locationNameList.get(pathSelected).get(locationSelected - 1));
            locationNameList.get(pathSelected).set(locationSelected - 1, tempName);

            locationWaitTime.get(pathSelected).set(locationSelected, locationWaitTime.get(pathSelected).get(locationSelected - 1));
            locationWaitTime.get(pathSelected).set(locationSelected - 1, tempWaitTime);

            locationSelected--;
            createLocationMenu();
            return;
        }
        if (!(totalLocationsInPathList.get(pathSelected) > 0)) {
            showShortToast("No paths!");
        } else {
            showShortToast("Already at top!");
        }
    }

    private void onPathSelected(int menuItemNumber, MenuItem menuItem) {
        pathSelected = menuItemNumber - 100;
        if (menuItem.isChecked()) {
            createLocationMenu();

            if (totalLocationsInPathList.get(pathSelected) > 1) {
                plotPathMarkers();
                getNumberOfMidpoints(pathList.get(pathSelected));
            }
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                createEditLocationMenu();
            }
        } else {
            createEditPathMenu();
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    private void onLocationSelected(int menuItemNumber, MenuItem menuItem) {
        locationSelected = menuItemNumber - 300;
        if (menuItem.isChecked()) {
            menuItem.setChecked(false);
            if (snackbar != null) {
                snackbar.dismiss();
            }
            locationSelected = -1;
        } else {
            menuItem.setChecked(true);
        }
        createEditLocationMenu();
    }

    private void plotPathMarkers() {
        if (!(totalPaths > 0)) {
            showShortToast("No paths!");
            return;
        }
        if (!(pathSelected > -1)) {
            showShortToast("No path selected!");
            return;
        }
        getMap().clear();
        for (int i = 0; i < totalLocationsInPathList.get(pathSelected); i++)
            createMarker(pathList.get(pathSelected).get(i));
        if (totalLocationsInPathList.get(pathSelected) < 1) {
            showShortToast("No locations in path!");
        }
    }

    private void deletePath() {
        if (!(totalPaths > 0)) {
            showShortToast("No paths!");
            return;
        }
        if (!(pathSelected > -1)) {
            showShortToast("No path selected!");
            return;
        }

        pathList.remove(pathSelected);
        pathNameList.remove(pathSelected);
        pathWander.remove(pathSelected);
        pathSpeed.remove(pathSelected);
        pathUpdateInterval.remove(pathSelected);
        totalLocationsInPathList.remove(pathSelected);

        locationNameList.remove(pathSelected);
        locationWaitTime.remove(pathSelected);

        pathSelected = -1;
        totalPaths--;
        createPathMenu();
        createEditPathMenu();
    }

    private void addNewPath() {
        Random r = new Random();
        if (totalPaths < 100) {
            pathList.add(new ArrayList<Location>());
            pathNameList.add(Integer.toString(r.nextInt()));
            pathWander.add(0);
            pathSpeed.add(1);
            pathUpdateInterval.add(100);
            totalLocationsInPathList.add(0);

            locationNameList.add(new ArrayList<String>());
            locationWaitTime.add(new ArrayList<Integer>());

            totalPaths++;
        } else {
            showShortToast("Too many paths!");
        }
        createPathMenu();
        createEditPathMenu();
    }

    private void deleteLocation() {
        if (!(totalLocationsInPathList.get(pathSelected) > 0)) {
            showShortToast("No more locations!");
            return;
        }
        if (!(locationSelected > -1)) {
            showShortToast("No location selected!");
            return;
        }


        pathList.get(pathSelected).remove(locationSelected);
        locationNameList.get(pathSelected).remove(locationSelected);
        totalLocationsInPathList.set(pathSelected, totalLocationsInPathList.get(pathSelected) - 1);
        locationSelected = -1;
        createLocationMenu();
        createEditLocationMenu();
        plotPathMarkers();
    }

    private void addNewLocation() {
        Random r = new Random();
        if (totalLocationsInPathList.get(pathSelected) < 100) {
            pathList.get(pathSelected).add(getRandomLocation());
            locationNameList.get(pathSelected).add(Integer.toString(r.nextInt()));
            totalLocationsInPathList.set(pathSelected, totalLocationsInPathList.get(pathSelected) + 1);
            locationWaitTime.get(pathSelected).add(0);
        } else {
            showShortToast("Too many locations!");
        }
        plotPathMarkers();
        createLocationMenu();
        createEditLocationMenu();
    }

    private void recreateMainMenu() {
        TextView navigationHeaderText = (TextView) findViewById(R.id.left_header_title);
        ImageView navigationHeaderImage = (ImageView) findViewById(R.id.left_header_icon);
        CharSequence text = "Your GPS";
        if (navigationHeaderText != null) {
            navigationHeaderText.setText(text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                navigationHeaderImage.setImageDrawable(getDrawable(R.drawable.ic_navigation_icon));
            }
        }

        Menu menu = navigationViewLeft.getMenu();
        menu.clear();
        navigationViewLeft.inflateMenu(R.menu.activity_main_drawer);
        initPreviousInstance();
        navigationViewLeft.getMenu().findItem(R.id.action_steady_random_mock).setChecked(steadyMockState);
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
        if (pathSelected > -1) {
            navigationViewLeft.getMenu().findItem(R.id.action_open_paths).setChecked(true);
        }

    }

    private void createPathMenu() {
        TextView navigationHeaderText = (TextView) findViewById(R.id.left_header_title);
        ImageView navigationHeaderImage = (ImageView) findViewById(R.id.left_header_icon);
        CharSequence text = "Your Paths";
        if (navigationHeaderText != null) {
            navigationHeaderText.setText(text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                navigationHeaderImage.setImageDrawable(getDrawable(R.drawable.ic_paths));
            }
        }

        Menu menu = navigationViewLeft.getMenu();
        menu.clear();
        menu.add(0, 98, 98, "Back").setIcon(R.drawable.ic_arrow_back);
        menu.add(1, 99, 99, "Edit Paths").setIcon(R.drawable.ic_edit);

        for (int j = 0; j < totalPaths; j++) {
            menu.add(2, 100 + j, 100 + j, pathNameList.get(j));
        }

        if (pathSelected > -1) {
            menu.findItem(pathSelected + 100).setChecked(true);
        }

        menu.setGroupCheckable(2, true, true);
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            navigationViewLeft.getMenu().findItem(99).setChecked(true);
        } else {
            navigationViewLeft.getMenu().findItem(99).setChecked(false);
        }

        addFiveBlankMenuItems(menu, 2);
        savePreferences();
    }

    private void createLocationMenu() {
        TextView navigationHeaderText = (TextView) findViewById(R.id.left_header_title);
        ImageView navigationHeaderImage = (ImageView) findViewById(R.id.left_header_icon);
        CharSequence text = pathNameList.get(pathSelected);
        if (navigationHeaderText != null) {
            navigationHeaderText.setText(text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                navigationHeaderImage.setImageDrawable(getDrawable(R.drawable.ic_location));
            }
        }

        Menu menu = navigationViewLeft.getMenu();
        menu.clear();
        menu.add(0, 298, 298, "Back").setIcon(R.drawable.ic_arrow_back);
        menu.add(1, 299, 299, "Edit Locations").setIcon(R.drawable.ic_edit);

        for (int j = 0; j < totalLocationsInPathList.get(pathSelected); j++) {
            menu.add(2, 300 + j, 300 + j, locationNameList.get(pathSelected).get(j));
        }

        menu.setGroupCheckable(2, true, true);

        if (locationSelected > -1) {
            menu.findItem(locationSelected + 300).setChecked(true);
        }

        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            navigationViewLeft.getMenu().findItem(299).setChecked(true);
        } else {
            navigationViewLeft.getMenu().findItem(299).setChecked(false);
        }

        addFiveBlankMenuItems(menu, 2);
        savePreferences();
    }

    private void createEditPathMenu() {
        TextView navigationHeaderText = (TextView) findViewById(R.id.right_header_title);
        CharSequence text = "Path Edit";
        if (navigationHeaderText != null) {
            navigationHeaderText.setText(text);
        }
        Menu menu = navigationViewRight.getMenu();
        menu.clear();
        navigationViewRight.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        menu.add(0, 1000, 1000, "Close").setIcon(R.drawable.ic_close);
        menu.add(1, 1010, 1010, "New Path").setIcon(R.drawable.ic_add);
        menu.add(1, 1020, 1020, "Delete Path").setIcon(R.drawable.ic_remove);
        menu.add(1, 1030, 1030, "Move Up").setIcon(R.drawable.ic_arrow_up);
        menu.add(1, 1040, 1040, "Move Down").setIcon(R.drawable.ic_arrow_down);
        menu.add(1, 1050, 1050, "Rename").setIcon(R.drawable.ic_rename);
        menu.add(1, 1060, 1060, "Wander (m)").setIcon(R.drawable.ic_wander);
        menu.add(1, 1070, 1070, "Speed (m/s)").setIcon(R.drawable.ic_speed);
        menu.add(1, 1080, 1080, "Update (ms)").setIcon(R.drawable.ic_update);
        menu.setGroupCheckable(1, false, false);
        if (pathSelected > -1) {
            menu.add(1, 1051, 1051, "    " + pathNameList.get(pathSelected));
            menu.add(1, 1061, 1061, "    " + Integer.toString(pathWander.get(pathSelected)));
            menu.add(1, 1071, 1071, "    " + Integer.toString(pathSpeed.get(pathSelected)));
            menu.add(1, 1081, 1081, "    " + Integer.toString(pathUpdateInterval.get(pathSelected)));
        }

        //Some spare menu items to add space to scroll down
        addFiveBlankMenuItems(menu, 1);
        savePreferences();

    }

    private void createEditLocationMenu() {
        TextView navigationHeaderText = (TextView) findViewById(R.id.right_header_title);
        CharSequence text = "Location Edit";
        if (navigationHeaderText != null) {
            navigationHeaderText.setText(text);
        }
        Menu menu = navigationViewRight.getMenu();
        menu.clear();
        navigationViewRight.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        menu.add(0, 1100, 1100, "Close").setIcon(R.drawable.ic_close);
        menu.add(1, 1110, 1110, "New Location").setIcon(R.drawable.ic_add);
        menu.add(1, 1120, 1120, "Delete Location").setIcon(R.drawable.ic_remove);
        menu.add(1, 1130, 1130, "Move Up").setIcon(R.drawable.ic_arrow_up);
        menu.add(1, 1140, 1140, "Move Down").setIcon(R.drawable.ic_arrow_down);
        menu.add(1, 1150, 1150, "Rename").setIcon(R.drawable.ic_rename);
        menu.add(1, 1160, 1160, "Wait Time (s)").setIcon(R.drawable.ic_wait);
        menu.setGroupCheckable(1, false, false);
        if (locationSelected > -1) {
            menu.add(1, 1151, 1151, "    " + locationNameList.get(pathSelected).get(locationSelected));
            menu.add(1, 1161, 1161, "    " + Integer.toString(locationWaitTime.get(pathSelected).get(locationSelected)));
        }
        addFiveBlankMenuItems(menu, 1);
        savePreferences();
    }

    private void addFiveBlankMenuItems(Menu menu, int i) {
        menu.add(i, 10001, 10001, "");
        menu.add(i, 10002, 10002, "");
        menu.add(i, 10003, 10003, "");
        menu.add(i, 10004, 10004, "");
        menu.add(i, 10005, 10005, "");
    }

    private void removeListeners() {
        if (getMap() != null) {
            getMap().setOnMarkerClickListener(null);
            getMap().setOnMapLongClickListener(null);
            getMap().setOnInfoWindowClickListener(null);
            getMap().setOnMapClickListener(null);
        }
        customLocationButton.setOnClickListener(null);
        customLocationButton.setOnLongClickListener(null);
        navigationViewLeft.setNavigationItemSelectedListener(null);
        navigationViewRight.setNavigationItemSelectedListener(null);
        floatingSearchView.setOnLeftMenuClickListener(null);
        floatingSearchView.setOnQueryChangeListener(null);
        drawerLayout.removeDrawerListener(mDrawerToggle);
    }

    private void initListeners() {
        if (getMap() != null) {
            getMap().setOnMapClickListener(this);
            getMap().setOnMapLongClickListener(this);
            getMap().setOnInfoWindowClickListener(this);
            getMap().setOnMapClickListener(this);
        }

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.clear_map, R.string.app_name) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }
            }

        };

        customLocationButton.setOnClickListener(this);
        customLocationButton.setOnLongClickListener(this);
        drawerLayout.addDrawerListener(mDrawerToggle);

        floatingSearchView.setOnLeftMenuClickListener(
                new FloatingSearchView.OnLeftMenuClickListener() {


                    @Override
                    public void onMenuOpened() {
                        drawerLayout.openDrawer(GravityCompat.START);
                        if ((navigationViewLeft.getMenu().findItem(99) != null) && navigationViewLeft.getMenu().findItem(99).isChecked()) {
                            drawerLayout.openDrawer(GravityCompat.END);
                        }
                        if ((navigationViewLeft.getMenu().findItem(299) != null) && navigationViewLeft.getMenu().findItem(299).isChecked()) {
                            drawerLayout.openDrawer(GravityCompat.END);
                        }
                    }

                    @Override
                    public void onMenuClosed() {
                    }
                });


        navigationViewLeft.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                int menuItemNumber = menuItem.getItemId();
                switch (menuItemNumber) {
                    case R.id.action_clear_map:
                        getMap().clear();
                        break;

                    case R.id.action_mock:
                        setMockLocation(locationToBeMocked);
                        break;

                    case R.id.toggle_mock:
                        toggleMockEnabledState();
                        break;

                    case R.id.toggle_traffic:
                        toggleTrafficState();
                        break;
                    case R.id.satellite:
                        if (!menuItem.isChecked()) {
                            mapState = 1;
                            setMapType();
                        } else {
                            mapState = 0;
                            setMapType();
                            menuItem.setChecked(false);
                        }
                        break;
                    case R.id.hybrid:
                        if (!menuItem.isChecked()) {
                            mapState = 2;
                            setMapType();
                        } else {
                            mapState = 0;
                            setMapType();
                            menuItem.setChecked(false);
                        }
                        break;
                    case R.id.terrain:
                        if (!menuItem.isChecked()) {
                            mapState = 3;
                            setMapType();
                        } else {
                            mapState = 0;
                            setMapType();
                            menuItem.setChecked(false);
                        }
                        break;
                    case R.id.action_steady_random_mock:
                        if (isMockEnabled) {
                            if (!menuItem.isChecked()) {
                                startRandomMock();
                                navigationViewLeft.getMenu().findItem(R.id.action_steady_random_mock).setChecked(steadyMockState);
                            } else {
                                stopRandomMock();
                            }
                        } else {
                            showShortToast("Mock is not allowed.");
                        }
                        break;
                    case R.id.action_open_paths:
                        createPathMenu();
                        break;

                    case R.id.action_settings:
                        startActivity(intent);
                        break;

                    case 98://Back to main menu
                        recreateMainMenu();
                        break;
                    case 99://Edit path
                        createEditPathMenu();
                        if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                            drawerLayout.openDrawer(GravityCompat.END);
                            menuItem.setChecked(true);
                        } else {
                            drawerLayout.closeDrawer(GravityCompat.END);
                            menuItem.setChecked(false);
                        }
                        break;

                    case 298://Back to path menu
                        createPathMenu();
                        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                            createEditPathMenu();
                        }
                        locationSelected = -1;
                        break;
                    case 299://Edit location
                        createEditLocationMenu();
                        if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                            drawerLayout.openDrawer(GravityCompat.END);
                            menuItem.setChecked(true);
                        } else {
                            drawerLayout.closeDrawer(GravityCompat.END);
                            menuItem.setChecked(false);
                        }
                        break;

                }

                if (menuItemNumber > 99 && menuItemNumber <= 199) {
                    onPathSelected(menuItemNumber, menuItem);
                }

                if (menuItemNumber > 299 && menuItemNumber <= 399) {
                    onLocationSelected(menuItemNumber, menuItem);
                }
                return true;
            }
        });

        navigationViewRight.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                int menuItemNumber = menuItem.getItemId();
                switch (menuItemNumber) {
                    case 1000://Close path edit
                        drawerLayout.closeDrawer(GravityCompat.END);
                        navigationViewLeft.getMenu().findItem(99).setChecked(false);
                        break;
                    case 1010://New path
                        addNewPath();
                        break;
                    case 1020://Delete path
                        deletePath();
                        break;
                    case 1030://Move path up
                        movePathUp();
                        break;
                    case 1040://Move path down
                        movePathDown();
                        break;
                    case 1050://Rename path
                    case 1051:
                        renamePathDialog();
                        break;
                    case 1060://Path wander
                    case 1061:
                        pathWanderDialog();
                        break;
                    case 1070://Path speed
                    case 1071:
                        pathSpeedDialog();
                        break;
                    case 1080://Path update interval
                    case 1081:
                        pathUpdateIntervalDialog();
                        break;
                    case 1100://Close location edit
                        if (snackbar != null) {
                            snackbar.dismiss();
                        }
                        drawerLayout.closeDrawer(GravityCompat.END);
                        navigationViewLeft.getMenu().findItem(299).setChecked(false);
                        break;
                    case 1110://New location
                        addNewLocation();
                        createLocationMenu();
                        break;
                    case 1120://Delete location
                        deleteLocation();
                        break;
                    case 1130://Move location up
                        moveLocationUp();
                        break;
                    case 1140://Move location down
                        moveLocationDown();
                        break;
                    case 1150://Rename location
                    case 1151:
                        renameLocationDialog();
                        break;
                    case 1160://Wait time
                    case 1161:
                        locationWaitDialog();
                        break;
                }

                return true;

            }
        });


        floatingSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {
                getAutocomplete(newQuery);
            }
        });
    }

    private void initCamera(Location location) {
        CameraPosition position = CameraPosition.builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(16f)
                .bearing(0.0f)
                .tilt(0.0f)
                .build();

        getMap().animateCamera(CameraUpdateFactory.newCameraPosition(position), null);
    }

    private void initViews() {

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationViewLeft = (NavigationView) findViewById(R.id.navigation_view_left);
        navigationViewRight = (NavigationView) findViewById(R.id.navigation_view_right);
        customLocationButton = (ImageView) findViewById(R.id.locationButton);
        floatingSearchView = (FloatingSearchView) findViewById(R.id.floating_search_view);
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        drawerLayout.setSaveEnabled(false);
        floatingSearchView.attachNavigationDrawerToMenuButton(drawerLayout);
        floatingSearchView.setSaveEnabled(false);
        mapFragment.getMapAsync(this);
    }

    private void initLocationServices() {
        googleApiClient = createGoogleApiClient();
        locationRequest = createLocationRequest();
        googleApiClient.connect();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void initPreviousInstance() {
        setToolbarState();
        setMapType();
        setLocationButtonState();
        setMockEnabledState();
        setTrafficState();
    }

    public void checkLocationPermissions() {
        locationTask = "checkLocationPermissions";
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            locationPermissionsGiven = true;
        }
    }

    public void enableMyLocation() {
        locationTask = "enableMyLocation";
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            userMap.setMyLocationEnabled(true);
        }
    }

    public void disableMyLocation() {
        locationTask = "disableMyLocation";
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            userMap.setMyLocationEnabled(false);
        }
    }

    public void showShortToast(String toast) {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
    }

    public void showLongToast(String toast) {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show();
    }

    public void startLocationUpdates() {
        locationTask = "startLocationUpdates";
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    public void setMockLocation(Location location) {
        if (isMockEnabled) {
            locationTask = "setMockLocation";
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, PERMISSIONS_REQUEST_ACCESS_LOCATION);
            } else {
                try {
                    if (location == null) {
                        location = getRandomLocation();
                    }
                    LocationServices.FusedLocationApi.setMockLocation(googleApiClient, location);
                    quickMessage = "Mock location set.";
                } catch (SecurityException e) {
                    quickMessage = "Mock locations are not allowed. Check developer options.";
                }
                showShortToast(quickMessage);
            }
        } else {
            quickMessage = "Mock locations are not enabled.";
            showShortToast(quickMessage);
        }
    }

    public void handleNewLocation(Location location) {
        if (getMockCheck(location)) {
            if (currentMockUserLocation != null) {
                lastMockUserLocation = currentMockUserLocation;
            }
            currentMockUserLocation = location;
            showShortToast(getMockCheckMessage(location));
        } else {
            if (currentRealUserLocation != null) {
                lastRealUserLocation = currentRealUserLocation;
            }
            currentRealUserLocation = location;
            showShortToast(getMockCheckMessage(location));
        }
        if (currentUserLocation != null) {
            lastUserLocation = currentUserLocation;
        }
        currentUserLocation = location;

        savePreferences();

    }

    public void setToolbarState() {
        if (isToolbarVisible) {
            floatingSearchView.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
        } else {
            floatingSearchView.animate().translationY(-floatingSearchView.getBottom()).setInterpolator(new AccelerateInterpolator()).start();
        }
    }

    public void toggleToolbarState() {
        isToolbarVisible = !isToolbarVisible;
        setToolbarState();
    }

    public void setLocationButtonState() {
        if (isLocationButtonActive) {
            customLocationButton.setImageResource(R.drawable.ic_custom_location_button2);
        } else {
            customLocationButton.setImageResource(R.drawable.ic_custom_location_button1);
        }
    }

    public void toggleLocationButtonState() {
        isLocationButtonActive = !isLocationButtonActive;
        setLocationButtonState();
    }

    public void setMockEnabledState() {
        if (isMockEnabled) {
            startMockMode();
        } else {
            stopMockMode();
        }
        navigationViewLeft.getMenu().findItem(R.id.toggle_mock).setChecked(isMockEnabled);
    }

    public void toggleMockEnabledState() {
        isMockEnabled = !isMockEnabled;
        stopRandomMock();
        setMockEnabledState();
    }

    public void startMockMode() {
        locationTask = "startMockMode";
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            try {
                LocationServices.FusedLocationApi.setMockMode(googleApiClient, true);
                quickMessage = "Mock mode enabled.";
                isMockEnabled = true;
            } catch (SecurityException e) {
                quickMessage = "Mock locations are not allowed. Check developer options.";
                isMockEnabled = false;
            }
            showShortToast(quickMessage);
        }
    }

    public void stopMockMode() {
        locationTask = "stopMockMode";
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            try {
                LocationServices.FusedLocationApi.setMockMode(googleApiClient, false);
                quickMessage = "Mock mode stopped.";
            } catch (SecurityException e) {
                quickMessage = "Mock locations are not allowed.";
            }
            showShortToast(quickMessage);
        }
    }

    private void setPathLocation() {
        Location location = getRandomLocation();
        location.setLatitude(currentMarkerLocation.getLatitude());
        location.setLongitude(currentMarkerLocation.getLongitude());
        pathList.get(pathSelected).get(locationSelected).set(location);
        locationSelected = -1;
        plotPathMarkers();
        createLocationMenu();
        createEditLocationMenu();
    }

    /**
     * Actions---------------------------------------------------------------------
     **/

    //Getters---------------------------------------------------------------------
    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this);

        String address;
        try {
            address = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1).get(0).getAddressLine(0);
        } catch (IOException e) {
            address = "Je ne sais pas";
        } catch (IndexOutOfBoundsException e) {
            address = Double.toString(latLng.latitude) + ", " + Double.toString(latLng.longitude);
        }
        return address;
    }

    public GoogleMap getMap() {
        return userMap;
    }

    public Location getLastLocationFromFused() {
        locationTask = "getLastLocationFromFused";
        Location location = null;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }
        if (location == null) {
            location = GOOGLEPLEX;
        }
        showShortToast(getMockCheckMessage(location));
        return location;
    }

    public Location getLastLocationFromApp() {
        locationTask = "getLastLocationFromFused";
        Location location;
        location = currentUserLocation;
        if (location == null) {
            location = GOOGLEPLEX;
        }
        showShortToast(getMockCheckMessage(location));
        return location;
    }

    public String getStringFromLocation(final Location location) {
        return Location.convert(location.getLatitude(), Location.FORMAT_DEGREES) + ", " + Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
    }

    private boolean getMockCheck(Location location) {
        boolean isMock;
        if (android.os.Build.VERSION.SDK_INT >= 18) {
            isMock = location.isFromMockProvider();
        } else {
            isMock = !Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
        }
        return isMock;
    }

    public String getMockCheckMessage(Location location) {
        if (getMockCheck(location)) {
            return "Location mocked.";
        } else {
            return "Location real.";
        }
    }

    public Location getRandomLocation() {
        Random r = new Random();
        double latitude = 180 * r.nextDouble() - 90;
        double longitude = 360 * r.nextDouble() - 180;
        float bearing = 360 * r.nextFloat();
        Location location = new Location("Random");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTime(new Date().getTime());
        location.setAccuracy(1);
        location.setSpeed(1);
        location.setBearing(bearing);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        return location;
    }
    //Getters---------------------------------------------------------------------

    public Location getLocationFromLatLng(LatLng latLng) {
        Random r = new Random();
        float bearing = 360 * r.nextFloat();
        Location location = new Location("LatLng");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        location.setTime(new Date().getTime());
        location.setAccuracy(1);
        location.setSpeed(1);
        location.setBearing(bearing);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        return location;
    }

    public LatLng getLatLngFromLocation(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    //Constructors----------------------------------------------------------------
    private void createMarker(Location markerLocation) {
        LatLng latLng = new LatLng(markerLocation.getLatitude(), markerLocation.getLongitude());
        userMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker())
                .position(latLng)
                .title(getAddressFromLatLng(latLng)));
    }

    protected LocationRequest createLocationRequest() {
        return LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    //Constructors----------------------------------------------------------------

    protected GoogleApiClient createGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
    }
    //Callbacks-------------------------------------------------------------------

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    //Callbacks-------------------------------------------------------------------
    //Permission request callback
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    switch (locationTask) {
                        case "getLastLocationFromFused": {
                            getLastLocationFromFused();
                            break;
                        }
                        case "enableMyLocation": {
                            enableMyLocation();
                            break;
                        }
                        case "disableMyLocation": {
                            enableMyLocation();
                            break;
                        }
                        case "startLocationUpdates": {
                            startLocationUpdates();
                            break;
                        }
                        case "startMockMode": {
                            startMockMode();
                            break;
                        }
                        case "stopMockMode": {
                            stopMockMode();
                            break;
                        }
                        case "setMockLocation": {
                            setMockLocation(locationToBeMocked);
                            break;
                        }
                        case "checkLocationPermissions": {
                            checkLocationPermissions();
                            break;
                        }
                    }

                }
            }
        }
    }

    public void stopRandomMock() {
        steadyMockState = false;
        navigationViewLeft.getMenu().findItem(R.id.action_steady_random_mock).setChecked(steadyMockState);
        handler.removeCallbacks(runnable);
    }

    public void startRandomMock() {
        steadyMockState = true;
        handler.postDelayed(runnable, 5000);
    }

    public ArrayList<AutocompletePrediction> getAutocomplete(CharSequence constraint) {
        if (googleApiClient.isConnected()) {
            //Log.i(TAG, "Starting autocomplete query for: " + constraint);

            // Submit the query to the autocomplete API and retrieve a PendingResult that will
            // contain the results when the query completes.
            PendingResult<AutocompletePredictionBuffer> results = Places.GeoDataApi.getAutocompletePredictions(googleApiClient, constraint.toString(), latLngBounds, null);

            // This method should have been called off the main UI thread. Block and wait for at most 60s
            // for a result from the API.
            results.setResultCallback(mUpdatePlaceDetailsCallback);


        }
        //Log.e(TAG, "Google API client is not connected for autocomplete query.");
        return null;
    }

    public void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        ArrayList<ArrayList<String>> pathLatitudeAsString = new ArrayList<>();
        ArrayList<ArrayList<String>> pathLongitudeAsString = new ArrayList<>();

        editor.putInt("totalpaths", totalPaths);
        editor.putString("last_latitude", Double.toString(currentUserLocation.getLatitude()));
        editor.putString("last_longitude", Double.toString(currentUserLocation.getLongitude()));

        for (int i = 0; i < pathList.size(); i++) {
            int a = i + 1000;

            pathLatitudeAsString.add(new ArrayList<String>());
            pathLongitudeAsString.add(new ArrayList<String>());

            editor.putString("pathname" + Integer.toString(a), pathNameList.get(i));
            editor.putInt("pathspeed" + Integer.toString(a), pathSpeed.get(i));
            editor.putInt("pathwander" + Integer.toString(a), pathWander.get(i));
            editor.putInt("totallocationsinpathlist" + Integer.toString(a), totalLocationsInPathList.get(i));
            editor.putInt("pathupdateinterval" + Integer.toString(a), pathUpdateInterval.get(i));

            for (int j = 0; j < pathList.get(i).size(); j++) {
                int b = j + 1000;
                pathLatitudeAsString.get(i).add(Double.toString(pathList.get(i).get(j).getLatitude()));
                pathLongitudeAsString.get(i).add(Double.toString(pathList.get(i).get(j).getLongitude()));

                editor.putString("latitude" + Integer.toString(a) + "," + Integer.toString(b), pathLatitudeAsString.get(i).get(j));
                editor.putString("longitude" + Integer.toString(a) + "," + Integer.toString(b), pathLongitudeAsString.get(i).get(j));
                editor.putInt("locationwaittime" + Integer.toString(a) + "," + Integer.toString(b), locationWaitTime.get(i).get(j));
                editor.putString("locationname" + Integer.toString(a) + "," + Integer.toString(b), locationNameList.get(i).get(j));

            }

        }

        editor.apply();
    }


    private void retrievePreferences() {
        Double lastLatitudeFromPreferences = Double.parseDouble(preferences.getString("last_latitude", "37.422535"));
        Double lastLongitudeFromPreferences = Double.parseDouble(preferences.getString("last_longitude", "-122.084804"));

        totalPaths = preferences.getInt("totalpaths", 0);

        if (lastLatitudeFromPreferences >= -90 && lastLatitudeFromPreferences <= 90) {
            currentUserLocation.setLatitude(lastLatitudeFromPreferences);
        } else {
            currentUserLocation.setLatitude(GOOGLEPLEX.getLatitude());
        }
        if (lastLongitudeFromPreferences >= -180 && lastLongitudeFromPreferences <= 180) {
            currentUserLocation.setLongitude(lastLongitudeFromPreferences);
        } else {
            currentUserLocation.setLatitude(GOOGLEPLEX.getLongitude());
        }

        currentUserLocation.setAccuracy(1);
        pathSelected = -1;
        locationSelected = -1;

        ArrayList<ArrayList<String>> pathLatitudeAsString = new ArrayList<>();
        ArrayList<ArrayList<String>> pathLongitudeAsString = new ArrayList<>();

        for (int i = 0; i < totalPaths; i++) {
            int a = i + 1000;

            pathLatitudeAsString.add(new ArrayList<String>());
            pathLongitudeAsString.add(new ArrayList<String>());

            locationWaitTime.add(new ArrayList<Integer>());
            locationNameList.add(new ArrayList<String>());
            pathList.add(new ArrayList<Location>());

            pathNameList.add(preferences.getString("pathname" + Integer.toString(a), "Blank"));
            pathSpeed.add(preferences.getInt("pathspeed" + Integer.toString(a), 1));
            pathWander.add(preferences.getInt("pathwander" + Integer.toString(a), 0));
            totalLocationsInPathList.add(preferences.getInt("totallocationsinpathlist" + Integer.toString(a), 0));
            pathUpdateInterval.add(preferences.getInt("pathupdateinterval" + Integer.toString(a), 100));


            for (int j = 0; j < totalLocationsInPathList.get(i); j++) {
                int b = j + 1000;
                pathLatitudeAsString.get(i).add(preferences.getString("latitude" + Integer.toString(a) + "," + Integer.toString(b), "37.422535"));
                pathLongitudeAsString.get(i).add(preferences.getString("longitude" + Integer.toString(a) + "," + Integer.toString(b), "-122.084804"));
                locationWaitTime.get(i).add(preferences.getInt("locationwaittime" + Integer.toString(a) + "," + Integer.toString(b), 0));
                locationNameList.get(i).add(preferences.getString("locationname" + Integer.toString(a) + "," + Integer.toString(b), "blank"));

                Location location = getRandomLocation();

                location.setLatitude(Double.parseDouble(pathLatitudeAsString.get(i).get(j)));
                location.setLongitude(Double.parseDouble(pathLongitudeAsString.get(i).get(j)));
                pathList.get(i).add(location);

            }

        }

    }

    public static double getDistance(Location location1, Location location2) {

        final double R = 6378.137; // Radius of the earth
        double lat1 = location1.getLatitude();
        double lon1 = location1.getLongitude();

        double lat2 = location2.getLatitude();
        double lon2 = location2.getLongitude();

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        return distance;
    }

    public float getBearing(Location location1, Location location2) {
        double lat1 = location1.getLatitude();
        double lon1 = location1.getLongitude();

        double lat2 = location2.getLatitude();
        double lon2 = location2.getLongitude();

        float angle = (float) Math.toDegrees(Math.atan2(lat2 - lat1, lon2 - lon1));

        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    public double getTotalDistance(ArrayList<Location> path) {
        double distance = 0;
        Location location1 = path.get(0);
        for (int i = 1; i < path.size(); i++) {
            Location location2 = path.get(i);
            distance += getDistance(location1, location2);
            location1 = location2;
        }
        return distance;
    }

    public ArrayList<Long> getNumberOfMidpoints(ArrayList<Location> path) {
        double distance;
        double time;
        long midpoints;

        double m1;
        double m2;
        double deltaLat;
        double deltaLon;
        double b1;
        double b2;

        ArrayList<Long> numberOfMidpoints = new ArrayList<>();
        ArrayList<Location> finalPathList = new ArrayList<>();

        Location location1 = new Location("final");
        Location location2 = new Location("final");
        Location locationA = new Location("final");
        Location locationB = new Location("final");

        location1.set(path.get(0));
        addCircle(location1);
        location1 = addWander(location1);
        finalPathList.add(location1);
        locationA.set(location1);
        for (int i = 1; i < path.size(); i++) {



            location2.set(path.get(i));
            addCircle(location2);
            distance = getDistance(location1, location2);
            showShortToast(Double.toString(distance));
            distance = (double) location1.distanceTo(location2);
            showShortToast(Double.toString(distance));
            time = distance / pathSpeed.get(pathSelected);
            midpoints = Math.round(time / ((double) pathUpdateInterval.get(pathSelected) / 1000));
            numberOfMidpoints.add(midpoints);

            deltaLat = location2.getLatitude() - location1.getLatitude();
            deltaLon = location2.getLongitude() - location1.getLongitude();
            m1 = deltaLat / midpoints;
            m2 = deltaLon / midpoints;
            b1 = location1.getLatitude();
            b2 = location1.getLongitude();


            location1.set(location2);
            location2.set(addWander(location2));
            for (int j = 1; j < midpoints; j++) {
                locationB = new Location(Integer.toString(j));
                locationB.setLatitude(j * m1 + b1);
                locationB.setLongitude(j * m2 + b2);
                locationB.setAccuracy(pathWander.get(pathSelected));
                addCircle(locationB);
                locationB.set(addWander(locationB));
                locationB.setSpeed((float) getDistance(locationA, locationB) / (pathUpdateInterval.get(pathSelected) / 1000));
                locationB.setBearing(getBearing(locationA, locationB));
                finalPathList.add(locationB);
                addLine(locationA, locationB);

                locationA.set(locationB);
            }


            addLine(locationB, location2);
            finalPathList.add(location2);
            locationA.set(location2);

        }

        addCircle(location1);
        finalPathList.add(location1);

        return numberOfMidpoints;
    }

    public void addCircle(Location location) {
        getMap().addCircle(new CircleOptions()
                .center(getLatLngFromLocation(location))
                .radius(pathWander.get(pathSelected))
                .strokeColor(Color.BLUE)
                .fillColor(Color.TRANSPARENT));

    }

    public void addLine(Location location1, Location location2) {
        getMap().addPolyline(new PolylineOptions()
                .add(getLatLngFromLocation(location1), getLatLngFromLocation(location2))
                .width(5)
                .color(Color.RED));
    }

    public Location addWander(Location pathLocation) {
        Location location = new Location("wander");
        location.set(pathLocation);
        Random r = new Random();
        double latitude1 = Math.toRadians(location.getLatitude());
        double longitutde1 = Math.toRadians(location.getLongitude());
        int wander = pathWander.get(pathSelected);
        double wanderRadians = (double) wander / 6378137;
        double rAngle = 2 * Math.PI * r.nextDouble();
        double rDistance = wanderRadians * r.nextDouble();

        //Courtesy of http://williams.best.vwh.net/avform.htm#LL
        double latitude2 = Math.asin(Math.sin(latitude1) * Math.cos(rDistance) + Math.cos(latitude1) * Math.sin(rDistance) * Math.cos(rAngle));
        double dlongitude = Math.atan2(Math.sin(rAngle) * Math.sin(rDistance) * Math.cos(latitude1), Math.cos(rDistance) - Math.sin(latitude1) * Math.sin(latitude2));
        double longitude2 = longitutde1 - dlongitude + Math.PI - 2 * Math.PI * Math.floor((longitutde1 - dlongitude + Math.PI) / (2 * Math.PI)) - Math.PI;

        location.setLatitude(Math.toDegrees(latitude2));
        location.setLongitude(Math.toDegrees(longitude2));
        return location;
    }
}
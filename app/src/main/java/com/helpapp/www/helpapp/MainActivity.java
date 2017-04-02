package com.helpapp.www.helpapp;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

import com.google.android.gms.location.LocationSettingsStatusCodes;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        //GoogleApiClient.OnConnectionFailedListener, TODO Add listener
        LocationListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    Activity mActivity;


    boolean mRequestingLocationUpdates = false;
    LocationRequest mLocationRequest;

    // UI elements
    TextView mTrackingStatusText;
    TextView mLocationLatitudeText;
    TextView mLocationLongitudeText;
    TextView mLocationLastUpdateText;
    Button mToggleTrackingButton;

    //Logging tags
    enum LOGGING {
        TRACKING,
        PERMISSIONS,
        PLAY;
    }

    enum SAVED_INSTANCE_KEYS {
        REQUESTING_LOCATION_UPDATES,
        CURRENT_LOCATION
    }

    //TODO Convert to enum?
    final int REQUESTPERMISSION_ACCESS_FINE_LOCATION_RETURN_GRANTED =  25;

    // Communication through intents
    final int REQUESTCODE_CHECK_SETTINGS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ONCREATE", "OnCreate");

        mActivity = this;
        setContentView(R.layout.activity_main);

        initializeUI();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                    .addConnectionCallbacks(this)
                    //.addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }

        updateValuesFromBundle(savedInstanceState);

        // Initialize screen status
        updateUI();
    }

    // Map view components to members, add events...
    private void initializeUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTrackingStatusText = (TextView) findViewById(R.id.trackingStatusText);
        mLocationLatitudeText = (TextView) findViewById(R.id.locationLatitudeText);
        mLocationLongitudeText = (TextView) findViewById(R.id.locationLongitudeText);
        mLocationLastUpdateText = (TextView) findViewById(R.id.locationLastUpdateText);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        /*
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLocation(view);
            }
        });
        */

        mToggleTrackingButton = (Button) findViewById(R.id.toggleTrackingButton);
        mToggleTrackingButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTracking();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        prepareBundle(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    // Prepare bundle to be saved between states
    private void prepareBundle(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(SAVED_INSTANCE_KEYS.REQUESTING_LOCATION_UPDATES.toString(), mRequestingLocationUpdates);
        if (mCurrentLocation!=null) {
            savedInstanceState.putParcelable(SAVED_INSTANCE_KEYS.CURRENT_LOCATION.toString(), mCurrentLocation);
        }
    }

    // Recover values for bundle (if present), and set values & UI states as needed
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(SAVED_INSTANCE_KEYS.REQUESTING_LOCATION_UPDATES.toString())) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        SAVED_INSTANCE_KEYS.REQUESTING_LOCATION_UPDATES.toString());
                //setButtonsEnabledState();
            }

            if (savedInstanceState.keySet().contains(SAVED_INSTANCE_KEYS.CURRENT_LOCATION.toString())) {
                mCurrentLocation = savedInstanceState.getParcelable(SAVED_INSTANCE_KEYS.CURRENT_LOCATION.toString());
            }

            //updateUI();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(LOGGING.PLAY.toString(), "CONNECTION - Connected");
        getLastLocation();
    }

    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(mActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOGGING.PERMISSIONS.toString(), "ACCESS_FINE_LOCATION - Granted");
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mCurrentLocation != null) {
                Log.d("LAST LOCATION", formatLocation(mCurrentLocation));
            } else {
                Log.d(LOGGING.PLAY.toString(), "LastLocation is null");
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOGGING.PLAY.toString(), "CONNECTION - Suspended");
        // TODO add code here
        Log.d("MISSING CODE", "onConnectionSuspended: Implementation pending");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void toggleTracking() {
        if (mRequestingLocationUpdates) {
            deactivateTracking();
        } else {
            activateTracking();
        }
    }

    void deactivateTracking() {
        // User wants to activate tracking
        Log.d(LOGGING.TRACKING.toString(), "User wants to deactivate tracking");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        mRequestingLocationUpdates = false;
        updateUI();
    }

    void activateTracking() {
        // User wants to activate tracking
        Log.d(LOGGING.TRACKING.toString(), "User wants to activate tracking");

        if (ContextCompat.checkSelfPermission(mActivity,android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOGGING.PERMISSIONS.toString(), "ACCESS_FINE_LOCATION - No permission has been granted yet");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(LOGGING.PERMISSIONS.toString(), "ACCESS_FINE_LOCATION - Should show rationale");
                Toast.makeText(mActivity, "ACCESS_FINE_LOCATION: Should show rationale", Toast.LENGTH_SHORT).show();
                // TODO: Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                // TODO add code here
                Log.d("MISSING CODE", "activateTracking - show rationale: Implementation pending");
            } else {
                // No explanation needed, we can request the permission.
                Log.d(LOGGING.PERMISSIONS.toString(), "ACCESS_FINE_LOCATION - No explanation needed");
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUESTPERMISSION_ACCESS_FINE_LOCATION_RETURN_GRANTED);
            }
        } else {
            // tracking is not active yet, and we have the needed permission
            Log.d(LOGGING.PERMISSIONS.toString(), "ACCESS_FINE_LOCATION - We have permission");

            mLocationRequest = new LocationRequest()
                    .setInterval(10000)
                    .setFastestInterval(5000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(mLocationRequest);

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                            builder.build());

            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    //final LocationSettingsStates = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can initialize location requests here.
                            initLocationTracking();
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied, but this can be fixed
                            // by showing the user a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(
                                        mActivity, //OuterClass.this,
                                        REQUESTCODE_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way
                            // to fix the settings so we won't show the dialog.
                            // TODO add code here
                            Log.d("MISSING CODE", "activateTracking - SETTINGS_CHANGE_UNAVAILABLE: Implementation pending");
                            break;
                    }
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUESTCODE_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        initLocationTracking();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        if (mActivity != null) {
                            ((MainActivity)mActivity).onLocationSettingsFailed();
                        }
                        break;
                    default:
                        if (mActivity != null) {
                            ((MainActivity)mActivity).onLocationSettingsFailed();
                        }
                        break;
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUESTPERMISSION_ACCESS_FINE_LOCATION_RETURN_GRANTED: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d(LOGGING.PERMISSIONS.toString(), "ACCESS_FINE_LOCATION - Granted");
                    activateTracking();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    // TODO How to disable completely???
                    Log.d(LOGGING.PERMISSIONS.toString(), "ACCESS_FINE_LOCATION - Denied");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    // Called when user was asked to change settings... but decided not to do it, it was not possible
    public void onLocationSettingsFailed() {
        // Called when user
        // TODO add code here
        Log.d("MISSING CODE", "onLocationSettingsFailed: Implementation pending");
    }

    void initLocationTracking() {
        Log.d("MISSING CODE", "initLocationTracking: Implementation pending");
        if (ContextCompat.checkSelfPermission(mActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        mRequestingLocationUpdates = true;
        showTrackingStatus();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("LOCATION UPDATE", formatLocation(location));
        mCurrentLocation = location;
        showLocation();
    }

    private void updateUI() {
        showTrackingStatus();
        showLocation();
    }

    public void showTrackingStatus() {
        if (mRequestingLocationUpdates) {
            mToggleTrackingButton.setText(R.string.deactivate_tracking);
            mTrackingStatusText.setText(R.string.trackme_on_description);

        } else {
            mToggleTrackingButton.setText(R.string.activate_tracking);
            mTrackingStatusText.setText(R.string.trackme_off_description);
        }
    }

    private void showLocation() {
        if (mCurrentLocation!=null) {
            mLocationLongitudeText.setText(String.valueOf(mCurrentLocation.getLongitude()));
            mLocationLatitudeText.setText(String.valueOf(mCurrentLocation.getLatitude()));
            mLocationLastUpdateText.setText(String.valueOf(mCurrentLocation.getTime()));
        } else {
            mLocationLongitudeText.setText("Longitude");
            mLocationLatitudeText.setText("Latitude");
            mLocationLastUpdateText.setText("Last update");
        }
    }

    private String formatLocation(Location location) {
        return "Latitude:" + String.valueOf(mCurrentLocation.getLatitude()) + ", Longitude:" + String.valueOf(mCurrentLocation.getLongitude());
    }
}

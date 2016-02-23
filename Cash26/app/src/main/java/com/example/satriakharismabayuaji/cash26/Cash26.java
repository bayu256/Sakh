package com.example.satriakharismabayuaji.cash26;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.JsonWriter;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.security.Permission;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Cash26 extends FragmentActivity implements OnMapReadyCallback
        , ActivityCompat.OnRequestPermissionsResultCallback, OnMyLocationButtonClickListener
        , ConnectionCallbacks, OnConnectionFailedListener, OnMarkerClickListener, LocationListener {

    private GoogleMap mMap;

    Polyline line;

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 100;
    private static final int MY_PERMISSIONS_ACCESS_NETWORK_STATE = 100;



    private GoogleApiClient mGoogleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash26);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // first check the Google Api Client
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    public void onConnected(Bundle connectionHint) {
        if(checkAndRequestLocationPermission())
            createLocationRequest();

    }

    public void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30000);
        mLocationRequest.setFastestInterval(20000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(
                mGoogleApiClient,builder.build());

    }

    public boolean checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return false;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(0, 150, 0, 0);
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        googleMap.setOnMyLocationButtonClickListener(this);
        enableUISettings(googleMap);
        goToCurrentLocation(googleMap);
        enableMyLocation();
        try {
            setMarkers(mMap, getCashPoints());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        googleMap.setOnMarkerClickListener(this);

    }

    public void enableMyLocation(){
        if(checkAndRequestLocationPermission())
            mMap.setMyLocationEnabled(true);
    }


    public void goToCurrentLocation(GoogleMap googleMap){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        checkAndRequestLocationPermission();

        Location location = locationManager.getLastKnownLocation(locationManager
                .getBestProvider(criteria, false));
        if(location != null)
        {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), 17));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                    .zoom(17)
                    .bearing(90)
                    .tilt(30)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        }

    }

    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(checkAndRequestLocationPermission())
                        mMap.setMyLocationEnabled(true);
                } else {
                    mMap.setMyLocationEnabled(false);
                }
                return;
            }
        }
    }

    /**
     * Method to enable the UISettings (zoom , compass, and MyLocation Layer)
     */
    public void enableUISettings(GoogleMap googleMap) {
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        checkAndRequestLocationPermission();
        uiSettings.setMyLocationButtonEnabled(true);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     *
     * @return JSONArray with the points of the stores
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws JSONException
     *
     * The link given in the PDF did not work, so i went to a friend that has a Number 26 Account
     * and reverse engineered the mobile App (using Charles to catch the Data transfer and look for the
     * api used in the Number 26 App)
     */
    public JSONArray getCashPoints() throws IOException, ExecutionException, InterruptedException, JSONException {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService((Context.CONNECTIVITY_SERVICE));
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String urlString = "http://api.tech26.de/api/barzahlen/branches?nelat=52.58667919201155&nelon=13.4155825694994&swlat=52.41352818085949&swlon=13.23225675044126";
            AsyncTask asyncTask = new myAsyncTask().execute(urlString);
            String response = asyncTask.get().toString();
            JSONArray jObject = new JSONArray(response);
            return jObject;
        } else {
            return null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Location mCurrentLocation = location;
        LatLng locPos = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        Marker mMarker = mMap.addMarker(new MarkerOptions().position(locPos));
        if(mMap != null){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locPos, 17.0f));
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class myAsyncTask extends AsyncTask<String,Void,String> {

            @Override
            protected String doInBackground(String... params) {

                // params comes from the execute() call: params[0] is the url.
                try {
                    return JSONManipulator.downloadUrl(params[0]);
                } catch (IOException e) {
                    return "Unable to retrieve web page. URL may be invalid.";
                }
            }
    }

    private void setMarkers(GoogleMap googleMap, JSONArray jsonArray) throws JSONException {
        int len = jsonArray.length();
        int i = 0;
        while (i <= len){
            JSONObject jObject = jsonArray.getJSONObject(i);
            String storeName = jObject.getString("title");
            String storePhone = jObject.getString("phone");
            String snippetString = "Tel : ";

            if(storePhone.equals("")){
                snippetString = snippetString.concat("-").concat("\n");
            }else {
                snippetString = snippetString.concat(storePhone);
            }
            LatLng storeCoord = new LatLng(jObject.getDouble("lat"),jObject.getDouble("lng"));
            Marker store = googleMap.addMarker(new MarkerOptions()
                    .position(storeCoord)
                    .title(storeName)
                    .snippet(snippetString)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
            i++;
        }
    }

    @Override
    public boolean onMarkerClick (Marker marker){

            while (line != null) {
                line.setVisible(false);
            }
            String url = makeUrl(marker.getPosition(), mMap.getMyLocation());
            AsyncTask asyncTask = new myAsyncTask().execute(url);
            String response = null;
            JSONObject path = null;
            try {
                response = asyncTask.get().toString();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            try {
                path = new JSONObject(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                Path.drawPath(marker, line, mMap.getMyLocation(), mMap, path);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            marker.showInfoWindow();
            return true;

    }

    public String makeUrl(LatLng destLatlng, Location currentLoc){
        double posLat = currentLoc.getLatitude();
        double posLng = currentLoc.getLongitude();
        double desLat = destLatlng.latitude;
        double desLng = destLatlng.longitude;

        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");
        urlString.append(Double.toString(posLat));
        urlString.append(",");
        urlString.append(Double.toString(posLng));
        urlString.append("&destination=");
        urlString.append(Double.toString(desLat));
        urlString.append(",");
        urlString.append(Double.toString(desLng));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        return urlString.toString();

    }





}


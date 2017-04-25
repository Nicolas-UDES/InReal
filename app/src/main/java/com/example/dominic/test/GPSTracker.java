package com.example.dominic.test;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ServiceCompat;
import android.util.Log;

public class GPSTracker extends Service implements LocationListener {

    private Location location;

    // flag for provider
    private String provider;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1; // 1 second

    // Declaring a Location Manager
    protected LocationManager locationManager;

    public GPSTracker(Context context) {
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
    }

    public void start() {
        updateProvider();
    }

    // Never throws SecurityException. It's just so Android Studio shushes.
    private void updateProvider() throws SecurityException {
        locationManager.removeUpdates(this);
        String gps = LocationManager.GPS_PROVIDER;
        String network = LocationManager.NETWORK_PROVIDER;

        // getting the provider
        provider = locationManager.isProviderEnabled(network) ? network :
                locationManager.isProviderEnabled(gps) ? gps : null;

        if (provider != null) {
            location = locationManager.getLastKnownLocation(provider);
            locationManager.requestLocationUpdates(
                    provider,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        }
    }

    public double getLongitude() {
        return location != null ? location.getLongitude() : Double.NaN;
    }

    public double getLatitude() {
        return location != null ? location.getLatitude() : Double.NaN;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        updateProvider();
    }

    @Override
    public void onProviderEnabled(String provider) {
        updateProvider();
    }

    @Override
    public void onProviderDisabled(String provider) {
        updateProvider();
    }
}
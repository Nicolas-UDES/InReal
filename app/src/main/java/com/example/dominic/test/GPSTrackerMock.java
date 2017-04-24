package com.example.dominic.test;

import android.content.Context;
import android.location.Location;

public class GPSTrackerMock extends GPSTracker {

    private double latitude;
    private double longitude;
    private Location location;

    public GPSTrackerMock(Context context) {
        latitude = 45.3791246;
        longitude = -71.9286939;

        location = new Location("Test");
        location.setLatitude((latitude));
        location.setLatitude((longitude));
    }

    /**
     * Function to get latitude
     * */
    public double getLatitude(){
        return latitude;
    }

    /**
     * Function to get longitude
     * */
    public double getLongitude(){
        return longitude;
    }

    public Location getLocation() throws SecurityException {
        return location;
    }
}

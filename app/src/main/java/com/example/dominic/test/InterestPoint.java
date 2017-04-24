package com.example.dominic.test;

import android.location.Location;
import android.widget.TextView;

public class InterestPoint {
    private Location location;
    private String name;
    private TextView textView;

    public InterestPoint(double latitude, double longitude, String name) {
        this.location = new Location("InterestPoint");
        this.location.setLatitude(latitude);
        this.location.setLongitude(longitude);
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TextView getTextView() { return textView; }

    public void setTextView(TextView textView) { this.textView = textView; }
}

package com.example.dominic.test;

import java.util.ArrayList;
import java.util.List;

public class PlaceServiceMock extends PlaceService {

    private List<InterestPoint> pointList;

    public PlaceServiceMock(int visible, int invisible) {
        pointList = new ArrayList<>();
        for (int i = 0; i < visible; ++i) {
            pointList.add(generateVisible());
        }
        for (int i = 0; i < invisible; ++i) {
            pointList.add(generateInvisible());
        }
    }

    private InterestPoint generateVisible() {
        return new InterestPoint(45.3797246,-71.9299939, "Visible point");
    }
    private InterestPoint generateInvisible() {
        return new InterestPoint(45.3741246,-71.9286939, "Invisible point");
    }

    public void getPlaces(double latitude, double longitude) {
    }

    public List<InterestPoint> getPointList() {
        return pointList;
    }
}

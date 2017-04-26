package com.example.dominic.test;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaceService extends Service implements Response.Listener<JSONObject>, Response.ErrorListener {
    private int RADIUS = 5000;
    private String KEY = "AIzaSyCIYOddQcDcoM5JZKELt4ayvAlIr5QknNs";
    private String BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%1$f,%2$f&radius=%3$d&key=%4$s";
    private int POOL_WIDTH = 100;

    private Context mContext;
    private List<InterestPoint> pointListPool;
    private List<InterestPoint> pointList;

    public PlaceService() {}

    public PlaceService(Context context) {
        mContext = context;

        pointListPool = createInterestPointPool(POOL_WIDTH);
        pointList = new ArrayList<>(POOL_WIDTH);
    }

    private String getRequest(double latitude, double longitude) {
        return String.format(Locale.CANADA, BASE_URL, latitude, longitude, RADIUS, KEY);
    }

    public void getPlaces(double latitude, double longitude) {
        String request = getRequest(latitude, longitude);
        System.out.println(request);

        RequestQueue queue = Volley.newRequestQueue(mContext);
        JsonObjectRequest jsonObjRequest = new JsonObjectRequest(Request.Method.GET, request, null, this, this);
        queue.add(jsonObjRequest);
    }

    @Override
    public void onResponse(JSONObject response) {
        JSONArray res = response.optJSONArray("results");
        if(res == null) {
            System.out.println("No results in Google's answer: " + response.toString());
            return;
        }
        for (int i = 0; i < Math.min(res.length(), POOL_WIDTH); ++i) {
            JSONObject interestPoint = res.optJSONObject(i);
            JSONObject geometry = interestPoint == null ? null : interestPoint.optJSONObject("geometry");
            JSONObject location = geometry == null ? null : geometry.optJSONObject("location");
            if(location == null) {
                System.out.println("Incorrect Google answer: " + response.toString());
                return;
            }

            double latitude = location.optDouble("lat", Double.NaN);
            double longitude = location.optDouble("lng", Double.NaN);
            String name = interestPoint.optString("name", null);

            if(!Double.isNaN(latitude) && !Double.isNaN(longitude) && name != null) {
                pointList.add(pointListPool.remove(pointListPool.size() - 1).set(latitude, longitude, name));
            }
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        error.printStackTrace();
    }

    private List<InterestPoint> createInterestPointPool(int n) {
        final List<InterestPoint> interestPointList = new ArrayList<>(n);

        for(int i = 0; i < n; ++i) {
            interestPointList.add(new InterestPoint(0, 0, null));
        }

        return interestPointList;
    }

    public List<InterestPoint> getPointList() {
        return pointList;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
package com.example.dominic.test;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaceService extends Service {
    private String BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";

    private Context mContext;
    private List<InterestPoint> pointList;

    public PlaceService() {}

    public PlaceService(Context context) {
        mContext = context;

        pointList = new ArrayList<>();
    }

    public void getPlaces(double latitude, double longitude) {
        StringBuilder builder = new StringBuilder();

        builder.append(BASE_URL);
        builder.append("location=");
        builder.append(latitude);
        builder.append(",");
        builder.append(longitude);
        builder.append("&radius=5000&key=AIzaSyCIYOddQcDcoM5JZKELt4ayvAlIr5QknNs");

        System.out.println(builder.toString());

        RequestQueue queue = Volley.newRequestQueue(mContext);

        JsonObjectRequest jsonObjRequest = new JsonObjectRequest(Request.Method.GET, builder.toString(), null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray res = response.getJSONArray("results");
                            for (int i = 0; i < res.length(); ++i) {
                                JSONObject interestPoint = res.getJSONObject(i);
                                JSONObject geometry = interestPoint.getJSONObject("geometry");
                                JSONObject location = geometry.getJSONObject("location");
                                double latitude = location.getDouble("lat");
                                double longitude = location.getDouble("lng");
                                String name = interestPoint.getString("name");

                                //On pourrait utiliser un pool de InterestPoint
                                pointList.add(new InterestPoint(latitude, longitude, name));

                            }
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("OUT ERREUR");
                    }
                });
        queue.add(jsonObjRequest);
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
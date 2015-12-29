package com.votors.runningx;
/**
 * Created by Jason on 2015/11/26 0026.
 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;


import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapFragment;
import com.amap.api.maps2d.SupportMapFragment;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MapActivity_gd extends FragmentActivity {

    public final static String EXTRA_MESSAGE = "com.votors.runningx.MESSAGE";
    private static final String BC_INTENT = "com.votors.runningx.BroadcastReceiver.location";
    public final static String EXTRA_GpsRec = "com.votors.runningx.GpsRec";

    public final static int MARK_DISTANCE = 100;
//    public static int ZOOM_LEVEL = 15;
    // The Map Object
    private AMap mMap;

    public static final String TAG = "MapActivity";

    private final LocationReceiver mReceiver = new LocationReceiver();
    private final IntentFilter intentFilter = new IntentFilter(BC_INTENT);
    ArrayList<GpsRec> locations = null;
    double total_dist = 0;
    double center_lat = 0;
    double center_lng = 0;

    int movePointCnt = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_map_gd);
        locations = (ArrayList<GpsRec>)getIntent().getSerializableExtra(EXTRA_MESSAGE);

//        intentFilter.setPriority(3);
        registerReceiver(mReceiver, intentFilter);

        Log.i(TAG, "location numbler: " + locations.size());

        // Get Map Object
        mMap = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        final PolylineOptions polylines = new PolylineOptions();
        polylines.color(Color.BLUE).width(10);
        /*line = map.addPolyline(new PolylineOptions()
                .add(new LatLng(51.5, -0.1), new LatLng(40.7, -74.0))
                .width(5)
                .color(Color.RED)*/

        if (null != mMap && locations != null) {
            // Add a marker for every earthquake
            int cnt = 0;
            // If already run a long way, distance between mark should be larger.
            int mark_distance = locations.size()>100 ? MARK_DISTANCE*10: MARK_DISTANCE;
            for (GpsRec rec: locations) {
                Log.i(TAG, rec.toString());
                cnt++;
                if (cnt==1 || cnt == locations.size() || (int)Math.floor(total_dist/ mark_distance) != (int)Math.floor((total_dist+rec.distance)/ mark_distance)) {
                    // Add a new marker
                    MarkerOptions mk = new MarkerOptions()
                            .position(new LatLng(rec.getLat(), rec.getLng()));

                    // Set the title of the Marker's information window
                    if (cnt==1) {
                        mk.title(String.valueOf("start"));
                    } else {
                        mk.title(String.format("%.2f%s,%.1f%s",
                                Conf.getDistance(getApplicationContext(), (float) (total_dist + rec.distance)),
                                Conf.getDistanceUnit(getApplicationContext()),
                                Conf.getSpeed(getApplicationContext(), rec.speed),
                                Conf.getSpeedUnit(getApplicationContext())));
                    }

                    // Set the color for the Marker
                    mk.icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(rec.speed)));
                    mMap.addMarker(mk);
                }
                total_dist += rec.distance;
                center_lat += rec.getLat();
                center_lng += rec.getLng();

                polylines.add(new LatLng(rec.getLat(),rec.getLng()));
            }
        }

        // Center the map, draw the path
        // Should compute map center from the actual data
        mMap.addPolyline(polylines);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(center_lat / locations.size(), center_lng / locations.size())));
        float zoom = mMap.getMaxZoomLevel()-2;
        if (total_dist > 500) zoom--;
        if (total_dist > 5000) zoom--;
        if (total_dist > 50000) zoom--;
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoom));
    }
    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    // hue: [0,360)
    private float getMarkerColor(float speed) {
        float hue = 0f;
        if (speed < 1) {
            hue = 1;
        } else if (speed > 9) {
            hue = 9;
        }

        return (36 * hue);
    }

    public class LocationReceiver extends BroadcastReceiver {
        private final String TAG = "LocationReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            GpsRec rec = (GpsRec)intent.getSerializableExtra(EXTRA_GpsRec);
            Log.i(TAG, "LocationReceiver, location " + rec.toString());

            final PolylineOptions polylines = new PolylineOptions();
            GpsRec last;
            polylines.color(Color.BLUE).width(10);
            if (locations.size()>0) {
                last = locations.get(locations.size() - 1);
                polylines.add(new LatLng(last.getLat(),last.getLng()));
            }

            // If already run a long way, distance between mark should be larger.
            int mark_distance = locations.size()>200 ? MARK_DISTANCE*10: MARK_DISTANCE;
            if (movePointCnt == 0 || (int)Math.floor(total_dist/ mark_distance) !=  (int)Math.floor((total_dist+rec.distance)/ mark_distance)) {
                // Add a new marker
                MarkerOptions mk = new MarkerOptions()
                        .position(new LatLng(rec.getLat(), rec.getLng()));

                // Set the title of the Marker's information window
                //mk.title(String.format("%.0fm,%.1fm/s",Math.floor(total_dist + rec.distance),rec.speed));
                mk.title(String.format("%.2f%s,%.1f%s",
                        Conf.getDistance(getApplicationContext(),(float)(total_dist + rec.distance)),
                        Conf.getDistanceUnit(getApplicationContext()),
                        Conf.getSpeed(getApplicationContext(),rec.speed),
                        Conf.getSpeedUnit(getApplicationContext())));

                // Set the color for the Marker
                mk.icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(rec.speed)));
                mMap.addMarker(mk);
            }
            movePointCnt++;
            total_dist += rec.distance;
            center_lat += rec.getLat();
            center_lng += rec.getLng();
            locations.add(rec);

            polylines.add(new LatLng(rec.getLat(), rec.getLng()));
            mMap.addPolyline(polylines);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(rec.getLat(), rec.getLng())));

            if (movePointCnt == 1){
                Log.i(TAG, String.format("Zoom max %f, min %f", mMap.getMaxZoomLevel(), mMap.getMinZoomLevel()));
                mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getMaxZoomLevel()-2));
            }
        }

    }
}

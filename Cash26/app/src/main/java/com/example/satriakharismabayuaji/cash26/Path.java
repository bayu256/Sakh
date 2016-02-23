package com.example.satriakharismabayuaji.cash26;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;



public class Path {

    public static void drawPath(Marker marker, Polyline line, Location location, GoogleMap mMap,
                                JSONObject json) throws JSONException {

        try {
            LatLng markerPosition = marker.getPosition();
            LatLng myPosition = new LatLng(location.getLatitude(), location.getLongitude());

            JSONArray arrayRoutes = json.getJSONArray("routes");
            JSONObject routes = arrayRoutes.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> path = decodePoly(encodedString);
            for (int z = 0; z < path.size() - 1; z++) {
                LatLng src = path.get(z);
                LatLng dest = path.get(z + 1);
                line = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.latitude, src.longitude),
                                new LatLng(dest.latitude, dest.longitude))
                        .width(5).color(Color.BLUE).geodesic(true));
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static List<LatLng> decodePoly(String string){
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = string.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = string.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = string.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

}

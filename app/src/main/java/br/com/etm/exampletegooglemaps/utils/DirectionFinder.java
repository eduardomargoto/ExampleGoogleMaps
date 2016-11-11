package br.com.etm.exampletegooglemaps.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by EDUARDO_MARGOTO on 11/10/2016.
 */

public class DirectionFinder {

    private final static String URL_DIRECTION_JSON = "https://maps.googleapis.com/maps/api/directions/json";
    final static String URL_DIRECTION_XML = "https://maps.googleapis.com/maps/api/directions/xml";
    private DirectionFinderListener listener;
    private List<LatLng> waypoints;

    private String origin;
    private String destination;
    private String key = null;

    private boolean alternatives = false;

    public DirectionFinder() {
    }

    public DirectionFinder(DirectionFinderListener listener, String origin, String destination) {
        this.listener = listener;
        this.origin = origin;
        this.destination = destination;
        waypoints = new ArrayList<>();
    }

    public DirectionFinder(String origin, String destination) {
        this.origin = origin;
        this.destination = destination;
        waypoints = new ArrayList<>();
    }

    public DirectionFinder(String origin, List<LatLng> waypoints, String destination) {
        this.origin = origin;
        this.destination = destination;
        this.waypoints = waypoints;
    }

    public DirectionFinder setListener(DirectionFinderListener listener) {
        this.listener = listener;
        return this;
    }

    public void setWaypoints(List<LatLng> waypoints) {
        this.waypoints = waypoints;
    }

    public DirectionFinder withKey(String key) {
        this.key = key;
        return this;
    }

//    public DirectionFinder withMap(GoogleMap map) {
//        this.map = map;
//        return this;
//    }

    public DirectionFinder setAlternatives(boolean alternatives) {
        this.alternatives = alternatives;
        return this;
    }

    public DirectionFinder setRoute(String origin, String destination) {
        this.origin = origin;
        this.destination = destination;
        return this;
    }

    public void execute() throws Exception {
        listener.onDirectionFinderStart();


        String json = new DownloadRoutes().execute(getUrl()).get();
        parseJson(json);
    }

    private String getUrl() throws Exception {
        if (key == null)
            throw new Exception("Key API invalided");
        String url;
        if (waypoints.isEmpty())
            url = URL_DIRECTION_JSON + "?origin=" + this.origin + "&destination=" + this.destination + "&alternatives=" + alternatives + "&language=pt-BR&key=" + this.key;
        else {
            url = URL_DIRECTION_JSON + "?origin=" + this.origin + "&destination=" + this.destination + "&waypoints=";
            Iterator<LatLng> iterator = waypoints.iterator();

            while (iterator.hasNext()) {
                LatLng waypoint = iterator.next();
                url += waypoint.latitude + "," + waypoint.longitude;

                if (iterator.hasNext())
                    url += "|";
            }
            url += "&alternatives=" + alternatives + "&language=pt-BR&key=" + this.key;
        }


        Log.i("URL", url);
        return url;
    }

    private void parseJson(String resourceJson) throws JSONException {
        if (resourceJson == null)
            return;
        List<Route> routes = new ArrayList<>();
        JSONObject jsonData = new JSONObject(resourceJson);
        JSONArray jsonRoutes = jsonData.getJSONArray("routes");
        for (int i = 0; i < jsonRoutes.length(); i++) {
            JSONObject jsonObject = jsonRoutes.getJSONObject(i);
//            Route route = new Route();

//            JSONObject overview_polylineJson = jsonObject.getJSONObject("overview_polyline");
            JSONArray jsonLegs = jsonObject.getJSONArray("legs");
            for (int k = 0; k < jsonLegs.length(); k++) {
                Route route = new Route();
                JSONObject jsonLeg = jsonLegs.getJSONObject(k);

                JSONObject distance = jsonLeg.getJSONObject("distance");
                JSONObject duration = jsonLeg.getJSONObject("duration");
                JSONObject endLocation = jsonLeg.getJSONObject("end_location");
                JSONObject startLocation = jsonLeg.getJSONObject("start_location");

                route.distance = new Distance(distance.getInt("value"), distance.getString("text"));
                route.duration = new Duration(duration.getInt("value"), duration.getString("text"));
                route.startAddress = jsonLeg.getString("start_address");
                route.endAddress = jsonLeg.getString("end_address");
                route.startLocation = new LatLng(startLocation.getDouble("lat"), startLocation.getDouble("lng"));
                route.endLocation = new LatLng(endLocation.getDouble("lat"), endLocation.getDouble("lng"));

//                route.points = decodePolyline(overview_polylineJson.getString("points"));

                JSONArray jsonSteps = jsonLeg.getJSONArray("steps");
                route.steps = new ArrayList<>();
                for (int j = 0; j < jsonSteps.length(); j++) {
                    JSONObject jsonStep = jsonSteps.getJSONObject(j);
                    Step step = new Step();

//                JSONObject distanceStep = jsonStep.getJSONObject("distance");
//                JSONObject durationStep = jsonStep.getJSONObject("duration");
                    JSONObject endLocationStep = jsonStep.getJSONObject("end_location");
                    JSONObject startLocationStep = jsonStep.getJSONObject("start_location");
                    JSONObject stepPoints = jsonStep.getJSONObject("polyline");

                    step.startLocation = new LatLng(startLocationStep.getDouble("lat"), startLocationStep.getDouble("lng"));
                    step.endLocation = new LatLng(endLocationStep.getDouble("lat"), endLocationStep.getDouble("lng"));

                    step.points = decodePolyline(stepPoints.getString("points"));
//                step.distance = new Distance(distance.getInt("value"), distance.getString("text"));
//                step.duration = new Duration(duration.getInt("value"), duration.getString("text"));

                    route.steps.add(step);
                }
                routes.add(route);
            }

        }
        listener.onDirectionFinderSuccess(routes);
    }


    private List<LatLng> decodePolyline(final String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng(
                    lat / 1e5, lng / 1e5
            ));
        }

        return decoded;
    }

    public interface DirectionFinderListener {

        void onDirectionFinderStart();

        void onDirectionFinderSuccess(List<Route> routes);
    }
    private class DownloadRoutes extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String link = params[0];
            HttpURLConnection urlConnection = null;
            StringBuilder builderJson = new StringBuilder();
            try {
                URL url = new URL(link);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                InputStream in = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in));


                String line;

                while ((line = reader.readLine()) != null) {
                    builderJson.append(line);
                }
                in.close();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }


            return builderJson.toString();
        }
    }



    public class Route {
        public Distance distance;
        public Duration duration;
        public String endAddress;
        public String startAddress;
        public LatLng startLocation;
        public LatLng endLocation;
        public List<Step> steps;
        public List<LatLng> points;
    }


    public class Step {
        public LatLng startLocation;
        public LatLng endLocation;
        public Distance distance;
        public Duration duration;
        public List<LatLng> points;


    }

    public class Distance {
        public Distance(int value, String text) {
            this.value = value;
            this.text = text;
        }

        public final int value;
        public final String text;
    }

    public class Duration {
        public Duration(int value, String text) {
            this.value = value;
            this.text = text;
        }

        public final int value;
        public final String text;
    }
}


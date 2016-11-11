package br.com.etm.exampletegooglemaps;


import android.*;
import android.Manifest;
import android.app.ProgressDialog;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;


import br.com.etm.exampletegooglemaps.utils.DirectionFinder;
import br.com.etm.exampletegooglemaps.utils.DirectionFinder.Route;
import br.com.etm.exampletegooglemaps.utils.DirectionFinder.Step;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DirectionFinder.DirectionFinderListener {

    private static int permsRequestCode = 200;

    private GoogleMap mMap;

    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;

    private EditText et_origin;
    private EditText et_destination;
    private TextView tv_km;
    private TextView tv_time;
    private Button btn_find;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        et_origin = (EditText) findViewById(R.id.et_origin);
        et_destination = (EditText) findViewById(R.id.et_destination);

        tv_time = (TextView) findViewById(R.id.tv_time);
        tv_km = (TextView) findViewById(R.id.tv_km);

        btn_find = (Button) findViewById(R.id.bt_find);

        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (et_origin.getText().toString().equals("")) {
                        Toast.makeText(MapsActivity.this, "Origin required!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (et_destination.getText().toString().equals("")) {
                        Toast.makeText(MapsActivity.this, "Destination required!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String origin;
                    if (!et_origin.getText().toString().equals(getResources().getString(R.string.my_location)))
                        origin = et_origin.getText().toString().replaceAll(" ", "%20");
                    else
                        origin = mMap.getMyLocation().getLatitude() + "," + mMap.getMyLocation().getLongitude();

                    String destination = et_destination.getText().toString().replaceAll(" ", "%20");
                    List<LatLng> waypoints = new ArrayList<>();
//                    waypoints.add(new LatLng(-19.540649, -40.638458)); // Marista
//                    waypoints.add(new LatLng(-19.533710, -40.626498)); // Conde de Linhares
//                    waypoints.add(new LatLng(-19.520094, -40.623822)); // FCB
//                    waypoints.add(new LatLng(-19.528099, -40.659217)); // UNESC

                    new DirectionFinder(origin, waypoints, destination).withKey(getResources().getString(R.string.google_maps_key))
                            .setListener(MapsActivity.this)
//                            .setAlternatives(true)
                            .execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setTrafficEnabled(true);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (mMap.getMyLocation() == null)
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                else {
                    et_origin.setText(getResources().getString(R.string.my_location));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude()), 16));
                }
                return false;
            }
        });


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String[] perms = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"};
                requestPermissions(perms, permsRequestCode);
            }
            return;
        }
        mMap.setMyLocationEnabled(true);

    }

    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);

    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();
        double distance = 0, duration = 0;
        for (int i = (routes.size() - 1); i >= 0; i--) {
            Route route = routes.get(i);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 10));
            tv_km.setText(route.distance.text);
            tv_time.setText(route.duration.text);
//            distance += route.distance.value;
//            duration += route.duration.value;

            mMap.addMarker(new MarkerOptions()
                    .title(route.startAddress)
                    .position(route.startLocation));

            mMap.addMarker(new MarkerOptions()
                    .title(route.endAddress)
                    .position(route.endLocation));


            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (Step step : route.steps) {
                for (int j = 0; j < step.points.size(); j++)
                    polylineOptions.add(step.points.get(j));
            }
            mMap.addPolyline(polylineOptions);

        }
//        tv_km.setText(distance/1000 + " km");
//        tv_time.setText(((duration/60)/60) + " horas");

    }


}

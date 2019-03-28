package com.example.singlelocationrecognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //For user position
    private static final int REQUEST_CODE = 1000;
    TextView txt_location;
    Button btn_start,btn_stop;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;


    //For user heading
    private float currentDegree = 0f; //never used originally
    private SensorManager mSensorManager;
    TextView txt_heading;

    //For recognition. Input x and y.
    private double locationlat = 37.7585111;
    private double locationlon = -122.3914671;

    private double userlon = 0;
    private double userlat = 0;
    private double userheadingdeg = 0;

    private boolean recognized = false;
    TextView txt_recognized;

    private boolean validdist = false;
    TextView txt_validdist;

    //For bug testing
    private double distance;
    TextView txt_distance;
    private float headingdifference;
    TextView txt_headingdifference;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch (requestCode)
        {
            case REQUEST_CODE:
            {
                if(grantResults.length>0)
                {
                    if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    {

                    }
                    else if(grantResults[0] == PackageManager.PERMISSION_DENIED)
                    {

                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init View of Location
        txt_location = (TextView) findViewById(R.id.txt_location);
        btn_start = (Button) findViewById(R.id.btn_start_updates);
        btn_stop =  (Button) findViewById(R.id.btn_stop_updates);

        //Init View of Heading
        txt_heading = (TextView) findViewById(R.id.txt_heading);
        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //Init view of all of the textviews
        txt_distance = (TextView) findViewById(R.id.txt_distance);
        txt_headingdifference = (TextView) findViewById(R.id.txt_headingdifference);
        txt_recognized = (TextView) findViewById(R.id.txt_recognized);

        //Necesary for location retrieval
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);
        }
        else
        {
            //if permission is granted
            buildLocationRequest();
            buildLocationCallBack();

            //Cr
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            //Set event for button
            btn_start.setOnClickListener(new View.OnClickListener() {
                @Override

                public void onClick(View v) {

                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                    }

                    fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());

                    btn_start.setEnabled(!btn_start.isEnabled());
                    btn_stop.setEnabled(!btn_stop.isEnabled());


                }
            });

            btn_stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)

                        ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);

                    fusedLocationProviderClient.removeLocationUpdates(locationCallback);

                    btn_start.setEnabled(!btn_start.isEnabled());
                    btn_stop.setEnabled(!btn_stop.isEnabled());
                }
            });
        }


    }


    //For heading
    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {

            // get the angle around the z-axis rotated
            double degree = Math.round(event.values[0]);
            userheadingdeg = event.values[0];

            txt_heading.setText("Heading: " + Double.toString(degree) + " degrees");
            txt_headingdifference.setText("Heading Difference: " + (userheadingdeg - radToDegree(calcRadBearing(userlat,userlon,locationlat,locationlon))) + " Degrees");

            // Continuous checking for now, on any heading change.
            recognized = compareDistance() && compareHeading();
            txt_recognized.setText("Recognized?: " + String.valueOf(recognized));



        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //For location
    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()){
                    userlon = location.getLongitude();
                    userlat = location.getLatitude();

                    txt_location.setText(String.valueOf(location.getLatitude())
                            + "/"
                            + String.valueOf(location.getLongitude()));

                    txt_distance.setText("Distance: "+radToFeet(calcRadDistance(userlat,userlon,locationlat,locationlon)) + "feet");

                    // Continuous checking for now, on any heading change.
                    recognized = compareDistance() && compareHeading();
                    txt_recognized.setText("Recognized?:" + String.valueOf(recognized));
                }


            }
        };
    }

    //Location request settings (accuracy and interval)
    private void buildLocationRequest() {

        long mult = (long) Math.pow(10,-1000);
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5);
        locationRequest.setFastestInterval(3);
    }

    private double calcRadDistance(double lat1, double lon1, double lat2, double lon2){

        double t1 = Math.sin(lat1) * Math.sin(lat2);
        double t2 = Math.cos(lat1) * Math.cos(lat2);
        double t3 = Math.cos(lon1 - lon2);
        double t4 = t2 * t3;
        double t5 = t1 + t4;
        double rad_dist = Math.atan(-t5/Math.sqrt(-t5 * t5 +1)) + 2 * Math.atan(1);
        return (rad_dist);

    }

    private double radToFeet(double rad){
        return(rad * 3437.74677 * 1.1508 *  5.2800102998e+3);
    }

    private double calcRadBearing(double lat1, double lon1, double lat2, double lon2){

        double t1 = Math.sin(lat1) * Math.sin(lat2);
        double t2 = Math.cos(lat1) * Math.cos(lat2);
        double t3 = Math.cos(lon1 - lon2);
        double t4 = t2 * t3;
        double t5 = t1 + t4;
        double rad_dist = Math.atan(-t5/Math.sqrt(-t5 * t5 +1)) + 2 * Math.atan(1);
        t1 = Math.sin(lat2) - Math.sin(lat1) * Math.cos(rad_dist);
        t2 = Math.cos(lat1) * Math.sin(rad_dist);
        t3 = t1/t2;

        double rad_bearing;

        if(Math.sin(lon2 - lon1) < 0)
        {
            t4 = Math.atan(-t3 /Math.sqrt(-t3 * t3 + 1)) + 2 * Math.atan(1);
            rad_bearing = t4;
        }
        else
        {
            t4 = -t3 * t3 + 1;
            t5 = 2 * Math.PI - (Math.atan(-t3 / Math.sqrt(-t3 * t3 + 1)) + 2 * Math.atan(1));
            rad_bearing = t5;
        }
        return(rad_bearing);


    }

    private double radToDegree(double rad){

        return rad * (180 / Math.PI);

    }

    private boolean compareHeading(){
        double maxheadingdiffdeg = 60;
        double degree = radToDegree(calcRadBearing(userlat,userlon,locationlat, locationlon));

        return (Math.abs(userheadingdeg-degree) < maxheadingdiffdeg);
    }

    private boolean compareDistance(){
        double maxdistance = 50;
        double feet = radToFeet(calcRadDistance(userlat, userlon, locationlat, locationlon));

        return feet < maxdistance;

    }
}

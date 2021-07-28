package org.asteroidos.sync.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

public class GPSTracker extends Service implements LocationListener {

    private final Context mContext;

    // Flag for GPS status
    boolean mCanGetLocation;

    double mLatitude; // Latitude
    double mLongitude; // Longitude

    // Declaring a Location Manager
    protected LocationManager mLocationManager;

    public GPSTracker(Context context) {
        this.mContext = context;
        mCanGetLocation = false;
        updateLocation();
    }

    public void updateLocation() {
        try {
            mLocationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setSpeedRequired(false);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            String provider = mLocationManager.getBestProvider(criteria, true);
            if(provider != null) {
                mLocationManager.requestSingleUpdate(provider, this, null);
                mCanGetLocation = true;
            }
        }
        catch (SecurityException | NullPointerException e) {
            e.printStackTrace();
        }
    }


    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app.
     * */
    public void stopUsingGPS(){
        mCanGetLocation = false;
        if(mLocationManager != null){
            mLocationManager.removeUpdates(GPSTracker.this);
        }
    }


    /**
     * Function to get mLatitude
     * */
    public double getLatitude(){
        return mLatitude;
    }


    /**
     * Function to get mLongitude
     * */
    public double getLongitude(){
        return mLongitude;
    }

    public void gotLocation() {
        mCanGetLocation = false;
    }

    /**
     * Function to check GPS/Wi-Fi enabled
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.mCanGetLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
    }


    @Override
    public void onProviderDisabled(String provider) {
    }


    @Override
    public void onProviderEnabled(String provider) {
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
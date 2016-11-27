/*
 * Copyright (C) 2016 - Florent Revest <revestflo@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.asteroidos.sync.ble;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.UUID;

import github.vatsal.easyweather.Helper.ForecastCallback;
import github.vatsal.easyweather.WeatherMap;
import github.vatsal.easyweather.retrofit.models.ForecastResponseModel;
import github.vatsal.easyweather.retrofit.models.List;

public class WeatherService implements BleDevice.ReadWriteListener, LocationListener {
    public static final UUID weatherCityCharac     = UUID.fromString("00008001-0000-0000-0000-00a57e401d05");
    public static final UUID weatherIdsCharac      = UUID.fromString("00008002-0000-0000-0000-00a57e401d05");
    public static final UUID weatherMinTempsCharac = UUID.fromString("00008003-0000-0000-0000-00a57e401d05");
    public static final UUID weatherMaxTempsCharac = UUID.fromString("00008004-0000-0000-0000-00a57e401d05");

    public static final String owmApiKey = "ffcb5a7ed134aac3d095fa628bc46c65";

    private BleDevice mDevice;
    private Context mCtx;

    public WeatherService(Context ctx, BleDevice device) {
        mDevice = device;
        mCtx = ctx;
    }

    public void sync() {
        LocationManager lm = (LocationManager) mCtx.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(mCtx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mCtx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_LOW);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_LOW);
        lm.requestSingleUpdate(lm.getBestProvider(criteria, false), this, null);
    }

    public void unsync() {}

    @Override
    public void onLocationChanged(Location location) {
        WeatherMap weatherMap = new WeatherMap(mCtx, owmApiKey);
        weatherMap.getLocationForecast(String.valueOf(location.getLongitude()), String.valueOf(location.getLatitude()), new ForecastCallback() {
            @Override
            public void success(ForecastResponseModel response) {
                List[] l = response.getList();
                final byte[] city = response.getCity().getName().getBytes(StandardCharsets.UTF_8);
                final byte[] ids = new byte[10];
                final byte[] maxTemps = new byte[10];
                final byte[] minTemps = new byte[10];

                int currentDay, i=0;

                for (int j = 0; j < 5; j++) { // For each day of forecast
                    currentDay = dayOfTimestamp(Long.parseLong(l[i].getDt()));
                    short min = Short.MAX_VALUE;
                    short max = Short.MIN_VALUE;
                    int id = 0;
                    while(i < l.length && dayOfTimestamp(Long.parseLong(l[i].getDt())) == currentDay) { // For each data point of the day
                        // TODO is there a better way to select the most significant ID than the first of the afternoon ?
                        if(hourOfTimestamp(Long.parseLong(l[i].getDt())) >= 12 && id == 0)
                            id = Short.parseShort(l[i].getWeather()[0].getId());

                        short currentTemp = (short)Math.round(Float.parseFloat(l[i].getMain().getTemp()));
                        if (currentTemp > max) max = currentTemp;
                        if (currentTemp < min) min = currentTemp;

                        currentDay = dayOfTimestamp(Long.parseLong(l[i].getDt()));
                        i = i+1;
                    }

                    ids[2*j] = (byte)(id >> 8);
                    ids[2*j+1] = (byte)id;
                    maxTemps[2*j] = (byte)(max >> 8);
                    maxTemps[2*j+1] = (byte)max;
                    minTemps[2*j] = (byte)(min >> 8);
                    minTemps[2*j+1] = (byte)min;
                }

                mDevice.write(weatherCityCharac, city, WeatherService.this);
                mDevice.write(weatherIdsCharac, ids, WeatherService.this);
                mDevice.write(weatherMaxTempsCharac, maxTemps, WeatherService.this);
                mDevice.write(weatherMinTempsCharac, minTemps, WeatherService.this);
            }

            @Override public void failure(String message) {
                Log.e("WeatherService", "Could not get weather from owm");
            }
        });
    }

    @Override public void onStatusChanged(String s, int i, Bundle bundle) {}
    @Override public void onProviderEnabled(String s) {}
    @Override public void onProviderDisabled(String s) {}

    private int dayOfTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp*1000);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    private int hourOfTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp*1000);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    @Override
    public void onEvent(ReadWriteEvent e) {
        if(!e.wasSuccess())
            Log.e("WeatherService", e.status().toString());
    }
}

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

package org.asteroidos.sync.connectivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;


import org.asteroidos.sync.asteroid.IAsteroidDevice;
import org.asteroidos.sync.services.GPSTracker;
import org.asteroidos.sync.utils.AsteroidUUIDS;
import org.osmdroid.config.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import github.vatsal.easyweather.Helper.ForecastCallback;
import github.vatsal.easyweather.WeatherMap;
import github.vatsal.easyweather.retrofit.models.ForecastResponseModel;
import github.vatsal.easyweather.retrofit.models.List;

public class WeatherService implements IConnectivityService {

    public static final String PREFS_OWM_API_KEY = "owmApiKey";
    public static final String PREFS_OWM_API_KEY_DEFAULT = "d2820c63d757912426d74a20e75fb3ce";

    public static final String PREFS_NAME = "WeatherPreferences";
    public static final String PREFS_LATITUDE = "latitude";
    public static final float PREFS_LATITUDE_DEFAULT = (float) 40.7128;
    public static final String PREFS_LONGITUDE = "longitude";
    public static final float PREFS_LONGITUDE_DEFAULT = (float) -74.006;
    public static final String PREFS_ZOOM = "zoom";
    public static final float PREFS_ZOOM_DEFAULT = (float) 7.0;
    public static final String PREFS_SYNC_WEATHER = "syncWeather";
    public static final boolean PREFS_SYNC_WEATHER_DEFAULT = false;
    public static final String WEATHER_SYNC_INTENT = "org.asteroidos.sync.WEATHER_SYNC_REQUEST_LISTENER";

    private IAsteroidDevice mDevice;
    private Context mCtx;
    private SharedPreferences mSettings;

    private WeatherSyncReqReceiver mSReceiver;
    private PendingIntent mAlarmPendingIntent;
    private AlarmManager mAlarmMgr;

    private GPSTracker mGPS;
    private Float mLatitude;
    private Float mLongitude;

    private String mOwmKey;

    public WeatherService(Context ctx, IAsteroidDevice device) {
        mDevice = device;
        mCtx = ctx;

        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        mSettings = mCtx.getSharedPreferences(PREFS_NAME, 0);
        mLatitude = mSettings.getFloat(PREFS_LATITUDE, PREFS_LATITUDE_DEFAULT);
        mLongitude = mSettings.getFloat(PREFS_LONGITUDE, PREFS_LONGITUDE_DEFAULT);
        mOwmKey = mSettings.getString(PREFS_OWM_API_KEY, PREFS_OWM_API_KEY_DEFAULT);
    }

    @Override
    public void sync() {
        updateWeather();

        // Register a broadcast handler to use for the alarm Intent
        mSReceiver = new WeatherSyncReqReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WEATHER_SYNC_INTENT);
        mCtx.registerReceiver(mSReceiver, filter);

        // Fire update intent every 30 Minutes to update Weather
        Intent alarmIntent = new Intent(WEATHER_SYNC_INTENT);
        mAlarmPendingIntent = PendingIntent.getBroadcast(mCtx, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
        mAlarmMgr = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
        mAlarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HALF_HOUR,
                AlarmManager.INTERVAL_HALF_HOUR, mAlarmPendingIntent);
    }

    @Override
    public void unsync() {
        try {
            mCtx.unregisterReceiver(mSReceiver);
        } catch (IllegalArgumentException ignored) {}

        if (mAlarmMgr != null) {
            mAlarmMgr.cancel(mAlarmPendingIntent);
        }
    }

    private void updateWeather() {
        if (mSettings.getBoolean(PREFS_SYNC_WEATHER, PREFS_SYNC_WEATHER_DEFAULT)) {
            if (mGPS == null) {
                mGPS = new GPSTracker(mCtx);
            }
            // Check if GPS enabled
            mGPS.updateLocation();
            if (mGPS.canGetLocation()) {
                mLatitude = (float) mGPS.getLatitude();
                mLongitude = (float) mGPS.getLongitude();
                mGPS.gotLocation();
                if(isNearNull(mLatitude) && isNearNull(mLongitude) ) {
                    // We don't have a valid Location yet
                    // Use the old location until we have a new one, recheck in 2 Minutes
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updateWeather();
                        }
                    }, 1000 * 60 * 2);
                    return;
                }
            }
            // } else {
                // Can't get location.
                // GPS or network is available, don't set new Location and reuse the old one.
            // }
        } else {
            if (mGPS != null) {
                mGPS.stopUsingGPS();
                mGPS = null;
            }
            mLatitude = mSettings.getFloat(PREFS_LATITUDE, PREFS_LATITUDE_DEFAULT);
            mLongitude = mSettings.getFloat(PREFS_LONGITUDE, PREFS_LONGITUDE_DEFAULT);
        }
        updateWeather(mLatitude, mLongitude);

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putFloat(WeatherService.PREFS_LATITUDE, mLatitude);
        editor.putFloat(WeatherService.PREFS_LONGITUDE, mLongitude);
        editor.apply();
    }

    private boolean isNearNull(float coord) {
        return -0.000001f < coord && coord < 0.000001f;
    }

    private void updateWeather(float latitude, float longitude) {
        mOwmKey = mSettings.getString(PREFS_OWM_API_KEY, PREFS_OWM_API_KEY_DEFAULT);

        WeatherMap weatherMap = new WeatherMap(mCtx, mOwmKey);
        weatherMap.getLocationForecast(String.valueOf(latitude), String.valueOf(longitude), new ForecastCallback() {
            @Override
            public void success(ForecastResponseModel response) {
                List[] l = response.getList();
                String cityName = response.getCity().getName();
                byte[] city = {};
                if(cityName != null)
                    city = cityName.getBytes(StandardCharsets.UTF_8);
                final byte[] ids = new byte[10];
                final byte[] maxTemps = new byte[10];
                final byte[] minTemps = new byte[10];

                int currentDay, i=0;

                try {
                    for (int j = 0; j < 5; j++) { // For each day of forecast
                        currentDay = dayOfTimestamp(Long.parseLong(l[i].getDt()));
                        short min = Short.MAX_VALUE;
                        short max = Short.MIN_VALUE;
                        int id = 0;
                        while (i < l.length && dayOfTimestamp(Long.parseLong(l[i].getDt())) == currentDay) { // For each data point of the day
                            // TODO is there a better way to select the most significant ID than the first of the afternoon ?
                            if (hourOfTimestamp(Long.parseLong(l[i].getDt())) >= 12 && id == 0)
                                id = Short.parseShort(l[i].getWeather()[0].getId());

                            short currentTemp = (short) Math.round(Float.parseFloat(l[i].getMain().getTemp()));
                            if (currentTemp > max) max = currentTemp;
                            if (currentTemp < min) min = currentTemp;

                            currentDay = dayOfTimestamp(Long.parseLong(l[i].getDt()));
                            i = i + 1;
                        }

                        ids[2 * j] = (byte) (id >> 8);
                        ids[2 * j + 1] = (byte) id;
                        maxTemps[2 * j] = (byte) (max >> 8);
                        maxTemps[2 * j + 1] = (byte) max;
                        minTemps[2 * j] = (byte) (min >> 8);
                        minTemps[2 * j + 1] = (byte) min;
                    }
                } catch(java.lang.ArrayIndexOutOfBoundsException ignored) {}

                mDevice.send(AsteroidUUIDS.WEATHER_CITY_CHAR, city, WeatherService.this);
                mDevice.send(AsteroidUUIDS.WEATHER_IDS_CHAR, ids, WeatherService.this);
                mDevice.send(AsteroidUUIDS.WEATHER_MAX_TEMPS_CHAR, maxTemps, WeatherService.this);
                mDevice.send(AsteroidUUIDS.WEATHER_MIN_TEMPS_CHAR, minTemps, WeatherService.this);
            }

            @Override public void failure(String message) {
                Log.e("WeatherService", "Could not get weather from owm");
            }
        });
    }

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
    public HashMap<UUID, Direction> getCharacteristicUUIDs() {
        HashMap<UUID, Direction> chars = new HashMap<>();
        chars.put(AsteroidUUIDS.WEATHER_CITY_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.WEATHER_IDS_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.WEATHER_MIN_TEMPS_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.WEATHER_MAX_TEMPS_CHAR, Direction.TO_WATCH);
        return chars;
    }

    @Override
    public UUID getServiceUUID() {
        return AsteroidUUIDS.WEATHER_SERVICE_UUID;
    }

    class WeatherSyncReqReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWeather();
        }
    }
}

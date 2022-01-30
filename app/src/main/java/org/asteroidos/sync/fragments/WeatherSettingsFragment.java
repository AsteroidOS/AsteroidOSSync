package org.asteroidos.sync.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.asteroidos.sync.R;
import org.asteroidos.sync.connectivity.WeatherService;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class WeatherSettingsFragment extends Fragment {
    private MapView mMapView;
    private SharedPreferences mSettings;
    private Button mButton;

    private SharedPreferences mWeatherSyncSettings;
    private CheckBox mWeatherSyncCheckBox;

    private String mOwmKey;

    public static final int WEATHER_LOCATION_SYNC_PERMISSION_REQUEST = 1;
    public static final int WEATHER_LOCATION_PERMISSION_REQUEST = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        mSettings = getContext().getSharedPreferences(WeatherService.PREFS_NAME, 0);
        setHasOptionsMenu(true);

        return inflater.inflate(R.layout.fragment_position_picker, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        float latitude = mSettings.getFloat(WeatherService.PREFS_LATITUDE, WeatherService.PREFS_LATITUDE_DEFAULT);
        float longitude = mSettings.getFloat(WeatherService.PREFS_LONGITUDE, WeatherService.PREFS_LONGITUDE_DEFAULT);
        float zoom = mSettings.getFloat(WeatherService.PREFS_ZOOM, WeatherService.PREFS_ZOOM_DEFAULT);
        mOwmKey = mSettings.getString(WeatherService.PREFS_OWM_API_KEY, WeatherService.PREFS_OWM_API_KEY_DEFAULT);

        mMapView = view.findViewById(R.id.map);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setMultiTouchControls(true);
        mMapView.setVisibility(View.GONE);
        mMapView.setZoomRounding(true);
        mMapView.setMaxZoomLevel(13.0);
        mMapView.setMinZoomLevel(5.0);
        mMapView.getController().setZoom(zoom);
        mMapView.getController().setCenter(new GeoPoint(latitude, longitude));

        if (getActivity() != null && ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    WEATHER_LOCATION_PERMISSION_REQUEST);
        } else if (getContext() != null) {
            MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), mMapView);
            mLocationOverlay.enableMyLocation();
            mMapView.getOverlays().add(mLocationOverlay);
        }

        mButton = view.findViewById(R.id.positionPickerButton);
        mButton.setOnClickListener(v -> {
            IGeoPoint center = mMapView.getMapCenter();

            float latitude1 = (float) center.getLatitude();
            float longitude1 = (float) center.getLongitude();
            float zoom1 = (float) mMapView.getZoomLevelDouble();

            SharedPreferences.Editor editor = mSettings.edit();
            editor.putFloat(WeatherService.PREFS_LATITUDE, latitude1);
            editor.putFloat(WeatherService.PREFS_LONGITUDE, longitude1);
            editor.putFloat(WeatherService.PREFS_ZOOM, zoom1);
            editor.apply();

            // Update the Weather after changing it
            getActivity().sendBroadcast(new Intent(WeatherService.WEATHER_SYNC_INTENT));

            getActivity().onBackPressed();
        });

        mWeatherSyncSettings = getActivity().getSharedPreferences(WeatherService.PREFS_NAME, 0);

        mWeatherSyncCheckBox = view.findViewById(R.id.autoLocationPickerButton);
        mWeatherSyncCheckBox.setChecked(mWeatherSyncSettings.getBoolean(WeatherService.PREFS_SYNC_WEATHER, WeatherService.PREFS_SYNC_WEATHER_DEFAULT));
        mWeatherSyncCheckBox.setOnCheckedChangeListener((ignored, checked) -> {
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        WEATHER_LOCATION_SYNC_PERMISSION_REQUEST);
            } else {
                handleLocationToggle(mWeatherSyncCheckBox.isChecked());
            }
        });

        mButton.setVisibility(mWeatherSyncCheckBox.isChecked() ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.owm_position_picker_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.customApiKey) {
            View view = View.inflate(getActivity(), R.layout.dialog_api_key, null);
            EditText editText = view.findViewById(R.id.apikey);
            editText.setText(mOwmKey);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setPositiveButton(R.string.apply, (dialog, which) -> {
                String apiKey = String.valueOf(editText.getText());
                if (apiKey.equals(""))
                    apiKey = WeatherService.PREFS_OWM_API_KEY_DEFAULT;

                SharedPreferences.Editor editor = mWeatherSyncSettings.edit();
                editor.putString(WeatherService.PREFS_OWM_API_KEY, apiKey);
                editor.apply();
                mOwmKey = apiKey;
            });
            builder.setNegativeButton(R.string.cancel, ((dialog, which) -> dialog.cancel()));
            builder.setView(view);

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        return (super.onOptionsItemSelected(menuItem));
    }

    private void handleLocationToggle(boolean enable) {
        SharedPreferences.Editor editor = mWeatherSyncSettings.edit();
        editor.putBoolean(WeatherService.PREFS_SYNC_WEATHER, enable);
        editor.apply();
        mButton.setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
        getActivity().sendBroadcast(new Intent(WeatherService.WEATHER_SYNC_INTENT));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case WEATHER_LOCATION_SYNC_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handleLocationToggle(mWeatherSyncCheckBox.isChecked());
                } else {
                    handleLocationToggle(false);
                    mWeatherSyncCheckBox.setChecked(false);
                }
                break;
            }
            case WEATHER_LOCATION_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), mMapView);
                    mLocationOverlay.enableMyLocation();
                    mMapView.getOverlays().add(mLocationOverlay);
                }
            }
        }
    }
}

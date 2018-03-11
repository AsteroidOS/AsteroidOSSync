package org.asteroidos.sync.fragments;

import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.asteroidos.sync.R;
import org.asteroidos.sync.ble.WeatherService;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class PositionPickerFragment extends Fragment {
    private MapView mMapView;
    private SharedPreferences mSettings;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        mSettings = getContext().getSharedPreferences(WeatherService.PREFS_NAME, 0);

        return inflater.inflate(R.layout.fragment_position_picker, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        float latitude = mSettings.getFloat(WeatherService.PREFS_LATITUDE, WeatherService.PREFS_LATITUDE_DEFAULT);
        float longitude = mSettings.getFloat(WeatherService.PREFS_LONGITUDE, WeatherService.PREFS_LONGITUDE_DEFAULT);
        float zoom = mSettings.getFloat(WeatherService.PREFS_ZOOM, WeatherService.PREFS_ZOOM_DEFAULT);

        mMapView = view.findViewById(R.id.map);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setMultiTouchControls(true);
        mMapView.setBuiltInZoomControls(false);
        mMapView.setMaxZoomLevel(13.0);
        mMapView.setMinZoomLevel(5.0);
        mMapView.getController().setZoom(zoom);
        mMapView.getController().setCenter(new GeoPoint(latitude, longitude));

        Button mButton = view.findViewById(R.id.positionPickerButton);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IGeoPoint center = mMapView.getMapCenter();

                float latitude = (float) center.getLatitude();
                float longitude = (float) center.getLongitude();
                float zoom = (float) mMapView.getZoomLevelDouble();

                SharedPreferences.Editor editor = mSettings.edit();
                editor.putFloat(WeatherService.PREFS_LATITUDE, latitude);
                editor.putFloat(WeatherService.PREFS_LONGITUDE, longitude);
                editor.putFloat(WeatherService.PREFS_ZOOM, zoom);
                editor.apply();

                getActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }
}

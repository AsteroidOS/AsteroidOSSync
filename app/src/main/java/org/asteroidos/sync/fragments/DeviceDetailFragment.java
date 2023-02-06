/*
 * AsteroidOSSync
 * Copyright (c) 2023 AsteroidOS
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

package org.asteroidos.sync.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.asteroidos.sync.R;
import org.asteroidos.sync.asteroid.IAsteroidDevice;
import org.asteroidos.sync.connectivity.SilentModeService;
import org.asteroidos.sync.connectivity.TimeService;
import org.asteroidos.sync.services.PhoneStateReceiver;

public class DeviceDetailFragment extends Fragment {
    private TextView mDisconnectedText;
    private TextView mBatteryText;

    private LinearLayout mDisconnectedPlaceholder;
    private LinearLayout mConnectedContent;

    private SharedPreferences mTimeSyncSettings;
    private SharedPreferences mSilenceModeSettings;
    private SharedPreferences mCallStateSettings;

    private FloatingActionButton mFab;

    private boolean mConnected = false;

    private IAsteroidDevice.ConnectionState mStatus = IAsteroidDevice.ConnectionState.STATUS_DISCONNECTED;
    private int mBatteryPercentage = 100;
    private DeviceDetailFragment.OnDefaultDeviceUnselectedListener mDeviceListener;
    private DeviceDetailFragment.OnConnectRequestedListener mConnectListener;
    private DeviceDetailFragment.OnAppSettingsClickedListener mAppSettingsListener;
    private DeviceDetailFragment.OnWeatherSettingsClickedListener mWeatherSettingsListener;
    private DeviceDetailFragment.OnUpdateListener mUpdateListener;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_detail, parent, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        mUpdateListener.onUpdateRequested();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mFab = view.findViewById(R.id.fab);
        mFab.setOnClickListener(fabView -> {
            if (mConnected)
                mConnectListener.onDisconnectRequested();
            else
                mConnectListener.onConnectRequested();
        });

        mDisconnectedText = view.findViewById(R.id.info_disconnected);
        mBatteryText = view.findViewById(R.id.info_battery);
        mBatteryText.setText(mBatteryPercentage + " %");

        mConnectedContent = view.findViewById(R.id.device_connected_content);
        mDisconnectedPlaceholder = view.findViewById(R.id.device_disconnected_placeholder);

        CardView weatherCard = view.findViewById(R.id.card_view1);
        weatherCard.setOnClickListener(weatherCardView -> mWeatherSettingsListener.onWeatherSettingsClicked());

        CardView findCard = view.findViewById(R.id.card_view2);
        findCard.setOnClickListener(FindCardView -> {
            Intent iremove = new Intent("org.asteroidos.sync.NOTIFICATION_LISTENER");
            iremove.putExtra("event", "removed");
            iremove.putExtra("id", 0xa57e401d);
            getActivity().sendBroadcast(iremove);

            Intent ipost = new Intent("org.asteroidos.sync.NOTIFICATION_LISTENER");
            ipost.putExtra("event", "posted");
            ipost.putExtra("packageName", "org.asteroidos.sync.findmywatch");
            ipost.putExtra("id", 0xa57e401d);
            ipost.putExtra("appName", getString(R.string.app_name));
            ipost.putExtra("appIcon", "ios-watch-vibrating");
            ipost.putExtra("summary", getString(R.string.watch_finder));
            ipost.putExtra("body", getString(R.string.phone_is_searching));
            getActivity().sendBroadcast(ipost);
        });

        CardView screenshotCard = view.findViewById(R.id.card_view3);
        screenshotCard.setOnClickListener(view1 -> getActivity().sendBroadcast(new Intent("org.asteroidos.sync.SCREENSHOT_REQUEST_LISTENER")));

        CardView notifSettCard = view.findViewById(R.id.card_view4);
        notifSettCard.setOnClickListener(notifSettCardView -> mAppSettingsListener.onAppSettingsClicked());

        mTimeSyncSettings = getActivity().getSharedPreferences(TimeService.PREFS_NAME, 0);

        CheckBox mTimeSyncCheckBox = view.findViewById(R.id.timeSyncCheckBox);
        mTimeSyncCheckBox.setChecked(mTimeSyncSettings.getBoolean(TimeService.PREFS_SYNC_TIME, TimeService.PREFS_SYNC_TIME_DEFAULT));
        mTimeSyncCheckBox.setOnCheckedChangeListener((ignored, checked) -> {
            SharedPreferences.Editor editor = mTimeSyncSettings.edit();
            editor.putBoolean(TimeService.PREFS_SYNC_TIME, checked);
            editor.apply();
        });

        mSilenceModeSettings = getActivity().getSharedPreferences(SilentModeService.PREFS_NAME, Context.MODE_PRIVATE);
        CheckBox mSilenceModeCheckBox = view.findViewById(R.id.SilentModeCheckBox);
        mSilenceModeCheckBox.setChecked(mSilenceModeSettings.getBoolean(SilentModeService.PREF_RINGER, false));
        mSilenceModeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = mSilenceModeSettings.edit();
            editor.putBoolean(SilentModeService.PREF_RINGER, isChecked);
            editor.apply();
        });

        mCallStateSettings = getActivity().getSharedPreferences(PhoneStateReceiver.PREFS_NAME, Context.MODE_PRIVATE);
        CheckBox mCallStateServiceCheckBox = view.findViewById(R.id.CallStateServiceCheckBox);
        mCallStateServiceCheckBox.setChecked(mCallStateSettings.getBoolean(PhoneStateReceiver.PREF_SEND_CALL_STATE, true));
        mCallStateServiceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = mCallStateSettings.edit();
            editor.putBoolean(PhoneStateReceiver.PREF_SEND_CALL_STATE, isChecked);
            editor.apply();
        });

        setStatus(mStatus);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.device_detail_manu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.unpairButton)
            mDeviceListener.onDefaultDeviceUnselected();

        return (super.onOptionsItemSelected(menuItem));
    }

    public void setLocalName(String name) {
        getActivity().setTitle(name);
    }

    public void setStatus(IAsteroidDevice.ConnectionState status) {
        mStatus = status;
        if (status == IAsteroidDevice.ConnectionState.STATUS_CONNECTED){
            mDisconnectedPlaceholder.setVisibility(View.GONE);
            mConnectedContent.setVisibility(View.VISIBLE);
            mFab.setImageResource(R.drawable.bluetooth_disconnect);
            mConnected = true;
            setMenuVisibility(true);
        } else if (status == IAsteroidDevice.ConnectionState.STATUS_DISCONNECTED){
            mDisconnectedPlaceholder.setVisibility(View.VISIBLE);
            mConnectedContent.setVisibility(View.GONE);
            mDisconnectedText.setText(R.string.disconnected);
            mFab.setImageResource(R.drawable.bluetooth_connect);
            mConnected = false;
            setMenuVisibility(true);
        } else if (status == IAsteroidDevice.ConnectionState.STATUS_CONNECTING) {
            mDisconnectedPlaceholder.setVisibility(View.VISIBLE);
            mConnectedContent.setVisibility(View.GONE);
            mDisconnectedText.setText(R.string.connecting);
            setMenuVisibility(true);
        } else {
            setMenuVisibility(false);
        }
    }

    @SuppressLint("SetTextI18n")
    public void setBatteryPercentage(int percentage) {
        try {
            mBatteryText.setText(mBatteryPercentage + " %");
            mBatteryPercentage = percentage;
        } catch (IllegalStateException ignore) {
        }
    }

    public void scanningStarted() {
        if (mStatus == IAsteroidDevice.ConnectionState.STATUS_DISCONNECTED)
            mDisconnectedText.setText(R.string.scanning);
    }

    public void scanningStopped() {
        if (mStatus == IAsteroidDevice.ConnectionState.STATUS_DISCONNECTED)
            mDisconnectedText.setText(R.string.disconnected);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DeviceDetailFragment.OnDefaultDeviceUnselectedListener)
            mDeviceListener = (DeviceDetailFragment.OnDefaultDeviceUnselectedListener) context;
        else
            throw new ClassCastException(context.toString()
                    + " does not implement DeviceDetailFragment.OnDefaultDeviceUnselectedListener");

        if (context instanceof DeviceDetailFragment.OnConnectRequestedListener)
            mConnectListener = (DeviceDetailFragment.OnConnectRequestedListener) context;
        else
            throw new ClassCastException(context.toString()
                    + " does not implement DeviceDetailFragment.OnConnectRequestedListener");

        if (context instanceof DeviceDetailFragment.OnAppSettingsClickedListener)
            mAppSettingsListener = (DeviceDetailFragment.OnAppSettingsClickedListener) context;
        else
            throw new ClassCastException(context.toString()
                    + " does not implement DeviceDetailFragment.OnAppSettingsClickedListener");

        if (context instanceof DeviceDetailFragment.OnWeatherSettingsClickedListener)
            mWeatherSettingsListener = (DeviceDetailFragment.OnWeatherSettingsClickedListener) context;
        else
            throw new ClassCastException(context.toString()
                    + " does not implement DeviceDetailFragment.OnWeatherSettingsClickedListener");

        if (context instanceof DeviceDetailFragment.OnUpdateListener)
            mUpdateListener = (DeviceDetailFragment.OnUpdateListener) context;
        else
            throw new ClassCastException(context.toString()
                    + " does not implement DeviceDetailFragment.onUpdateListener");
    }

    /* Notifies MainActivity when a device unpairing is requested */
    public interface OnDefaultDeviceUnselectedListener {
        void onDefaultDeviceUnselected();
    }

    public interface OnAppSettingsClickedListener {
        void onAppSettingsClicked();
    }

    public interface OnWeatherSettingsClickedListener {
        void onWeatherSettingsClicked();
    }

    public interface OnConnectRequestedListener {
        void onConnectRequested();
        void onDisconnectRequested();
    }

    public interface OnUpdateListener {
        void onUpdateRequested();
    }

}

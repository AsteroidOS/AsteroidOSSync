/*
 * Copyright (C) 2016 - Florent Revest <revestflo@gmail.com>
 *                      Doug Koellmer <dougkoellmer@hotmail.com>
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

package org.asteroidos.sync;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;

import static com.idevicesinc.sweetblue.BleManager.get;

public class DeviceDetailFragment extends Fragment implements BleDevice.StateListener {
    public static final String ARG_DEVICE_ADDRESS = "device_address";

    private WeatherService mWeatherService;
    private NotificationService mNotificationService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_DEVICE_ADDRESS)) {
            BleManager bleMngr = get(getActivity().getApplication());

            BleDevice device = bleMngr.getDevice(getArguments().getString(ARG_DEVICE_ADDRESS));
            device.setListener_State(this);
            device.setListener_ConnectionFail(new BleDevice.DefaultConnectionFailListener()
            {
                @Override public Please onEvent(ConnectionFailEvent event)
                {
                    Please please = super.onEvent(event);

                    if( !please.isRetry() )
                    {
                        final String toast =  event.device().getName_debug() + " connection failed with " + event.failureCountSoFar() + " retries - " + event.status();
                        Toast.makeText(getContext(), toast, Toast.LENGTH_LONG).show();
                    }

                    return please;
                }
            });

            mWeatherService = new WeatherService(getActivity(), device);
            mNotificationService = new NotificationService(getActivity(), device);

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(device.getName_normalized());
            }

            device.connect();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.device_detail, container, false);
    }

    @Override
    public void onEvent(StateEvent event) {
        if (event.didEnter(BleDeviceState.INITIALIZED)) {
            Log.i("DeviceDetailFragment", event.device().getName_debug() + " just initialized!");

            if (mWeatherService != null)
                mWeatherService.sync();
            if (mNotificationService != null)
                mNotificationService.sync();
        }
    }
}

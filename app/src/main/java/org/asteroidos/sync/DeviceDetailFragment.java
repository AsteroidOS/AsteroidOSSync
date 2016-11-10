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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;

import static com.idevicesinc.sweetblue.BleManager.get;

public class DeviceDetailFragment extends Fragment implements BleDevice.StateListener {
    public static final String ARG_DEVICE_ADDRESS = "device_address";

    private WeatherService mWeatherService;
    private NotificationService mNotificationService;
    private MediaService mMediaService;

    private TextView mConnectedText;
    private ImageView mConnectedImage;

    private CardView mScreenshotCard;
    private CardView mFindCard;
    private CardView mNotifSettCard;

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
                        final String toast = event.device().getName_debug() + " connection failed with " + event.failureCountSoFar() + " retries - " + event.status();
                        if(getContext() != null)
                            Toast.makeText(getContext(), toast, Toast.LENGTH_LONG).show();
                    }

                    return please;
                }
            });

            mWeatherService = new WeatherService(getActivity(), device);
            mNotificationService = new NotificationService(getActivity(), device);
            mMediaService = new MediaService(getActivity(), device);

            getActivity().setTitle(device.getName_normalized());

            device.connect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mWeatherService != null)
            mWeatherService.unsync();
        if (mNotificationService != null)
            mNotificationService.unsync();
        if (mMediaService != null)
            mMediaService.unsync();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.device_detail, container, false);
    }

    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);
        mConnectedText = (TextView)getActivity().findViewById(R.id.info_connected);
        mConnectedImage = (ImageView)getActivity().findViewById(R.id.info_icon_connected);

        mScreenshotCard = (CardView)getActivity().findViewById(R.id.card_view1);
        mScreenshotCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getContext() != null)
                    Toast.makeText(getContext(), "Not supported yet", Toast.LENGTH_SHORT).show();
            }
        });
        mFindCard = (CardView)getActivity().findViewById(R.id.card_view2);
        mFindCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getContext() != null) {
                    Intent i = new  Intent("org.asteroidos.sync.NOTIFICATION_LISTENER");
                    i.putExtra("event", "posted");
                    i.putExtra("packageName", "org.asteroidos.sync");
                    i.putExtra("id", 0);
                    i.putExtra("appName", getString(R.string.app_name));
                    i.putExtra("appIcon", "");
                    i.putExtra("summary", getString(R.string.watch_finder));
                    i.putExtra("body", getString(R.string.phone_is_searching));

                    getActivity().sendBroadcast(i);
                }
            }
        });
        mNotifSettCard = (CardView)getActivity().findViewById(R.id.card_view3);
        mNotifSettCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getContext() != null)
                    Toast.makeText(getContext(), "Not supported yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEvent(StateEvent event) {
        if (event.didEnter(BleDeviceState.INITIALIZED)) {
            Log.i("DeviceDetailFragment", event.device().getName_debug() + " just initialized!");

            if(mConnectedText != null)
                mConnectedText.setText(R.string.connected);
            if(mConnectedImage != null)
                mConnectedImage.setImageResource(R.mipmap.android_cloud);

            if (mWeatherService != null)
                mWeatherService.sync();
            if (mNotificationService != null)
                mNotificationService.sync();
            if (mMediaService != null)
                mMediaService.sync();
        } else if (event.didEnter(BleDeviceState.DISCONNECTED)) {
            Log.i("DeviceDetailFragment", event.device().getName_debug() + " just disconnected!");

            if(mConnectedText != null)
                mConnectedText.setText(R.string.disconnected);
            if(mConnectedImage != null)
                mConnectedImage.setImageResource(R.mipmap.android_cloud);

            if (mWeatherService != null)
                mWeatherService.unsync();
            if (mNotificationService != null)
                mNotificationService.unsync();
            if (mMediaService != null)
                mMediaService.unsync();
        }
    }
}

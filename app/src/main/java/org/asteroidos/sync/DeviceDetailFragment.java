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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceDetailFragment extends Fragment {
    public static final String ARG_DEVICE_ADDRESS = "device_address";
    private String mDeviceAddress;

    private TextView mConnectedText;
    private ImageView mConnectedImage;

    private TextView mBatteryText;
    private ImageView mBatteryImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getContext().bindService(new Intent(getContext(),
                SynchronizationService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

        mDeviceAddress = getArguments().getString(ARG_DEVICE_ADDRESS, "");
    }

    class SynchronizationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SynchronizationService.MSG_SET_LOCAL_NAME:
                    getActivity().setTitle((String)msg.obj);
                    break;
                case SynchronizationService.MSG_SET_STATUS:
                    switch(msg.arg1) {
                        case SynchronizationService.STATUS_CONNECTED:
                            if(mConnectedText != null)
                                mConnectedText.setText(R.string.connected);
                            if(mConnectedImage != null)
                                mConnectedImage.setImageResource(R.mipmap.android_cloud_done);
                            break;
                        case SynchronizationService.STATUS_DISCONNECTED:
                            if(mConnectedText != null)
                                mConnectedText.setText(R.string.disconnected);
                            if(mConnectedImage != null)
                                mConnectedImage.setImageResource(R.mipmap.android_cloud);
                            if(mBatteryText != null)
                                mBatteryText.setVisibility(View.INVISIBLE);
                            if(mBatteryImage != null)
                                mBatteryImage.setVisibility(View.INVISIBLE);
                            break;
                        case SynchronizationService.STATUS_CONNECTING:
                            if(mConnectedText != null)
                                mConnectedText.setText(R.string.connecting);
                            if(mConnectedImage != null)
                                mConnectedImage.setImageResource(R.mipmap.android_cloud);
                            break;
                        default:
                            break;
                    }
                    break;
                case SynchronizationService.MSG_SET_BATTERY_PERCENTAGE:
                    if(mBatteryText != null)
                        mBatteryText.setVisibility(View.VISIBLE);
                    if(mBatteryImage != null)
                        mBatteryImage.setVisibility(View.VISIBLE);
                    mBatteryText.setText(getString(R.string.percentage, msg.arg1));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    final Messenger mMessenger = new Messenger(new SynchronizationHandler());
    Messenger mService = null;
    boolean mIsBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, SynchronizationService.MSG_CONNECT);
                msg.obj = mDeviceAddress;
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException ignored) {}
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, SynchronizationService.MSG_DISCONNECT);
                    msg.obj = mDeviceAddress;
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException ignored) {}
            }

            getContext().unbindService(mConnection);
            mIsBound = false;
        }
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

        mBatteryText = (TextView)getActivity().findViewById(R.id.info_battery);
        mBatteryImage = (ImageView)getActivity().findViewById(R.id.info_icon_battery);

        CardView mScreenshotCard = (CardView) getActivity().findViewById(R.id.card_view1);
        mScreenshotCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getContext() != null)
                    Toast.makeText(getContext(), "Not supported yet", Toast.LENGTH_SHORT).show();
            }
        });
        CardView mFindCard = (CardView) getActivity().findViewById(R.id.card_view2);
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
        CardView mNotifSettCard = (CardView) getActivity().findViewById(R.id.card_view3);
        mNotifSettCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getContext() != null)
                    Toast.makeText(getContext(), "Not supported yet", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

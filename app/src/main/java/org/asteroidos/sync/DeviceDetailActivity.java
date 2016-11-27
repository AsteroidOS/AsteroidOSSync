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
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.asteroidos.sync.services.SynchronizationService;

public class DeviceDetailActivity extends AppCompatActivity {
    public static final String ARG_DEVICE_ADDRESS = "device_address";
    private String mDeviceAddress;

    private TextView mConnectedText;
    private ImageView mConnectedImage;

    private TextView mBatteryText;
    private ImageView mBatteryImage;

    final Messenger mMessenger = new Messenger(new SynchronizationHandler());
    Messenger mService = null;
    boolean mIsBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        bindService(new Intent(this, SynchronizationService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

        mDeviceAddress = getIntent().getStringExtra(ARG_DEVICE_ADDRESS);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        mConnectedText = (TextView)findViewById(R.id.info_connected);
        mConnectedImage = (ImageView)findViewById(R.id.info_icon_connected);

        mBatteryText = (TextView)findViewById(R.id.info_battery);
        mBatteryImage = (ImageView)findViewById(R.id.info_icon_battery);

        CardView mScreenshotCard = (CardView)findViewById(R.id.card_view1);
        mScreenshotCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DeviceDetailActivity.this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            }
        });
        CardView mFindCard = (CardView)findViewById(R.id.card_view2);
        mFindCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new  Intent("org.asteroidos.sync.NOTIFICATION_LISTENER");
                i.putExtra("event", "posted");
                i.putExtra("packageName", "org.asteroidos.sync");
                i.putExtra("id", 0);
                i.putExtra("appName", getString(R.string.app_name));
                i.putExtra("appIcon", "");
                i.putExtra("summary", getString(R.string.watch_finder));
                i.putExtra("body", getString(R.string.phone_is_searching));
                sendBroadcast(i);
            }
        });
        CardView mNotifSettCard = (CardView) findViewById(R.id.card_view3);
        mNotifSettCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DeviceDetailActivity.this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            }
        });
    }

    class SynchronizationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SynchronizationService.MSG_SET_LOCAL_NAME:
                    setTitle((String)msg.obj);
                    break;
                case SynchronizationService.MSG_SET_STATUS:
                    switch(msg.arg1) {
                        case SynchronizationService.STATUS_CONNECTED:
                            mConnectedText.setText(R.string.connected);
                            mConnectedImage.setImageResource(R.mipmap.android_cloud_done);
                            break;
                        case SynchronizationService.STATUS_DISCONNECTED:
                            mConnectedText.setText(R.string.disconnected);
                            mConnectedImage.setImageResource(R.mipmap.android_cloud);
                            mBatteryText.setVisibility(View.INVISIBLE);
                            mBatteryImage.setVisibility(View.INVISIBLE);
                            break;
                        case SynchronizationService.STATUS_CONNECTING:
                            mConnectedText.setText(R.string.connecting);
                            mConnectedImage.setImageResource(R.mipmap.android_cloud);
                            break;
                        default:
                            break;
                    }
                    break;
                case SynchronizationService.MSG_SET_BATTERY_PERCENTAGE:
                    mBatteryText.setVisibility(View.VISIBLE);
                    mBatteryImage.setVisibility(View.VISIBLE);
                    mBatteryText.setText(getString(R.string.percentage, msg.arg1));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

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

            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(this, DeviceListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

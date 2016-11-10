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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;

import static com.idevicesinc.sweetblue.BleManager.get;

public class DeviceListActivity extends AppCompatActivity implements BleManager.StateListener,
        BleManager.NativeStateListener, BleManager.DiscoveryListener {
    private static final Interval SCAN_TIMEOUT = Interval.secs(10.0);

    private BleManager mBleMngr;
    private final BleManagerConfig m_bleManagerConfig = new BleManagerConfig() {{
        this.undiscoveryKeepAlive = Interval.DISABLED;
        this.loggingEnabled = true;
    }};

    private LeDeviceListAdapter mLeDeviceListAdapter;

    private RippleBackground mRippleBackground;
    private TextView searchingText;

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        searchingText = (TextView)findViewById(R.id.searchingText);

        mRippleBackground = (RippleBackground)findViewById(R.id.content);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBleMngr.turnOn();
                mBleMngr.startScan(SCAN_TIMEOUT);
                mRippleBackground.startRippleAnimation();
            }
        });

        if (findViewById(R.id.device_detail_container) != null)
            mTwoPane = true;

        BluetoothEnabler.start(this);
        mBleMngr = get(getApplication(), m_bleManagerConfig);
        mBleMngr.setListener_State(this);
        mBleMngr.setListener_NativeState(this);
        mBleMngr.setListener_Discovery(this);

        AlertManager m_alertMngr = new AlertManager(this, mBleMngr);

        if (!mBleMngr.isBleSupported())
            m_alertMngr.showBleNotSupported();
        else if (!mBleMngr.is(BleManagerState.ON))
            mBleMngr.turnOn();

        mBleMngr.startScan(SCAN_TIMEOUT);

        ListView mScanListView = (ListView) findViewById(R.id.device_list);
        assert mScanListView != null;
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mScanListView.setAdapter(mLeDeviceListAdapter);

        mScanListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final BleDevice device = mLeDeviceListAdapter.getDevice(i);
                if (device == null) return;
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(DeviceDetailFragment.ARG_DEVICE_ADDRESS, device.getMacAddress());
                    DeviceDetailFragment fragment = new DeviceDetailFragment();
                    fragment.setArguments(arguments);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.device_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, DeviceDetailActivity.class);
                    intent.putExtra(DeviceDetailFragment.ARG_DEVICE_ADDRESS, device.getMacAddress());

                    context.startActivity(intent);
                }
            }
        });

        ComponentName cn = new ComponentName(this, NLService.class);
        String flat = Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners");
        if (!(flat != null && flat.contains(cn.flattenToString()))) {
            new AlertDialog.Builder(this)
                    .setTitle("Notifications")
                    .setMessage("To enable notifications synchronization, you need to allow AsteroidOS Sync in the following scren.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                            startActivity(intent);
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBleMngr.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBleMngr.onPause();
    }

    @Override
    public void onEvent(BleManager.StateListener.StateEvent event) {
        if(event.didExit(BleManagerState.SCANNING)) {
            mRippleBackground.stopRippleAnimation();
            int deviceCount = mLeDeviceListAdapter.getCount();
            if(deviceCount == 0)
                searchingText.setText(R.string.nothing_found);
            else if(deviceCount == 1)
                searchingText.setText(R.string.one_found);
            else
                searchingText.setText(String.valueOf(deviceCount) + R.string.n_found);
        }
        else if(event.didEnter(BleManagerState.SCANNING)) {
            mRippleBackground.startRippleAnimation();
            searchingText.setText(R.string.searching);
        }
    }

    @Override
    public void onEvent(BleManager.NativeStateListener.NativeStateEvent event) {}

    @Override
    public void onEvent(BleManager.DiscoveryListener.DiscoveryEvent event) {
        if (event.was(BleManager.DiscoveryListener.LifeCycle.DISCOVERED)) {
            mLeDeviceListAdapter.addDevice(event.device());
            mLeDeviceListAdapter.notifyDataSetChanged();
        } else if (event.was(BleManager.DiscoveryListener.LifeCycle.UNDISCOVERED)) {
            mLeDeviceListAdapter.removeDevice(event.device());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BleDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = DeviceListActivity.this.getLayoutInflater();
        }

        public void addDevice(BleDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public void removeDevice(BleDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.remove(device);
            }
        }

        public BleDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.device_list_content, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.content);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BleDevice device = mLeDevices.get(i);
            final String deviceName = device.getName_normalized();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
    }
}

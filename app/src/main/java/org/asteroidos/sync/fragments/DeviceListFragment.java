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

package org.asteroidos.sync.fragments;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.skyfishjy.library.RippleBackground;

import org.asteroidos.sync.R;

import java.util.ArrayList;
import java.util.Objects;

public class DeviceListFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private RippleBackground mRippleBackground;
    private TextView mSearchingText;
    private OnDefaultDeviceSelectedListener mDeviceListener;
    private OnScanRequestedListener mScanListener;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_list, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mSearchingText = view.findViewById(R.id.searchingText);
        mRippleBackground = view.findViewById(R.id.content);
        FloatingActionButton fab = view.findViewById(R.id.fab);
        ListView mScanListView = view.findViewById(R.id.device_list);

        mRippleBackground.startRippleAnimation();

        fab.setOnClickListener(this);

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mScanListView.setAdapter(mLeDeviceListAdapter);
        mScanListView.setOnItemClickListener(this);
    }

    /* Fab events */
    @Override
    public void onClick(View view) {
        mScanListener.onScanRequested();
    }

    /* Device selection */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(i);
        if (device != null)
            mDeviceListener.onDefaultDeviceSelected(device);
    }

    /* Scanning events handling */
    public void scanningStarted() {
        if (mRippleBackground != null) mRippleBackground.startRippleAnimation();
        if (mSearchingText != null) mSearchingText.setText(R.string.searching);
    }

    public void scanningStopped() {
        mRippleBackground.stopRippleAnimation();
        int deviceCount = mLeDeviceListAdapter.getCount();
        if (deviceCount == 0)
            mSearchingText.setText(R.string.nothing_found);
        else if (deviceCount == 1)
            mSearchingText.setText(R.string.one_found);
        else
            mSearchingText.setText(getString(R.string.n_found, deviceCount));
    }

    public void deviceDiscovered(BluetoothDevice dev) {
        mLeDeviceListAdapter.addDevice(dev);
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    public void deviceUndiscovered(BluetoothDevice dev) {
        mLeDeviceListAdapter.removeDevice(dev);
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDefaultDeviceSelectedListener)
            mDeviceListener = (OnDefaultDeviceSelectedListener) context;
        else
            throw new ClassCastException(context.toString()
                    + " does not implement DeviceListFragment.OnDeviceSelectedListener");

        if (context instanceof OnScanRequestedListener)
            mScanListener = (OnScanRequestedListener) context;
        else
            throw new ClassCastException(context.toString()
                    + " does not implement DeviceListFragment.OnScanRequestedListener");
    }

    /* Notifies MainActivity when a device pairing or scanning is requested */
    public interface OnDefaultDeviceSelectedListener {
        void onDefaultDeviceSelected(BluetoothDevice mDevice);
    }

    public interface OnScanRequestedListener {
        void onScanRequested();
    }

    private static class ViewHolder {
        TextView deviceName;
    }

    /* Adapter for holding devices found through scanning */
    private class LeDeviceListAdapter extends BaseAdapter {
        private final ArrayList<BluetoothDevice> mLeDevices;
        private final LayoutInflater mInflator;

        LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = requireActivity().getLayoutInflater();
        }

        void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        void removeDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.remove(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
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
                view = mInflator.inflate(R.layout.device_list_item, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = view.findViewById(R.id.content);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);

            /*
            The below function was generated by Android Studio to ensure `device.getName` occurs successfully.

            As the message from Android Studio `lint`:
            "Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]"

            ~ Doomsdayrs
             */
            if (ActivityCompat.checkSelfPermission(view.getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return view;
            }
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            return view;
        }
    }
}

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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleDevice;

import com.skyfishjy.library.RippleBackground;

import org.asteroidos.sync.R;

import java.util.ArrayList;

public class DeviceListFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private RippleBackground mRippleBackground;
    private TextView mSearchingText;

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
        final BleDevice device = mLeDeviceListAdapter.getDevice(i);
        if (device != null)
            mDeviceListener.onDefaultDeviceSelected(device.getMacAddress());
    }

    /* Scanning events handling */
    public void scanningStarted()
    {
        mRippleBackground.startRippleAnimation();
        mSearchingText.setText(R.string.searching);
    }

    public void scanningStopped()
    {
        mRippleBackground.stopRippleAnimation();
        int deviceCount = mLeDeviceListAdapter.getCount();
        if(deviceCount == 0)
            mSearchingText.setText(R.string.nothing_found);
        else if(deviceCount == 1)
            mSearchingText.setText(R.string.one_found);
        else
            mSearchingText.setText(getString(R.string.n_found, deviceCount));
    }

    public void deviceDiscovered(BleDevice dev)
    {
        mLeDeviceListAdapter.addDevice(dev);
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    public void deviceUndiscovered(BleDevice dev)
    {
        mLeDeviceListAdapter.removeDevice(dev);
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    /* Adapter for holding devices found through scanning */
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BleDevice> mLeDevices;
        private LayoutInflater mInflator;

        LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = getActivity().getLayoutInflater();
        }

        void addDevice(BleDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        void removeDevice(BleDevice device) {
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
                view = mInflator.inflate(R.layout.device_list_item, null);
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
    private static class ViewHolder {
        TextView deviceName;
    }

    /* Notifies MainActivity when a device pairing or scanning is requested */
    public interface OnDefaultDeviceSelectedListener {
        void onDefaultDeviceSelected(String macAddress);
    }
    public interface OnScanRequestedListener {
        void onScanRequested();
    }
    private OnDefaultDeviceSelectedListener mDeviceListener;
    private OnScanRequestedListener mScanListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnDefaultDeviceSelectedListener)
            mDeviceListener = (OnDefaultDeviceSelectedListener) context;
        else
            throw new ClassCastException(context.toString()
                    + " does not implement DeviceListFragment.OnDeviceSelectedListener");

        if(context instanceof OnScanRequestedListener)
            mScanListener = (OnScanRequestedListener) context;
        else
            throw new ClassCastException(context.toString()
                    + " does not implement DeviceListFragment.OnScanRequestedListener");
    }
}

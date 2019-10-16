package org.asteroidos.sync;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.view.MenuItem;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;

import org.asteroidos.sync.fragments.AppListFragment;
import org.asteroidos.sync.fragments.DeviceDetailFragment;
import org.asteroidos.sync.fragments.DeviceListFragment;
import org.asteroidos.sync.fragments.PositionPickerFragment;
import org.asteroidos.sync.services.SynchronizationService;
import org.asteroidos.sync.utils.AppInfo;
import org.asteroidos.sync.utils.AppInfoHelper;

import java.util.ArrayList;

import static com.idevicesinc.sweetblue.BleManager.get;

@SuppressWarnings( "deprecation" ) // Before upgrading to SweetBlue 3.0, we don't have an alternative to the deprecated StateListener
public class MainActivity extends AppCompatActivity implements DeviceListFragment.OnDefaultDeviceSelectedListener,
        DeviceListFragment.OnScanRequestedListener, DeviceDetailFragment.OnDefaultDeviceUnselectedListener,
        DeviceDetailFragment.OnConnectRequestedListener, BleManager.DiscoveryListener,
        DeviceDetailFragment.OnAppSettingsClickedListener, DeviceDetailFragment.OnLocationSettingsClickedListener,
        DeviceDetailFragment.OnUpdateListener {
    private BleManager mBleMngr;
    private DeviceListFragment mListFragment;
    private DeviceDetailFragment mDetailFragment;
    private Fragment mPreviousFragment;
    Messenger mSyncServiceMessenger;
    Intent mSyncServiceIntent;
    final Messenger mDeviceDetailMessenger = new Messenger(new MainActivity.SynchronizationHandler(this));
    int mStatus = SynchronizationService.STATUS_DISCONNECTED;

    public static ArrayList<AppInfo> appInfoList;

    public static final String PREFS_NAME = "MainPreferences";
    public static final String PREFS_DEFAULT_MAC_ADDR = "defaultMacAddress";
    public static final String PREFS_DEFAULT_LOC_NAME = "defaultLocalName";

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String defaultDevMacAddr = mPrefs.getString(PREFS_DEFAULT_MAC_ADDR, "");

        Thread appInfoRetrieval = new Thread(new Runnable() {
            public void run() {
                appInfoList = AppInfoHelper.getPackageInfo(MainActivity.this);
            }
        });
        appInfoRetrieval.start();

        /* Start and/or attach to the Synchronization Service */
        mSyncServiceIntent = new Intent(this, SynchronizationService.class);
        startService(mSyncServiceIntent);

        BluetoothEnabler.start(this);
        mBleMngr = get(getApplication());
        mBleMngr.setListener_State(new ManagerStateListener() {
            @Override
            public void onEvent(BleManager.StateListener.StateEvent event) {
                if(event.didExit(BleManagerState.SCANNING)) {
                    if(mListFragment != null)        mListFragment.scanningStopped();
                    else if(mDetailFragment != null) mDetailFragment.scanningStopped();
                } else if(event.didEnter(BleManagerState.SCANNING)) {
                    if(mListFragment != null)        mListFragment.scanningStarted();
                    else if(mDetailFragment != null) mDetailFragment.scanningStarted();
                }
            }
        });
        mBleMngr.setListener_Discovery(this);

        if (savedInstanceState == null) {
            Fragment f;
            if (defaultDevMacAddr.isEmpty()) {
                f = mListFragment = new DeviceListFragment();
                onScanRequested();
            } else {
                setTitle(mPrefs.getString(PREFS_DEFAULT_LOC_NAME, ""));
                f = mDetailFragment = new DeviceDetailFragment();
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.flContainer, f);
            ft.commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mStatus != SynchronizationService.STATUS_CONNECTED)
            stopService(mSyncServiceIntent);
    }

    /* Fragments switching */
    @Override
    public void onDefaultDeviceSelected(String macAddress) {
        mDetailFragment = new DeviceDetailFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flContainer, mDetailFragment)
                .commit();

        try {
            Message msg = Message.obtain(null, SynchronizationService.MSG_SET_DEVICE);
            msg.obj = macAddress;
            msg.replyTo = mDeviceDetailMessenger;
            mSyncServiceMessenger.send(msg);
        } catch (RemoteException ignored) {}

        onConnectRequested();

        mListFragment = null;
    }

    @Override
    public void onDefaultDeviceUnselected() {
        mListFragment = new DeviceListFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flContainer, mListFragment)
                .commit();

        try {
            Message msg = Message.obtain(null, SynchronizationService.MSG_SET_DEVICE);
            msg.obj = "";
            msg.replyTo = mDeviceDetailMessenger;
            mSyncServiceMessenger.send(msg);
        } catch (RemoteException ignored) {}

        mDetailFragment = null;
        setTitle(R.string.app_name);
    }

    @Override
    public void onUpdateRequested() {
        try {
            Message msg = Message.obtain(null, SynchronizationService.MSG_UPDATE);
            msg.replyTo = mDeviceDetailMessenger;
            if(mSyncServiceMessenger != null)
                mSyncServiceMessenger.send(msg);
        } catch (RemoteException ignored) {}
    }

    /* Synchronization service events handling */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mSyncServiceMessenger = new Messenger(service);
            onUpdateRequested();
        }

        public void onServiceDisconnected(ComponentName className) {
            mSyncServiceMessenger = null;
        }
    };

    @Override
    public void onConnectRequested() {
        try {
            Message msg = Message.obtain(null, SynchronizationService.MSG_CONNECT);
            msg.replyTo = mDeviceDetailMessenger;
            mSyncServiceMessenger.send(msg);
        } catch (RemoteException ignored) {}
    }

    @Override
    public void onDisconnectRequested() {
        try {
            Message msg = Message.obtain(null, SynchronizationService.MSG_DISCONNECT);
            msg.replyTo = mDeviceDetailMessenger;
            mSyncServiceMessenger.send(msg);
        } catch (RemoteException ignored) {}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if(menuItem.getItemId() ==  android.R.id.home)
            onBackPressed();

        return (super.onOptionsItemSelected(menuItem));
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if(fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            setTitle(mPrefs.getString(PREFS_DEFAULT_LOC_NAME, ""));
            ActionBar ab = getSupportActionBar();
            if (ab != null)
                ab.setDisplayHomeAsUpEnabled(false);
        } else
            finish();
        try {
            mDetailFragment = (DeviceDetailFragment)mPreviousFragment;
        } catch (ClassCastException ignored1) {
            try {
                mListFragment = (DeviceListFragment)mPreviousFragment;
            } catch (ClassCastException ignored2) {}
        }
    }

    @Override
    public void onAppSettingsClicked() {
        Fragment f = new AppListFragment();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (mDetailFragment != null) {
            mPreviousFragment = mDetailFragment;
            mDetailFragment = null;
        }
        if (mListFragment != null) {
            mPreviousFragment = mListFragment;
            mListFragment = null;
        }
        ft.replace(R.id.flContainer, f);
        ft.addToBackStack(null);
        ft.commit();

        setTitle(getString(R.string.notifications_settings));
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onLocationSettingsClicked() {
        Fragment f = new PositionPickerFragment();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (mDetailFragment != null) {
            mPreviousFragment = mDetailFragment;
            mDetailFragment = null;
        }
        if (mListFragment != null) {
            mPreviousFragment = mListFragment;
            mListFragment = null;
        }
        ft.replace(R.id.flContainer, f);
        ft.addToBackStack(null);
        ft.commit();

        setTitle(getString(R.string.weather_settings));
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);
    }

    void handleSetLocalName(String name) {
        if(mDetailFragment != null)
            mDetailFragment.setLocalName(name);
    }

    void handleSetStatus(int status) {
        if(mDetailFragment != null) {
            mDetailFragment.setStatus(status);
            if(status == SynchronizationService.STATUS_CONNECTED) {
                try {
                    Message batteryMsg = Message.obtain(null, SynchronizationService.MSG_REQUEST_BATTERY_LIFE);
                    batteryMsg.replyTo = mDeviceDetailMessenger;
                    mSyncServiceMessenger.send(batteryMsg);
                } catch (RemoteException ignored) {}
            }
            mStatus = status;
        }
    }

    void handleBatteryPercentage(int percentage) {
        if(mDetailFragment != null)
            mDetailFragment.setBatteryPercentage(percentage);
    }

    static private class SynchronizationHandler extends Handler {
        private MainActivity mActivity;

        SynchronizationHandler(MainActivity activity) {
            mActivity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SynchronizationService.MSG_SET_LOCAL_NAME:
                    mActivity.handleSetLocalName((String)msg.obj);
                    break;
                case SynchronizationService.MSG_SET_STATUS:
                    mActivity.handleSetStatus(msg.arg1);
                    break;
                case SynchronizationService.MSG_SET_BATTERY_PERCENTAGE:
                    mActivity.handleBatteryPercentage(msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onEvent(BleManager.DiscoveryListener.DiscoveryEvent event) {
        if (mListFragment == null) return;

        if (event.was(BleManager.DiscoveryListener.LifeCycle.DISCOVERED))
            mListFragment.deviceDiscovered(event.device());
        else if (event.was(BleManager.DiscoveryListener.LifeCycle.UNDISCOVERED))
            mListFragment.deviceUndiscovered(event.device());
    }

    @Override
    public void onScanRequested() {
        mBleMngr.turnOn();
        mBleMngr.undiscoverAll();
        mBleMngr.startScan(Interval.secs(10.0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBleMngr.onResume();
        bindService(mSyncServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBleMngr.onPause();
        unbindService(mConnection);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
        int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
    }
}

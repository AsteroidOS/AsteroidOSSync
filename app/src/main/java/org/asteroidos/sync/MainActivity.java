package org.asteroidos.sync;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.Window;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;

import org.asteroidos.sync.fragments.AppListFragment;
import org.asteroidos.sync.fragments.DeviceListFragment;
import org.asteroidos.sync.fragments.DeviceDetailFragment;
import org.asteroidos.sync.fragments.PositionPickerFragment;
import org.asteroidos.sync.utils.AppInfo;
import org.asteroidos.sync.utils.AppInfoHelper;
import org.asteroidos.sync.services.NLService;
import org.asteroidos.sync.services.SynchronizationService;

import java.util.ArrayList;

import static com.idevicesinc.sweetblue.BleManager.get;

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
    final Messenger mDeviceDetailMessenger = new Messenger(new MainActivity.SynchronizationHandler());
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

        //white list the app
        //also need to add permission in manifest file
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // only for marshmallow and newer versions
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        /* Check that bluetooth is enabled */
        if (!mBleMngr.isBleSupported())
            showBleNotSupported();

        /* Check that notifications are enabled */
        ComponentName cn = new ComponentName(this, NLService.class);
        String flat = Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners");
        if (!(flat != null && flat.contains(cn.flattenToString()))) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.notifications)
                    .setMessage(R.string.notifications_enablement)
                    .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                            startActivity(intent);
                        }
                    })
                    .show();
        }

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

        mPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREFS_DEFAULT_MAC_ADDR, macAddress);
        editor.apply();

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

        mPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREFS_DEFAULT_MAC_ADDR, "");
        editor.putString(PREFS_DEFAULT_LOC_NAME, "");
        editor.apply();

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

            mPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String defaultDevMacAddr = mPrefs.getString(PREFS_DEFAULT_MAC_ADDR, "");
            String defaultLocalName = mPrefs.getString(PREFS_DEFAULT_LOC_NAME, "");

            if(!defaultDevMacAddr.isEmpty()) {
                if(!mBleMngr.hasDevice(defaultDevMacAddr))
                    mBleMngr.newDevice(defaultDevMacAddr, defaultLocalName);
                try {
                    Message msg = Message.obtain(null, SynchronizationService.MSG_SET_DEVICE);
                    msg.obj = defaultDevMacAddr;
                    msg.replyTo = mDeviceDetailMessenger;
                    mSyncServiceMessenger.send(msg);
                } catch (RemoteException ignored) {}

                onConnectRequested();
            }
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
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else
            finish();

        try {
            mDetailFragment = (DeviceDetailFragment)mPreviousFragment;
        } catch (ClassCastException ignored) {}
        try {
            mListFragment = (DeviceListFragment)mPreviousFragment;
        } catch (ClassCastException ignored) {}
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private class SynchronizationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if(mDetailFragment == null) return;

            switch (msg.what) {
                case SynchronizationService.MSG_SET_LOCAL_NAME:
                    String name = (String)msg.obj;
                    mDetailFragment.setLocalName(name);

                    mPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(PREFS_DEFAULT_LOC_NAME, name);
                    editor.apply();
                    break;
                case SynchronizationService.MSG_SET_STATUS:
                    mDetailFragment.setStatus(msg.arg1);
                    if(msg.arg1 == SynchronizationService.STATUS_CONNECTED) {
                        try {
                            Message batteryMsg = Message.obtain(null, SynchronizationService.MSG_REQUEST_BATTERY_LIFE);
                            batteryMsg.replyTo = mDeviceDetailMessenger;
                            mSyncServiceMessenger.send(batteryMsg);
                        } catch (RemoteException ignored) {}
                    }
                    mStatus = msg.arg1;
                    break;
                case SynchronizationService.MSG_SET_BATTERY_PERCENTAGE:
                    mDetailFragment.setBatteryPercentage(msg.arg1);
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

    public void showBleNotSupported() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        final android.app.AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        };

        dialog.setMessage(getString(R.string.ble_not_supported));
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                getString(R.string.generic_ok), clickListener);
        if (!isFinishing())
            dialog.show();
    }
}

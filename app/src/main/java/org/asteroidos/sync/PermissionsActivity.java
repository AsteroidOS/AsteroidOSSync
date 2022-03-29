package org.asteroidos.sync;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.asteroidos.sync.services.NLService;

import io.github.dreierf.materialintroscreen.MaterialIntroActivity;
import io.github.dreierf.materialintroscreen.SlideFragment;
import io.github.dreierf.materialintroscreen.SlideFragmentBuilder;

public class PermissionsActivity extends MaterialIntroActivity {
    private static final int BATTERYOPTIM_REQUEST = 0;
    private static final int NOTIFICATION_REQUEST = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PackageManager pm = getPackageManager();
        boolean hasBLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

        ActivityManager am = (ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        boolean isLowRamDevice = am.isLowRamDevice();

        if (hasBLE) {
            if (!isLowRamDevice) {
            SlideFragment welcomeFragment = new SlideFragmentBuilder()
                    .backgroundColor(R.color.colorintroslide1)
                    .buttonsColor(R.color.colorintroslide1button)
                    .image(R.drawable.introslide1icon)
                    .title(getString(R.string.intro_slide1_title))
                    .description(getString(R.string.intro_slide1_subtitle))
                    .build();

            SlideFragment externalStorageFragment = new SlideFragmentBuilder()
                    .backgroundColor(R.color.colorintroslide2)
                    .buttonsColor(R.color.colorintroslide2button)
                    .neededPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE})
                    .image(R.drawable.introslide2icon)
                    .title(getString(R.string.intro_slide2_title))
                    .description(getString(R.string.intro_slide2_subtitle))
                    .build();
            boolean externalStorageFragmentShown = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);

            SlideFragmentBuilder localizationFragmentBuilder = new SlideFragmentBuilder()
                    .backgroundColor(R.color.colorintroslide3)
                    .buttonsColor(R.color.colorintroslide3button)
                    .neededPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION})
                    .image(R.drawable.introslide3icon)
                    .title(getString(R.string.intro_slide3_title))
                    .description(getString(R.string.intro_slide3_subtitle));

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                localizationFragmentBuilder.neededPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
            }
            SlideFragment localizationFragment = localizationFragmentBuilder.build();

            boolean localizationFragmentShown = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                localizationFragmentShown |= (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED);
                localizationFragmentShown |= (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED);
            }

            NotificationsSlide notificationFragment = new NotificationsSlide();
            notificationFragment.setContext(this);
            boolean notificationFragmentShown = notificationFragment.hasAnyPermissionsToGrant();

            BatteryOptimSlide batteryOptimFragment = new BatteryOptimSlide();
            batteryOptimFragment.setContext(this);
            boolean batteryOptimFragmentShown = batteryOptimFragment.hasAnyPermissionsToGrant();

            SlideFragment phoneStateFragment = new SlideFragmentBuilder()
                                        .backgroundColor(R.color.colorintroslide2)
                                        .buttonsColor(R.color.colorintroslide2button)
                                        .neededPermissions(new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS})
                                        .image(R.drawable.ic_ring_volume)
                                        .title(getString(R.string.intro_phonestateslide_title))
                                        .description(getString(R.string.intro_phonestateslide_subtitle))
                                        .build();
            boolean phoneStateFragmentShown = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED);

            if (externalStorageFragmentShown || localizationFragmentShown ||
                    notificationFragmentShown || batteryOptimFragmentShown || phoneStateFragmentShown) {
                addSlide(welcomeFragment);
                if (externalStorageFragmentShown) addSlide(externalStorageFragment);
                if (localizationFragmentShown) addSlide(localizationFragment);
                if (notificationFragmentShown) addSlide(notificationFragment);
                if (batteryOptimFragmentShown) addSlide(batteryOptimFragment);
                if (phoneStateFragmentShown) addSlide(phoneStateFragment);
            } else
                startMainActivity();
        } else {
                addSlide(new AndroidGoSlide());
            }
        } else {
                addSlide(new BLENotSupportedSlide());
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
        this.finish();
    }

    @Override
    public void onFinish() {
        startMainActivity();
        super.onFinish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BATTERYOPTIM_REQUEST || requestCode == NOTIFICATION_REQUEST)
            updateMessageButtonVisible();

        super.onActivityResult(requestCode, resultCode, data);
    }

    static public class NotificationsSlide extends SlideFragment {
        Context mCtx;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            Bundle bundle = new Bundle();
            bundle.putInt("background_color", R.color.colorintroslide4);
            bundle.putInt("buttons_color", R.color.colorintroslide4button);
            bundle.putInt("image", R.drawable.introslide4icon);
            bundle.putString("title", mCtx.getString(R.string.intro_slide4_title));
            bundle.putString("description", mCtx.getString(R.string.intro_slide4_subtitle));
            setArguments(bundle);

            return super.onCreateView(inflater, container, savedInstanceState);
        }

        public void setContext(Context ctx) {
            mCtx = ctx;
        }

        @Override
        public boolean hasAnyPermissionsToGrant() {
            ComponentName cn = new ComponentName(mCtx, NLService.class);
            String flat = Settings.Secure.getString(mCtx.getContentResolver(), "enabled_notification_listeners");
            return (flat == null || !flat.contains(cn.flattenToString()));
        }

        @Override
        public void askForPermissions() {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            getActivity().startActivityForResult(intent, NOTIFICATION_REQUEST);
        }

        @Override
        public boolean canMoveFurther() {
            return !hasAnyPermissionsToGrant();
        }
    }

    static public class BatteryOptimSlide extends SlideFragment {
        Context mCtx;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            Bundle bundle = new Bundle();
            bundle.putInt("background_color", R.color.colorintroslide5);
            bundle.putInt("buttons_color", R.color.colorintroslide5button);
            bundle.putInt("image", R.drawable.introslide5icon);
            bundle.putString("title", mCtx.getString(R.string.intro_slide5_title));
            bundle.putString("description", mCtx.getString(R.string.intro_slide5_subtitle));
            setArguments(bundle);

            return super.onCreateView(inflater, container, savedInstanceState);
        }

        public void setContext(Context ctx) {
            mCtx = ctx;
        }

        @Override
        public boolean hasAnyPermissionsToGrant() {
                String packageName = mCtx.getPackageName();
                PowerManager pm = (PowerManager) mCtx.getSystemService(POWER_SERVICE);
                return (pm != null && !pm.isIgnoringBatteryOptimizations(packageName));
        }

        @Override
        public void askForPermissions() {
            Intent intent = new Intent();
            String packageName = mCtx.getPackageName();
            PowerManager pm = (PowerManager) mCtx.getSystemService(POWER_SERVICE);
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            getActivity().startActivityForResult(intent, BATTERYOPTIM_REQUEST);
        }

        @Override
        public boolean canMoveFurther() {
            return !hasAnyPermissionsToGrant();
        }
    }

    static public class BLENotSupportedSlide extends SlideFragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            Bundle bundle = new Bundle();
            bundle.putInt("background_color", R.color.colorintroslideerror);
            bundle.putInt("buttons_color", R.color.colorintroslideerrorbutton);
            bundle.putInt("image", R.drawable.introslidebluetoothicon);
            bundle.putString("title", inflater.getContext().getString(R.string.intro_slideerror_title));
            bundle.putString("description", inflater.getContext().getString(R.string.intro_slideerror_subtitle));
            setArguments(bundle);

            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public boolean canMoveFurther() {
            return false;
        }
    }

    static public class AndroidGoSlide extends SlideFragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            Bundle bundle = new Bundle();
            bundle.putInt("background_color", R.color.colorintroslideerror);
            bundle.putInt("buttons_color", R.color.colorintroslideerrorbutton);
            bundle.putInt("image", R.drawable.introslidelowramicon);
            bundle.putString("title", inflater.getContext().getString(R.string.intro_slideandroidgo_title));
            bundle.putString("description", inflater.getContext().getString(R.string.intro_slideandroidgo_subtitle));
            setArguments(bundle);

            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public boolean canMoveFurther() {
            return false;
        }
    }
}

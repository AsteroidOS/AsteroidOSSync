package dk.jens.backup;

// copied from https://github.com/jensstein/oandbackup, used under MIT license

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppInfoHelper
{
    final static String TAG = "AsteroidOS Sync";

    public static ArrayList<AppInfo> getPackageInfo(Context context)
    {
        ArrayList<AppInfo> list = new ArrayList<AppInfo>();
        ArrayList<String> packageNames = new ArrayList<String>();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> pinfoList = pm.getInstalledPackages(0);
        Collections.sort(pinfoList, pInfoPackageNameComparator);
        // list seemingly starts scrambled on 4.3

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for(PackageInfo pinfo : pinfoList)
        {
            packageNames.add(pinfo.packageName);
            boolean isSystem = false;
            if((pinfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
            {
                isSystem = true;
            }
            Bitmap icon = null;
            Drawable apkIcon = pm.getApplicationIcon(pinfo.applicationInfo);
            try
            {
                if(apkIcon instanceof BitmapDrawable) {
                    // getApplicationIcon gives a Drawable which is then cast as a BitmapDrawable
                    Bitmap src = ((BitmapDrawable) apkIcon).getBitmap();
                    if(src.getWidth() > 0 && src.getHeight() > 0) {
                        icon = Bitmap.createScaledBitmap(src,
                                src.getWidth(), src.getHeight(), true);
                    } else {
                        Log.d(TAG, String.format(
                                "icon for %s had invalid height or width (h: %d w: %d)",
                                pinfo.packageName, src.getHeight(), src.getWidth()));
                    }
                }
                else {
                    icon = Bitmap.createBitmap(apkIcon.getIntrinsicWidth(), apkIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(icon);
                    apkIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    apkIcon.draw(canvas);
                }
            }
            catch(ClassCastException e) {}
            // for now the error is ignored since logging it would fill a lot in the log
            String dataDir = pinfo.applicationInfo.dataDir;
            // workaround for dataDir being null for the android system
            // package at least on cm14
            if(pinfo.packageName.equals("android") && dataDir == null)
                dataDir = "/data/system";
            AppInfo appInfo = new AppInfo(pinfo.packageName,
                    pinfo.applicationInfo.loadLabel(pm).toString(),
                    pinfo.versionName, pinfo.versionCode,
                    pinfo.applicationInfo.sourceDir, dataDir, isSystem,
                    true);
            appInfo.icon = icon;
            list.add(appInfo);
        }
        return list;
    }
    public static Comparator<PackageInfo> pInfoPackageNameComparator = new Comparator<PackageInfo>()
    {
        public int compare(PackageInfo p1, PackageInfo p2)
        {
            return p1.packageName.compareToIgnoreCase(p2.packageName);
        }
    };
}

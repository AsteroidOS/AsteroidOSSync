package org.asteroidos.sync.utils;

// copied from https://github.com/jensstein/oandbackup, used under MIT license

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppInfoHelper
{
    private final static String TAG = AppInfoHelper.class.getSimpleName();

    public static ArrayList<AppInfo> getPackageInfo(Context context)
    {
        ArrayList<AppInfo> list = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> pinfoList = pm.getInstalledPackages(0);
        Collections.sort(pinfoList, pInfoPackageNameComparator);
        // list seemingly starts scrambled on 4.3

        for(PackageInfo pinfo : pinfoList)
        {
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
            } catch(ClassCastException ignored) {}
            AppInfo appInfo = new AppInfo(pinfo.packageName,
                    pinfo.applicationInfo.loadLabel(pm).toString(),
                    isSystem,
                    true);
            appInfo.icon = icon;
            list.add(appInfo);
        }
        return list;
    }
    private static Comparator<PackageInfo> pInfoPackageNameComparator = new Comparator<PackageInfo>()
    {
        public int compare(PackageInfo p1, PackageInfo p2)
        {
            return p1.packageName.compareToIgnoreCase(p2.packageName);
        }
    };
}

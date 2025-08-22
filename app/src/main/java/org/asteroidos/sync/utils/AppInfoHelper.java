/*
 * AsteroidOSSync
 * Copyright (c) 2023 AsteroidOS
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

package org.asteroidos.sync.utils;

// TODO MIT license in GPL project, consider rewriting class
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

import org.asteroidos.sync.R; 

import java.util.ArrayList;
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
        pinfoList.sort(pInfoPackageNameComparator);
        // list seemingly starts scrambled on 4.3

        for(PackageInfo pinfo : pinfoList)
        {
            boolean isSystem = (pinfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
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
                } else {
                    if (apkIcon.getIntrinsicWidth() <= 0 || apkIcon.getIntrinsicHeight() <= 0) {
                        Log.e("BitmapDebug", "Invalid dimensions! Width or height is not positive.");
                        // Handle the error appropriately:
                        // - Return a default bitmap
                        // - Skip this bitmap creation
                        // - Throw a custom exception with more context
                        // - etc.
                        apkIcon = context.getResources().getDrawable(R.drawable.introslide1icon);
                        icon = Bitmap.createBitmap(apkIcon.getIntrinsicWidth(), apkIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(icon);
                        apkIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        apkIcon.draw(canvas);
                    } else {
                        icon = Bitmap.createBitmap(apkIcon.getIntrinsicWidth(), apkIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(icon);
                        apkIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        apkIcon.draw(canvas);
                    }
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
    private static final Comparator<PackageInfo> pInfoPackageNameComparator = (p1, p2) -> p1.packageName.compareToIgnoreCase(p2.packageName);
}

/*
 * Copyright (C) 2016 - Florent Revest <revestflo@gmail.com>
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

package org.asteroidos.sync.ble;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice;

import org.asteroidos.sync.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.UUID;

public class ScreenshotService implements BleDevice.ReadWriteListener {
    private int NOTIFICATION = 2726;

    public static final UUID screenshotRequestCharac = UUID.fromString("00006001-0000-0000-0000-00a57e401d05");
    public static final UUID screenshotContentCharac = UUID.fromString("00006002-0000-0000-0000-00a57e401d05");

    private Context mCtx;
    private BleDevice mDevice;

    private ScreenshotReqReceiver mSReceiver;

    private boolean mFirstNotify = true;
    private boolean mDownloading = false;

    private NotificationManager mNM;

    public ScreenshotService(Context ctx, BleDevice device)
    {
        mDevice = device;
        mCtx = ctx;
        mNM = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void sync() {
        mDevice.enableNotify(screenshotContentCharac, contentListener);

        mSReceiver = new ScreenshotReqReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("org.asteroidos.sync.SCREENSHOT_REQUEST_LISTENER");
        mCtx.registerReceiver(mSReceiver, filter);

        mDownloading = false;
    }

    public void unsync() {
        mDevice.disableNotify(screenshotContentCharac);
        try {
            mCtx.unregisterReceiver(mSReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    private static int bytesToInt(byte[] b) {
        int result = 0;
        for (int i = 3; i >= 0; i--) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    private BleDevice.ReadWriteListener contentListener = new BleDevice.ReadWriteListener() {
        private int progress = 0;
        private int previousNotifiedProgress = -500;
        private int size = 0;
        private byte[] totalData;

        @Override
        public void onEvent(ReadWriteEvent e) {
            if(e.isNotification() && e.charUuid().equals(screenshotContentCharac)) {
                byte data[] = e.data();
                if(mFirstNotify) {
                    size = bytesToInt(data);
                    totalData = new byte[size];
                    mFirstNotify = false;
                    progress = 0;
                } else {
                    if(data.length + progress < totalData.length)
                        System.arraycopy(data, 0, totalData, progress, data.length);
                    progress += data.length;

                    Notification.Builder notificationBuilder = new Notification.Builder(mCtx)
                            .setContentTitle(mCtx.getText(R.string.screenshot));

                    if(size == progress) {
                        String dirStr = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/AsteroidOSSync";
                        File directory = new File(dirStr);
                        if(!directory.exists())
                            directory.mkdirs();
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis());
                        File fileName = new File(dirStr + "/Screenshot_" + timeStamp + ".jpg");

                        try {
                            FileOutputStream out = new FileOutputStream(fileName);
                            out.write(totalData);
                            out.close();
                        } catch(IOException ignored) {}

                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri = Uri.fromFile(fileName);
                        mediaScanIntent.setData(contentUri);

                        mCtx.sendBroadcast(mediaScanIntent);

                        notificationBuilder.setContentText(mCtx.getText(R.string.downloaded));
                        notificationBuilder.setLargeIcon(BitmapFactory.decodeByteArray(totalData, 0, size));
                        notificationBuilder.setSmallIcon(R.mipmap.android_image);

                        Intent notificationIntent = new Intent();
                        notificationIntent.setAction(Intent.ACTION_VIEW);
                        notificationIntent.setDataAndType(Uri.parse(fileName.getAbsolutePath()), "image/*");
                        PendingIntent contentIntent = PendingIntent.getActivity(mCtx, 0, notificationIntent, 0);
                        notificationBuilder.setContentIntent(contentIntent);

                        mDownloading = false;
                    } else {
                        notificationBuilder.setContentText(mCtx.getText(R.string.downloading));
                        notificationBuilder.setSmallIcon(R.mipmap.android_image);
                        notificationBuilder.setProgress(size, progress, false);
                    }

                    if(progress >= previousNotifiedProgress + 500 || progress == size) {
                        Notification notification = notificationBuilder.build();
                        mNM.notify(NOTIFICATION, notification);
                        previousNotifiedProgress = progress;
                    }
                }
            }
        }
    };

    @Override
    public void onEvent(ReadWriteEvent e) {
        if(!e.wasSuccess())
            Log.e("ScreenshotService", e.status().toString());
    }

    class ScreenshotReqReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!mDownloading) {
                mFirstNotify = true;
                mDownloading = true;
                byte[] data = new byte[1];
                data[0] = 0x0;
                mDevice.write(screenshotRequestCharac, data, ScreenshotService.this);
            }
        }
    }
}

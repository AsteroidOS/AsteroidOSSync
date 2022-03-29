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

package org.asteroidos.sync.connectivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import org.asteroidos.sync.R;
import org.asteroidos.sync.asteroid.IAsteroidDevice;
import org.asteroidos.sync.utils.AsteroidUUIDS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"FieldCanBeLocal"}) // For clarity, we prefer having NOTIFICATION as a top level field
public class ScreenshotService implements IConnectivityService {
    private static final String NOTIFICATION_CHANNEL_ID = "screenshotservice_channel_id_01";
    private int NOTIFICATION = 2726;

    private Context mCtx;
    private IAsteroidDevice mDevice;

    private ScreenshotReqReceiver mSReceiver;

    private boolean mFirstNotify = true;
    private boolean mDownloading = false;
    private int progress = 0;
    private int size = 0;
    private byte[] totalData;
    private ScheduledExecutorService processUpdate;

    private NotificationManager mNM;

    public ScreenshotService(Context ctx, IAsteroidDevice device) {
        mDevice = device;
        mCtx = ctx;

        mNM = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Screenshot Service", NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setDescription("Screenshot download");
            notificationChannel.setVibrationPattern(new long[]{0L});
            mNM.createNotificationChannel(notificationChannel);
        }

        device.registerCallback(AsteroidUUIDS.SCREENSHOT_CONTENT, data -> {
            if (data == null) return;
            if (data.length != 4){
                mFirstNotify = false;
            }
            if (mFirstNotify) {
                size = bytesToInt(data);
                totalData = new byte[size];
                mFirstNotify = false;
                progress = 0;

                processUpdate = Executors.newSingleThreadScheduledExecutor();
                processUpdate.scheduleWithFixedDelay(() -> {
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mCtx, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle(mCtx.getText(R.string.screenshot))
                            .setLocalOnly(true);
                    notificationBuilder.setContentText(mCtx.getText(R.string.downloading));
                    notificationBuilder.setSmallIcon(R.drawable.image_white);
                    notificationBuilder.setProgress(size, progress, false);

                    Notification notification = notificationBuilder.build();
                    mNM.notify(NOTIFICATION, notification);
                }, 0, 1, TimeUnit.SECONDS);
            } else {
                if (data.length + progress <= totalData.length)
                    System.arraycopy(data, 0, totalData, progress, data.length);
                progress += data.length;

                if (size == progress) {
                    processUpdate.shutdown();
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mCtx, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle(mCtx.getText(R.string.screenshot))
                            .setLocalOnly(true);

                    Uri fileName = null;
                    try {
                        fileName = createFile(totalData);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    notificationBuilder.setContentText(mCtx.getText(R.string.downloaded));
                    notificationBuilder.setLargeIcon(BitmapFactory.decodeByteArray(totalData, 0, size));
                    notificationBuilder.setSmallIcon(R.drawable.image_white);

                    Intent notificationIntent = new Intent();
                    notificationIntent.setAction(Intent.ACTION_VIEW);
                    notificationIntent.setDataAndType(fileName, "image/*");
                    notificationIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    PendingIntent contentIntent = PendingIntent.getActivity(
                            mCtx,
                            0,
                            notificationIntent,
                            PendingIntent.FLAG_IMMUTABLE
                    );
                    notificationBuilder.setContentIntent(contentIntent);
                    mDownloading = false;

                    Notification notification = notificationBuilder.build();
                    mNM.notify(NOTIFICATION, notification);
                }
            }
        });
    }

    @Override
    public void sync() {
        mSReceiver = new ScreenshotReqReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("org.asteroidos.sync.SCREENSHOT_REQUEST_LISTENER");
        mCtx.registerReceiver(mSReceiver, filter);

        mDownloading = false;
    }

    @Override
    public void unsync() {
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

    private Uri createFile(byte[] totalData) throws IOException {
        String dirStr = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/AsteroidOSSync";
        Uri uri;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
        File fileName = new File(dirStr + "/Screenshot_" + timeStamp + ".jpg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = mCtx.getContentResolver();
            ContentValues metaInfo = new ContentValues();
            metaInfo.put(MediaStore.MediaColumns.DISPLAY_NAME, "Screenshot_" + timeStamp + ".jpg");
            metaInfo.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            metaInfo.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AsteroidOSSync");
            metaInfo.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
            metaInfo.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI , metaInfo);
            assert imageUri != null;
            OutputStream out = resolver.openOutputStream(imageUri);
            assert out != null;
            try {
                out.write(totalData);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                out.close();
                uri = imageUri;
            }
        } else {
            File directory = new File(dirStr);
            if (!directory.exists())
                directory.mkdirs();

            try {
                FileOutputStream out = new FileOutputStream(fileName);
                out.write(totalData);
                out.close();
                doMediaScan(fileName);
            } catch(IOException e) {
                e.printStackTrace();
            }
            uri = FileProvider.getUriForFile(mCtx, mCtx.getApplicationContext().getPackageName() + ".fileprovider", fileName);
        }
        return uri;
    }

    private void doMediaScan(File file){
        MediaScannerConnection.scanFile(mCtx,
                new String[] { file.toString() }, null,
                (path, uri) -> {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                    Log.i("ExternalStorage", "-> uri=" + uri);
                });
    }

    @Override
    public HashMap<UUID, Direction> getCharacteristicUUIDs() {
        HashMap<UUID, Direction> chars = new HashMap<>();
        chars.put(AsteroidUUIDS.SCREENSHOT_REQUEST, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.SCREENSHOT_CONTENT, Direction.FROM_WATCH);
        return chars;
    }

    @Override
    public UUID getServiceUUID() {
        return AsteroidUUIDS.SCREENSHOT_SERVICE_UUID;
    }

    class ScreenshotReqReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mDownloading) {
                mFirstNotify = true;
                mDownloading = true;
                byte[] data = new byte[1];
                data[0] = 0x0;
                mDevice.send(AsteroidUUIDS.SCREENSHOT_REQUEST, data, ScreenshotService.this);
            }
        }
    }
}

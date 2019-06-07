package com.maxmpz.poweramp.player;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import androidx.annotation.Nullable;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;

public class PowerampAPIHelper {
	private static final String TAG = "PowerampAPIHelper";
	private static final boolean LOG = false;
	

	public static void startPAService(Context context, Intent intent) {
		intent.setComponent(PowerampAPI.PLAYER_SERVICE_COMPONENT_NAME);
		if(Build.VERSION.SDK_INT >= 26) {
			context.startForegroundService(intent);
		} else {
			context.startService(intent);
		}
	}

	// WARNING: openFileDescriptor() will return the original image right from embed track loaded in Poweramp or
	// file cached image. The later is more or less under control in terms of size, though, that can be in-folder user provided image.
	// As for embed album art, the resulting bitmap can be any size. Poweramp has some upper limits on embed album art, still, the decoded image can be very large.
	public static @Nullable
	Bitmap getAlbumArt(Context context, @Nullable Bundle track, int subsampleWidth, int subsampleHeight) {
		if(track == null) {
			if(LOG) Log.e(TAG, "getAlbumArt !track");
			return null;
		}

		Uri aaUri = PowerampAPI.AA_ROOT_URI.buildUpon().appendEncodedPath("files").appendEncodedPath(Long.toString(track.getLong(PowerampAPI.Track.REAL_ID))).build();

		ParcelFileDescriptor pfd = null;

		try {
			pfd = context.getContentResolver().openFileDescriptor(aaUri, "r");
			if(pfd != null) {
				// Get original bitmap size
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inJustDecodeBounds = true;
				BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, opts);
	
				// Calculate subsample and load subsampled image
				opts.inJustDecodeBounds = false;
				if(subsampleWidth > 0 && subsampleHeight > 0) {
					opts.inSampleSize = calcSubsample(subsampleWidth, subsampleHeight, opts.outWidth, opts.outHeight); // Subsamples images up to 2047x2047, should be safe, though this is up to 16mb per bitmap
				}
	
				Bitmap b = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, opts);
				
				if(LOG) Log.e(TAG, "getAlbumArt aaUri=" + aaUri + " b=" + b);
				if(LOG && b != null) Log.e(TAG, "getAlbumArt w=" + b.getWidth() + " h=" + b.getHeight());
				
				return b;
				
			} else if(LOG) Log.e(TAG, "getAlbumArt no pfd for aaUri=" + aaUri);

		} catch(FileNotFoundException ex) {
			// OK
			if(LOG) Log.e(TAG, "getAlbumArt aaUri=" + aaUri, ex);

		} catch(Throwable th) {
			Log.e(TAG, "", th);

		} finally {
			if(pfd != null) {
				try {
					pfd.close();
				} catch(IOException e) {}
			}
		}

		return null;
	}

	// NOTE: maxW/maxH is not actual max, as we just subsample. Output image size will be up to maxW(H)*2 - 1
	private static int calcSubsample(final int maxW, final int maxH, final int outWidth, final int outHeight) {
		int sampleSize = 1;
		int nextWidth = outWidth >> 1;
		int nextHeight = outHeight >> 1;
		while(nextWidth >= maxW && nextHeight >= maxH) {
			sampleSize <<= 1;
			nextWidth >>= 1;
			nextHeight >>= 1;
		}
		return sampleSize;
	}

}

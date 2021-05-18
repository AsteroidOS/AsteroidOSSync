package org.asteroidos.sync.services

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.PhoneLookup
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED
import org.asteroidos.sync.R

class PhoneStateReceiver : BroadcastReceiver() {
	companion object {
		const val PREFS_NAME = "PhoneStatePreference"
		const val PREF_SEND_CALL_STATE = "PhoneCallNotificationForwarding"
	}

	private lateinit var telephony: TelephonyManager

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == ACTION_PHONE_STATE_CHANGED) {
			val callStateService = CallStateService(context)
			telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
			telephony.listen(callStateService, PhoneStateListener.LISTEN_CALL_STATE)
		}
	}

	private class CallStateService(private val context: Context) : PhoneStateListener() {
		private val prefs by lazy {
			context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
		}

		override fun onCallStateChanged(state: Int, incomingNumber: String) {
			when (state) {
				TelephonyManager.CALL_STATE_IDLE -> stopRinging()
				TelephonyManager.CALL_STATE_OFFHOOK -> stopRinging()
				TelephonyManager.CALL_STATE_RINGING -> startRinging(incomingNumber)
			}
		}

		private fun getContact(number: String): String? {
			var contact: String? = null
			context.contentResolver.query(
				Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
				arrayOf(PhoneLookup.DISPLAY_NAME),
				null,
				null,
				null
			)?.use { cursor ->
				if (cursor.moveToFirst())
					contact = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME))
			}
			return contact
		}

		private fun startRinging(number: String) {
			val notificationPref = prefs.getBoolean(PREF_SEND_CALL_STATE, true)
			if (notificationPref) {
				val contact = getContact(number) ?: number
				context.sendBroadcast(Intent("org.asteroidos.sync.NOTIFICATION_LISTENER").apply {
					putExtra("event", "posted")
					putExtra("packageName", "org.asteroidos.generic.dialer")
					putExtra("id", 56345)
					putExtra("appName", context.resources.getString(R.string.dialer))
					putExtra("appIcon", "ios-call")
					putExtra("summary", contact)
					putExtra("body", number)
					putExtra("vibration", "ringtone")
				})
			}
		}

		private fun stopRinging(): Unit =
			context.sendBroadcast(Intent("org.asteroidos.sync.NOTIFICATION_LISTENER").apply {
				putExtra("event", "removed")
				putExtra("id", 56345)
			})
	}
}

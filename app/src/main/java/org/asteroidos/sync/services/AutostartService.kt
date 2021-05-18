package org.asteroidos.sync.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.asteroidos.sync.MainActivity.PREFS_DEFAULT_MAC_ADDR
import org.asteroidos.sync.MainActivity.PREFS_NAME

class AutostartService : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
		context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).let { prefs ->
			val defaultDevMacAddr = prefs.getString(PREFS_DEFAULT_MAC_ADDR, "")!!
			if (defaultDevMacAddr.isNotEmpty())
				context.startService(Intent(context, SynchronizationService::class.java))
		}
	}
}
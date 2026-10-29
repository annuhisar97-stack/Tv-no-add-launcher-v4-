package com.example.noadstvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Requests the launcher activity after boot/update. Android TV manufacturers may
 * restrict background activity launches; setting this app as the default Home
 * app is still required for dependable home-screen startup.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val launch = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                try {
                    context.startActivity(launch)
                } catch (_: Exception) {
                    // Android/TV firmware may block activity launches from background.
                }
            }
        }
    }
}

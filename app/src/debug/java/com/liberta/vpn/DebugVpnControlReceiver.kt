package com.liberta.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.liberta.vpn.data.ConnectionMethod
import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.service.LibertaVpnService

class DebugVpnControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return
        val serviceAction = when (intent.action) {
            ACTION_CONNECT -> LibertaVpnService.ACTION_CONNECT
            ACTION_DISCONNECT -> LibertaVpnService.ACTION_DISCONNECT
            else -> return
        }
        val profile = ConnectionProfile.fromName(intent.getStringExtra(EXTRA_PROFILE))
        val method = ConnectionMethod.fromName(intent.getStringExtra(EXTRA_METHOD)).let { parsed ->
            if (intent.hasExtra(EXTRA_METHOD)) parsed else ConnectionMethod.fromProfile(profile)
        }
        val serviceIntent = Intent(context, LibertaVpnService::class.java).apply {
            action = serviceAction
            putExtra(LibertaVpnService.EXTRA_METHOD, method.name)
            putExtra(LibertaVpnService.EXTRA_PROFILE, profile.name)
            putExtra(LibertaVpnService.EXTRA_FORCE_REFRESH, intent.getBooleanExtra(EXTRA_FORCE_REFRESH, false))
            if (intent.hasExtra(EXTRA_PHANTOM_CALL)) {
                putExtra(LibertaVpnService.EXTRA_PHANTOM_CALL, intent.getBooleanExtra(EXTRA_PHANTOM_CALL, false))
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val ACTION_CONNECT = "com.liberta.vpn.debug.CONNECT"
        const val ACTION_DISCONNECT = "com.liberta.vpn.debug.DISCONNECT"
        const val EXTRA_METHOD = "method"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_FORCE_REFRESH = "force_refresh"
        const val EXTRA_PHANTOM_CALL = "phantom_call"
    }
}

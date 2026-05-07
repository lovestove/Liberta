package com.liberta.vpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.liberta.vpn.LibertaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val app = context.applicationContext as LibertaApplication
                val settings = app.container.settingsRepository.settings.first()
                if (!settings.autoStart || VpnService.prepare(context) != null) return@runCatching
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LibertaVpnService::class.java).apply {
                        action = LibertaVpnService.ACTION_CONNECT
                        putExtra(LibertaVpnService.EXTRA_METHOD, settings.connectionMethod.name)
                        putExtra(LibertaVpnService.EXTRA_PROFILE, settings.profile.name)
                    }
                )
            }.also {
                pending.finish()
            }
        }
    }
}

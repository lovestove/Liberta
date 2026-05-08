package com.liberta.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import com.liberta.vpn.data.ConnectionMethod
import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.service.LibertaVpnService

class DebugVpnPermissionActivity : Activity() {
    private val profile: ConnectionProfile
        get() = ConnectionProfile.fromName(intent.getStringExtra(EXTRA_PROFILE))

    private val method: ConnectionMethod
        get() = ConnectionMethod.fromName(intent.getStringExtra(EXTRA_METHOD)).let { parsed ->
            if (intent.hasExtra(EXTRA_METHOD)) parsed else ConnectionMethod.fromProfile(profile)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            finish()
            return
        }
        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent == null) {
            startVpn()
            finish()
        } else {
            startActivityForResult(permissionIntent, REQUEST_VPN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            startVpn()
        }
        finish()
    }

    private fun startVpn() {
        val serviceIntent = Intent(this, LibertaVpnService::class.java).apply {
            action = LibertaVpnService.ACTION_CONNECT
            putExtra(LibertaVpnService.EXTRA_METHOD, method.name)
            putExtra(LibertaVpnService.EXTRA_PROFILE, profile.name)
            putExtra(LibertaVpnService.EXTRA_FORCE_REFRESH, intent.getBooleanExtra(EXTRA_FORCE_REFRESH, false))
            if (intent.hasExtra(EXTRA_PHANTOM_CALL)) {
                putExtra(LibertaVpnService.EXTRA_PHANTOM_CALL, intent.getBooleanExtra(EXTRA_PHANTOM_CALL, false))
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    companion object {
        const val EXTRA_METHOD = "method"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_FORCE_REFRESH = "force_refresh"
        const val EXTRA_PHANTOM_CALL = "phantom_call"
        private const val REQUEST_VPN = 2001
    }
}

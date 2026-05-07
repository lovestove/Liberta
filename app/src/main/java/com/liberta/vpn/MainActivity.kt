package com.liberta.vpn

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liberta.vpn.data.ConnectionMethod
import com.liberta.vpn.ui.LibertaApp
import com.liberta.vpn.viewmodel.LibertaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LibertaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val status by viewModel.status.collectAsStateWithLifecycle()
            var launchRefreshDone by rememberSaveable { mutableStateOf(false) }
            var pendingMethod by rememberSaveable { mutableStateOf(settings.connectionMethod.name) }
            var pendingPhantomCall by rememberSaveable { mutableStateOf(settings.labs.phantomCall) }
            var pendingMeshAccess by rememberSaveable { mutableStateOf(settings.connectionMethod == ConnectionMethod.MESH_ACCESS) }
            var pendingSovereignRelay by rememberSaveable { mutableStateOf(settings.labs.sovereignRelay) }
            val vpnPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                val method = ConnectionMethod.fromName(pendingMethod)
                viewModel.connect(
                    this,
                    method,
                    phantomCall = pendingPhantomCall,
                    meshAccess = pendingMeshAccess,
                    sovereignRelay = pendingSovereignRelay
                )
            }
            val notificationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) {}

            LaunchedEffect(settings.autoRefreshOnLaunch) {
                if (settings.autoRefreshOnLaunch && !launchRefreshDone) {
                    launchRefreshDone = true
                    viewModel.refreshAll(forceRefresh = false)
                }
            }

            LibertaApp(
                settings = settings,
                status = status,
                onPower = {
                    if (status.isConnected || status.isBusy) {
                        viewModel.disconnect(this)
                    } else {
                        val method = settings.connectionMethod
                        pendingMethod = method.name
                        pendingPhantomCall = method == ConnectionMethod.PHANTOM_CALL
                        pendingMeshAccess = method == ConnectionMethod.MESH_ACCESS
                        pendingSovereignRelay = settings.labs.sovereignRelay
                        startVpnWithPermission(
                            notificationLauncher = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            permissionLauncher = { vpnPermissionLauncher.launch(it) },
                            onReady = {
                                viewModel.connect(
                                    this,
                                    method,
                                    phantomCall = method == ConnectionMethod.PHANTOM_CALL,
                                    meshAccess = method == ConnectionMethod.MESH_ACCESS,
                                    sovereignRelay = settings.labs.sovereignRelay
                                )
                            }
                        )
                    }
                },
                onConnectionModePower = { method ->
                    if (!status.isConnected && !status.isBusy) {
                        pendingMethod = method.name
                        pendingPhantomCall = method == ConnectionMethod.PHANTOM_CALL
                        pendingMeshAccess = method == ConnectionMethod.MESH_ACCESS
                        pendingSovereignRelay = settings.labs.sovereignRelay
                        viewModel.updateSettings { current ->
                            current.copy(
                                connectionMethod = method,
                                profile = method.profile
                            )
                        }
                        startVpnWithPermission(
                            notificationLauncher = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            permissionLauncher = { vpnPermissionLauncher.launch(it) },
                            onReady = {
                                viewModel.connect(
                                    this,
                                    method,
                                    phantomCall = method == ConnectionMethod.PHANTOM_CALL,
                                    meshAccess = method == ConnectionMethod.MESH_ACCESS,
                                    sovereignRelay = settings.labs.sovereignRelay
                                )
                            }
                        )
                    }
                },
                onRecover = { viewModel.recover(this) },
                onRefresh = { viewModel.refreshAll(forceRefresh = true) },
                onShareApp = { shareLatestRelease() },
                onAutoRefreshChange = viewModel::setAutoRefresh,
                onAutoRefreshOnLaunchChange = viewModel::setAutoRefreshOnLaunch,
                onAutoRefreshIntervalChange = viewModel::setAutoRefreshIntervalMinutes,
                onAutoStartChange = viewModel::setAutoStart,
                onDnsProviderChange = viewModel::setDnsProvider,
                onMtuChange = viewModel::setMtu,
                onSettingsChange = viewModel::updateSettings,
                onLabsChange = viewModel::updateLabs
            )
        }
    }

    private fun startVpnWithPermission(
        notificationLauncher: () -> Unit,
        permissionLauncher: (Intent) -> Unit,
        onReady: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher()
        }
        val permissionIntent: Intent? = VpnService.prepare(this)
        android.util.Log.i("LibertaPermission", "VpnService.prepare() returned: $permissionIntent")
        if (permissionIntent != null) {
            permissionLauncher(permissionIntent)
        } else {
            onReady()
        }
    }

    private fun shareLatestRelease() {
        val text = "Liberta для Android: ${BuildConfig.GITHUB_REPO_URL}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Liberta")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Поделиться Liberta"))
    }
}

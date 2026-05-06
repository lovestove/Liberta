package com.liberta.vpn.service

import com.liberta.vpn.data.ConnectionPhase
import com.liberta.vpn.data.ConnectionMethod
import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.data.ServerCandidate
import com.liberta.vpn.data.VpnStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object LibertaRuntime {
    private val mutableStatus = MutableStateFlow(VpnStatus())
    val status: StateFlow<VpnStatus> = mutableStatus

    fun update(
        phase: ConnectionPhase,
        connectionMethod: ConnectionMethod = mutableStatus.value.connectionMethod,
        profile: ConnectionProfile = mutableStatus.value.profile,
        message: String,
        activeServer: ServerCandidate? = mutableStatus.value.activeServer,
        lastUpdatedEpochMs: Long? = mutableStatus.value.lastUpdatedEpochMs,
        trafficPulse: Float = mutableStatus.value.trafficPulse,
        helpedUsers: Int = mutableStatus.value.helpedUsers,
        error: String? = null
    ) {
        mutableStatus.value = VpnStatus(
            phase = phase,
            connectionMethod = connectionMethod,
            profile = profile,
            message = message,
            activeServer = activeServer,
            lastUpdatedEpochMs = lastUpdatedEpochMs,
            trafficPulse = trafficPulse,
            helpedUsers = helpedUsers,
            error = error
        )
    }

    fun pulse(value: Float) {
        mutableStatus.update { it.copy(trafficPulse = value.coerceIn(0f, 1f)) }
    }
}

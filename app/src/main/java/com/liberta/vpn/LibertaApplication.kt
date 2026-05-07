package com.liberta.vpn

import android.app.Application
import com.liberta.vpn.core.LibboxCoreEngine
import com.liberta.vpn.core.SingBoxConfigBuilder
import com.liberta.vpn.data.ServerRacer
import com.liberta.vpn.data.SettingsRepository
import com.liberta.vpn.data.SubscriptionRepository
import com.liberta.vpn.data.WorkingServersRepository
import com.liberta.vpn.service.PhantomCallCoordinator

class LibertaApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val settingsRepository = SettingsRepository(application)
    val subscriptionRepository = SubscriptionRepository(application)
    val workingServersRepository = WorkingServersRepository(application)
    val serverRacer = ServerRacer()
    val configBuilder = SingBoxConfigBuilder()
    val coreEngine = LibboxCoreEngine()
    val phantomCallCoordinator = PhantomCallCoordinator()
}

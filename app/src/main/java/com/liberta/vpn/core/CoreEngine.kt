package com.liberta.vpn.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.Inet4Address
import java.net.NetworkInterface

data class CoreResult(
    val ok: Boolean,
    val message: String,
    val version: String? = null
) {
    companion object {
        fun ok(message: String, version: String? = null) = CoreResult(true, message, version)
        fun error(message: String, version: String? = null) = CoreResult(false, message, version)
    }
}

interface CoreEngine {
    val isAvailable: Boolean
    fun version(): String?
    suspend fun validateConfig(configJson: String): CoreResult
    suspend fun start(configJson: String, tunFd: Int, cacheDir: File, protect: (Int) -> Boolean): CoreResult
    suspend fun stop(): CoreResult
}

class LibboxCoreEngine : CoreEngine {
    private var service: Any? = null
    private var setupRoot: String? = null

    private val libboxClass: Class<*>? by lazy {
        listOf(
            "io.nekohasekai.libbox.Libbox",
            "libbox.Libbox",
            "Libbox"
        ).firstNotNullOfOrNull { name -> runCatching { Class.forName(name) }.getOrNull() }
    }

    override val isAvailable: Boolean
        get() = libboxClass != null

    override fun version(): String? =
        libboxClass?.findMethod("version", "Version", parameterCount = 0)?.let { method ->
            runCatching { method.invoke(null)?.toString() }.getOrNull()
        }

    override suspend fun validateConfig(configJson: String): CoreResult = withContext(Dispatchers.IO) {
        val clazz = libboxClass ?: return@withContext CoreResult.error(
            "libbox.aar не найден. Выполните tools\\build_libbox.ps1 и пересоберите APK."
        )
        val method = clazz.findMethod("checkConfig", "CheckConfig", parameterCount = 1)
            ?: return@withContext CoreResult.error("В libbox отсутствует CheckConfig(config)")
        runCatching {
            method.invoke(null, configJson)
            CoreResult.ok("Конфигурация sing-box валидна", version())
        }.getOrElse { CoreResult.error("sing-box config rejected: ${it.rootMessage()}", version()) }
    }

    override suspend fun start(
        configJson: String,
        tunFd: Int,
        cacheDir: File,
        protect: (Int) -> Boolean
    ): CoreResult = withContext(Dispatchers.IO) {
        val clazz = libboxClass ?: return@withContext CoreResult.error(
            "libbox.aar не найден. VPN не запущен, статус Connected не будет выставлен."
        )
        stop()
        val platformInterfaceClass = runCatching {
            Class.forName("${clazz.`package`?.name}.PlatformInterface")
        }.getOrNull() ?: return@withContext CoreResult.error("Не найден PlatformInterface в libbox")

        val platform = Proxy.newProxyInstance(
            platformInterfaceClass.classLoader,
            arrayOf(platformInterfaceClass),
            PlatformInvocationHandler(tunFd, protect)
        )
        val commandServerHandlerClass = runCatching {
            Class.forName("${clazz.`package`?.name}.CommandServerHandler")
        }.getOrNull() ?: return@withContext CoreResult.error("Не найден CommandServerHandler в libbox")
        val overrideOptionsClass = runCatching {
            Class.forName("${clazz.`package`?.name}.OverrideOptions")
        }.getOrNull() ?: return@withContext CoreResult.error("Не найден OverrideOptions в libbox")
        val handler = Proxy.newProxyInstance(
            commandServerHandlerClass.classLoader,
            arrayOf(commandServerHandlerClass),
            CommandServerInvocationHandler
        )
        val newCommandServer = clazz.methods.firstOrNull {
            it.name == "newCommandServer" && it.parameterTypes.size == 2
        } ?: return@withContext CoreResult.error("В libbox отсутствует newCommandServer(handler, platform)")

        runCatching {
            setupIfNeeded(clazz, cacheDir)
            val commandServer = newCommandServer.invoke(null, handler, platform)
            val startMethod = commandServer.javaClass.findMethod("start", parameterCount = 0)
                ?: error("CommandServer.start() not found")
            val startOrReloadMethod = commandServer.javaClass.findMethod("startOrReloadService", parameterCount = 2)
                ?: error("CommandServer.startOrReloadService(config, options) not found")
            val overrideOptions = overrideOptionsClass.getDeclaredConstructor().newInstance()
            overrideOptions.javaClass.findMethod("setAutoRedirect", parameterCount = 1)?.invoke(overrideOptions, false)
            startMethod.invoke(commandServer)
            startOrReloadMethod.invoke(commandServer, configJson, overrideOptions)
            service = commandServer
            CoreResult.ok("libbox service started", version())
        }.getOrElse { CoreResult.error("libbox start failed: ${it.rootMessage()}", version()) }
    }

    override suspend fun stop(): CoreResult = withContext(Dispatchers.IO) {
        val current = service ?: return@withContext CoreResult.ok("libbox already stopped", version())
        runCatching {
            current.javaClass.findMethod("closeService", parameterCount = 0)?.invoke(current)
            current.javaClass.findMethod("close", "Close", parameterCount = 0)?.invoke(current)
            service = null
            CoreResult.ok("libbox stopped", version())
        }.getOrElse {
            service = null
            CoreResult.error("libbox stop failed: ${it.rootMessage()}", version())
        }
    }

    private fun setupIfNeeded(clazz: Class<*>, cacheDir: File) {
        val root = cacheDir.absolutePath
        if (setupRoot == root) return
        val setupOptionsClass = Class.forName("${clazz.`package`?.name}.SetupOptions")
        val options = setupOptionsClass.getDeclaredConstructor().newInstance()
        val workingDir = File(cacheDir, "libbox").apply { mkdirs() }
        val tempDir = File(cacheDir, "libbox-temp").apply { mkdirs() }
        options.javaClass.findMethod("setBasePath", parameterCount = 1)?.invoke(options, workingDir.absolutePath)
        options.javaClass.findMethod("setWorkingPath", parameterCount = 1)?.invoke(options, workingDir.absolutePath)
        options.javaClass.findMethod("setTempPath", parameterCount = 1)?.invoke(options, tempDir.absolutePath)
        options.javaClass.findMethod("setFixAndroidStack", parameterCount = 1)?.invoke(options, true)
        options.javaClass.findMethod("setCommandServerListenPort", parameterCount = 1)?.invoke(options, 0)
        options.javaClass.findMethod("setCommandServerSecret", parameterCount = 1)?.invoke(options, "liberta")
        options.javaClass.findMethod("setLogMaxLines", parameterCount = 1)?.invoke(options, 300L)
        options.javaClass.findMethod("setDebug", parameterCount = 1)?.invoke(options, true)
        val setup = clazz.findMethod("setup", parameterCount = 1)
            ?: error("Libbox.setup(options) not found")
        setup.invoke(null, options)
        setupRoot = root
    }

    private object CommandServerInvocationHandler : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
            return when (method.name) {
                "writeDebugMessage" -> {
                    Log.d("LibertaCore", args?.firstOrNull()?.toString().orEmpty())
                    null
                }
                "serviceReload", "serviceStop", "setSystemProxyEnabled" -> null
                "getSystemProxyStatus" -> null
                "toString" -> "LibertaCommandServerHandler"
                else -> defaultValue(method.returnType)
            }
        }
    }

    private class PlatformInvocationHandler(
        private val tunFd: Int,
        private val protect: (Int) -> Boolean
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
            return when (method.name) {
                "OpenTun", "openTun" -> tunFd
                "UsePlatformAutoDetectInterfaceControl", "usePlatformAutoDetectInterfaceControl" -> true
                "AutoDetectInterfaceControl", "autoDetectInterfaceControl" -> {
                    val fd = (args?.firstOrNull() as? Number)?.toInt() ?: return null
                    Log.i("LibertaCore", "protecting fd=$fd")
                    if (!protect(fd)) error("VpnService.protect($fd) failed")
                    null
                }
                "WriteLog", "writeLog" -> {
                    Log.i("LibertaCore", args?.firstOrNull()?.toString().orEmpty())
                    null
                }
                "UseProcFS", "useProcFS" -> false
                "UnderNetworkExtension", "underNetworkExtension" -> false
                "IncludeAllNetworks", "includeAllNetworks" -> false
                "ClearDNSCache", "clearDNSCache" -> null
                "SendNotification", "sendNotification" -> null
                "StartDefaultInterfaceMonitor", "startDefaultInterfaceMonitor" -> {
                    notifyDefaultInterface(args?.firstOrNull())
                    null
                }
                "CloseDefaultInterfaceMonitor", "closeDefaultInterfaceMonitor" -> null
                "FindConnectionOwner", "findConnectionOwner" -> method.returnType.newConnectionOwner()
                "PackageNameByUid", "packageNameByUid" -> ""
                "UIDByPackageName", "uidByPackageName" -> 0
                "ReadWIFIState", "readWIFIState" -> null
                "SystemCertificates", "systemCertificates" -> null
                "LocalDNSTransport", "localDNSTransport" -> null
                "GetInterfaces", "getInterfaces" -> method.returnType.newNetworkInterfaceIterator()
                "toString" -> "LibertaPlatformInterface"
                else -> defaultValue(method.returnType)
            }
        }

        private fun notifyDefaultInterface(listener: Any?) {
            val selected = currentInterfaces().firstOrNull() ?: return
            runCatching {
                listener?.javaClass?.methods?.firstOrNull { method ->
                    method.name == "updateDefaultInterface" && method.parameterTypes.size == 4
                }?.invoke(
                    listener,
                    selected.name,
                    selected.index,
                    selected.interfaceType() == INTERFACE_TYPE_CELLULAR,
                    false
                )
                Log.i("LibertaCore", "default interface=${selected.name} index=${selected.index}")
            }.onFailure { error ->
                Log.w("LibertaCore", "default interface notify failed: ${error.rootMessage()}")
            }
        }

        private fun Class<*>.newNetworkInterfaceIterator(): Any {
            val packageName = `package`?.name ?: return newNetworkInterfaceIterator(emptyList())
            val interfaceClass = Class.forName("$packageName.NetworkInterface")
            val interfaces = currentInterfaces()
            val values = interfaces.mapNotNull { it.toLibboxInterface(packageName, interfaceClass) }
            val names = interfaces.joinToString { "${it.name}/${it.index}/flags=${it.unixLinkFlags()}" }
            Log.i("LibertaCore", "platform interfaces=${values.size} [$names]")
            return newNetworkInterfaceIterator(values)
        }

        private fun Class<*>.newNetworkInterfaceIterator(values: List<Any>): Any =
            Proxy.newProxyInstance(
                classLoader,
                arrayOf(this),
                NetworkInterfaceIteratorInvocationHandler(values)
            )

        private fun NetworkInterface.toLibboxInterface(packageName: String, interfaceClass: Class<*>): Any? =
            runCatching {
                val target = interfaceClass.getDeclaredConstructor().newInstance()
                val addresses = inetAddresses.toList()
                    .filterIsInstance<Inet4Address>()
                    .mapNotNull { address -> address.hostAddress?.let { "$it/32" } }
                interfaceClass.findMethod("setName", parameterCount = 1)?.invoke(target, name)
                interfaceClass.findMethod("setIndex", parameterCount = 1)?.invoke(target, index)
                interfaceClass.findMethod("setMTU", parameterCount = 1)?.invoke(target, mtu)
                interfaceClass.findMethod("setFlags", parameterCount = 1)?.invoke(target, unixLinkFlags())
                interfaceClass.findMethod("setType", parameterCount = 1)?.invoke(target, interfaceType())
                interfaceClass.findMethod("setMetered", parameterCount = 1)
                    ?.invoke(target, interfaceType() == INTERFACE_TYPE_CELLULAR)
                interfaceClass.findMethod("setAddresses", parameterCount = 1)?.invoke(
                    target,
                    Class.forName("$packageName.StringIterator").newStringIterator(addresses)
                )
                interfaceClass.findMethod("setDNSServer", parameterCount = 1)?.invoke(
                    target,
                    Class.forName("$packageName.StringIterator").newStringIterator(emptyList())
                )
                target
            }.getOrElse { error ->
                Log.w("LibertaCore", "skip interface $name: ${error.rootMessage()}")
                null
            }

        private fun Class<*>.newStringIterator(values: List<String>): Any =
            Proxy.newProxyInstance(
                classLoader,
                arrayOf(this),
                StringIteratorInvocationHandler(values)
            )

        private fun currentInterfaces(): List<NetworkInterface> =
            NetworkInterface.getNetworkInterfaces()
                ?.toList()
                .orEmpty()
                .filter { it.isUp && !it.isLoopback && !it.name.startsWith("tun") }
                .sortedWith(compareBy<NetworkInterface> { if (it.interfaceType() == INTERFACE_TYPE_CELLULAR) 0 else 1 }.thenBy { it.index })

        private fun NetworkInterface.interfaceType(): Int =
            when {
                name.startsWith("wlan", ignoreCase = true) -> INTERFACE_TYPE_WIFI
                name.startsWith("ccmni", ignoreCase = true) ||
                    name.startsWith("rmnet", ignoreCase = true) ||
                    name.startsWith("pdp", ignoreCase = true) ||
                    name.startsWith("wwan", ignoreCase = true) -> INTERFACE_TYPE_CELLULAR
                name.startsWith("eth", ignoreCase = true) -> INTERFACE_TYPE_ETHERNET
                else -> INTERFACE_TYPE_OTHER
            }

        private fun NetworkInterface.unixLinkFlags(): Int {
            var flags = IFF_UP or IFF_RUNNING
            if (!isLoopback && !isPointToPoint) flags = flags or IFF_BROADCAST
            if (isLoopback) flags = flags or IFF_LOOPBACK
            if (isPointToPoint) flags = flags or IFF_POINTOPOINT
            if (runCatching { supportsMulticast() }.getOrDefault(false)) flags = flags or IFF_MULTICAST
            return flags
        }

        private companion object {
            private const val INTERFACE_TYPE_WIFI = 0
            private const val INTERFACE_TYPE_CELLULAR = 1
            private const val INTERFACE_TYPE_ETHERNET = 2
            private const val INTERFACE_TYPE_OTHER = 3

            private const val IFF_UP = 0x1
            private const val IFF_BROADCAST = 0x2
            private const val IFF_LOOPBACK = 0x8
            private const val IFF_POINTOPOINT = 0x10
            private const val IFF_RUNNING = 0x40
            private const val IFF_MULTICAST = 0x1000
        }
    }
}

private class NetworkInterfaceIteratorInvocationHandler(
    private val values: List<Any>
) : InvocationHandler {
    private var index = 0

    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? =
        when (method.name) {
            "hasNext" -> index < values.size
            "next" -> values[index++]
            "toString" -> "LibertaNetworkInterfaceIterator(size=${values.size})"
            else -> defaultValue(method.returnType)
        }
}

private class StringIteratorInvocationHandler(
    private val values: List<String>
) : InvocationHandler {
    private var index = 0

    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? =
        when (method.name) {
            "hasNext" -> index < values.size
            "len" -> values.size
            "next" -> values[index++]
            "toString" -> values.joinToString(prefix = "[", postfix = "]")
            else -> defaultValue(method.returnType)
        }
}

private fun Class<*>.findMethod(vararg names: String, parameterCount: Int): Method? =
    methods.firstOrNull { method ->
        method.parameterTypes.size == parameterCount && names.any { method.name == it }
    }

private fun defaultValue(type: Class<*>): Any? = when {
    type == java.lang.Boolean.TYPE -> false
    type == java.lang.Integer.TYPE -> 0
    type == java.lang.Long.TYPE -> 0L
    type == java.lang.Float.TYPE -> 0f
    type == java.lang.Double.TYPE -> 0.0
    type == java.lang.Void.TYPE -> null
    else -> null
}

private fun Class<*>.newConnectionOwner(): Any {
    val owner = getDeclaredConstructor().newInstance()
    owner.javaClass.findMethod("setUserId", parameterCount = 1)?.invoke(owner, -1)
    owner.javaClass.findMethod("setUserName", parameterCount = 1)?.invoke(owner, "")
    owner.javaClass.findMethod("setProcessPath", parameterCount = 1)?.invoke(owner, "")
    return owner
}

private fun Throwable.rootMessage(): String {
    var root: Throwable = this
    while (root.cause != null) root = root.cause!!
    return root.message ?: root.javaClass.simpleName
}

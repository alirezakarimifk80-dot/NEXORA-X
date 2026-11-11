package com.nexora.x.core.engine.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.nexora.x.core.engine.EngineException
import com.nexora.x.core.engine.EngineState
import com.nexora.x.core.engine.EngineTrafficStatistics
import com.nexora.x.core.engine.NexoraTunPlatformCallbacks
import com.nexora.x.core.engine.SingBoxEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Native Android [VpnService] hosting the `sing-box` (`libbox`) engine and TUN file descriptor.
 *
 * Lifecycle, Routing & File-Descriptor Ownership Guarantees:
 * - Declared non-exported (`android:exported="false"`) with `android.permission.BIND_VPN_SERVICE`.
 * - Establishes the virtual TUN interface via `VpnService.Builder` in non-blocking mode (`setBlocking(false)`)
 *   required by Go's runtime netpoller inside `libbox.so`.
 * - Configures IPv4 address (`172.19.0.1/30`), IPv4 default route (`0.0.0.0/0`), and DNS servers
 *   (`1.1.1.1` and TUN peer `172.19.0.2`), and prevents routing loops by excluding `packageName`
 *   (`addDisallowedApplication(packageName)`) and protecting outbound sockets via `protect(fd)`.
 * - Duplicates the TUN file descriptor via `ParcelFileDescriptor.dup(pfd.fileDescriptor).detachFd()`
 *   before handing it to `libbox` so `BoxService.close()` owns its fd cleanly without double-close races.
 * - Executes Phase 4 Stages D, E, and F connectivity probes after `libbox` starts and updates the
 *   foreground notification to reflect `VPN_ACTIVE`, `CONNECTIVITY_CHECK`, `INTERNET_AVAILABLE`, or `NO_INTERNET`.
 */
@AndroidEntryPoint
class NexoraVpnService : VpnService(), NexoraTunPlatformCallbacks {

    companion object {
        const val ACTION_START_VPN = "com.nexora.x.action.START_VPN"
        const val ACTION_STOP_VPN = "com.nexora.x.action.STOP_VPN"
    }

    @Inject
    lateinit var singBoxEngine: SingBoxEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var tunDescriptor: ParcelFileDescriptor? = null

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        VpnNotificationManager.ensureNotificationChannel(this)
        registerUnderlyingNetworkMonitor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_VPN -> {
                stopVpnSessionAndService("Stopped from notification or user control")
                return START_NOT_STICKY
            }
            ACTION_START_VPN, null -> {
                promoteToForeground(EngineState.STARTING)
                serviceScope.launch {
                    try {
                        singBoxEngine.startPreparedSessionInService(this@NexoraVpnService)
                        promoteToForeground(singBoxEngine.engineState.value)

                        // Phase 4: Run real Stages D, E, and F connectivity verification
                        singBoxEngine.verifyActiveTunnelConnectivity { outboundSocket ->
                            protect(outboundSocket)
                        }
                        promoteToForeground(singBoxEngine.engineState.value)
                    } catch (_: Throwable) {
                        closeTunDescriptorSafely()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
                return START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    /**
     * Called by the Android system when another VPN app preempts NEXORA X or the user revokes
     * VPN consent from system settings.
     */
    override fun onRevoke() {
        stopVpnSessionAndService("VPN permission revoked or preempted by another active VPN")
        super.onRevoke()
    }

    override fun onDestroy() {
        unregisterUnderlyingNetworkMonitor()
        closeTunDescriptorSafely()
        singBoxEngine.stopSession("Android VpnService destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun protectSocketFd(fd: Int): Boolean {
        return protect(fd)
    }

    @Synchronized
    override fun openTunInterface(
        mtu: Int,
        ipv4Address: String,
        ipv4Prefix: Int,
        dnsAddress: String
    ): Int {
        closeTunDescriptorSafely()

        val safeMtu = mtu.coerceIn(1280, 9000)
        val builder = Builder()
            .setSession("NEXORA X")
            .setMtu(safeMtu)
            .setBlocking(false)
            .addAddress(ipv4Address, ipv4Prefix)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(dnsAddress)

        if (ipv4Address == "172.19.0.1" && dnsAddress != "172.19.0.2") {
            runCatching { builder.addDnsServer("172.19.0.2") }
        }

        // Prevent routing loops by excluding NEXORA X's own process from the TUN capture
        runCatching {
            builder.addDisallowedApplication(packageName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        val pfd = builder.establish()
            ?: throw EngineException.TunEstablishmentException(
                "VpnService.Builder.establish() returned null (VPN permission may have been revoked or another VPN is locked)."
            )

        tunDescriptor = pfd

        // Duplicate a dedicated file descriptor for libbox so Go's BoxService.close() owns its fd
        // without double-closing NexoraVpnService's ParcelFileDescriptor.
        val libboxFd = try {
            ParcelFileDescriptor.dup(pfd.fileDescriptor).detachFd()
        } catch (_: Throwable) {
            pfd.fd
        }

        singBoxEngine.onTunInterfaceOpened(
            fd = libboxFd,
            mtu = safeMtu,
            ipv4Address = ipv4Address,
            ipv4Prefix = ipv4Prefix
        )
        return libboxFd
    }

    override fun onNativeCoreLog(message: String) {
        singBoxEngine.recordDiagnosticLog(message)
    }

    override fun onTrafficStatusUpdate(stats: EngineTrafficStatistics) {
        singBoxEngine.onTrafficStatisticsReceived(stats)
        val diag = singBoxEngine.diagnostics.value
        val notification = VpnNotificationManager.buildForegroundNotification(
            context = this,
            state = diag.currentVpnState,
            profileName = diag.activeProfileName,
            traffic = stats
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(VpnNotificationManager.NOTIFICATION_ID, notification)
    }

    private fun promoteToForeground(state: EngineState) {
        val diag = singBoxEngine.diagnostics.value
        val notification = VpnNotificationManager.buildForegroundNotification(
            context = this,
            state = state,
            profileName = diag.activeProfileName,
            traffic = diag.trafficStats
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                VpnNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(VpnNotificationManager.NOTIFICATION_ID, notification)
        }
    }

    private fun stopVpnSessionAndService(reason: String) {
        closeTunDescriptorSafely()
        singBoxEngine.stopSession(reason)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Synchronized
    private fun closeTunDescriptorSafely() {
        val pfd = tunDescriptor ?: return
        tunDescriptor = null
        runCatching { pfd.close() }
    }

    private fun registerUnderlyingNetworkMonitor() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        connectivityManager = cm
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    runCatching { setUnderlyingNetworks(arrayOf(network)) }
                }
                singBoxEngine.onNetworkStateChanged(true, "Physical network available")
            }

            override fun onLost(network: Network) {
                singBoxEngine.onNetworkStateChanged(false, "Physical network lost")
            }
        }
        networkCallback = callback
        runCatching { cm.registerNetworkCallback(request, callback) }
    }

    private fun unregisterUnderlyingNetworkMonitor() {
        val cm = connectivityManager ?: return
        val cb = networkCallback ?: return
        networkCallback = null
        runCatching { cm.unregisterNetworkCallback(cb) }
    }
}

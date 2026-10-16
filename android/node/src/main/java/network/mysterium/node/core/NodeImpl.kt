package network.mysterium.node.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import network.mysterium.node.Node
import network.mysterium.node.Storage
import network.mysterium.node.data.NodeServiceDataSource
import network.mysterium.node.model.NodeConfig
import network.mysterium.node.model.NodeIdentity
import network.mysterium.node.model.NodeServiceType
import network.mysterium.node.model.NodeStatus
import network.mysterium.node.model.NodeTerms
import network.mysterium.terms.Terms
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class NodeImpl(
    private val context: Context,
    private val storage: Storage,
    private val dataSource: NodeServiceDataSource,
    private val nodeContainer: NodeContainer,
) : Node {

    private companion object {
        val TAG: String = NodeImpl::class.java.simpleName
    }

    private var serviceConnection: ServiceConnection? = null

    override val terms: NodeTerms
        get() = NodeTerms(Terms.endUserMD(), Terms.version())

    override val nodeUIUrl: String = "http://localhost:4449"

    override val isRegistered: Boolean
        get() = storage.isRegistered

    override val config: NodeConfig
        get() = storage.config

    override val services: StateFlow<List<NodeServiceType>>
        get() = dataSource.services

    override val balance: StateFlow<Double>
        get() = dataSource.balance

    override val identity: StateFlow<NodeIdentity>
        get() = dataSource.identity

    override val limitMonitor: StateFlow<Boolean>
        get() = dataSource.limitMonitor

    override val status: StateFlow<NodeStatus>
        get() = dataSource.status

    override suspend fun updateConfig(config: NodeConfig) {
        storage.config = config
        service?.updateServices()
    }

    private var service: NodeServiceBinder? = null

    override suspend fun start() {
        if (service != null) return
        // Start the service in addition to binding, so it has an independent
        // lifecycle anchor that survives task removal and UI unbinding.
        // The service puts itself into the foreground in onCreate().
        val service = startService()
        service.start()
        this.service = service
    }

    override suspend fun enableForegroundService() {
        service?.startForegroundService()
        service?.updateServices()
    }

    override fun disableForegroundService() {
        service?.stopForegroundService()
    }

    override suspend fun restartServices() {
        service?.restartServices()
    }

    override suspend fun stop() {
        service?.stop()
        disableForegroundService()
        serviceConnection?.let { context.unbindService(it) }
        // Explicit user shutdown — also stop the started service so the system
        // doesn't consider this a crash and doesn't attempt a STICKY restart.
        context.stopService(Intent(context, NodeService::class.java))
        service = null
        serviceConnection = null
        // The mobile node has been shut down — drop the cached instance so a
        // relaunch creates a fresh one instead of reusing the dead node.
        nodeContainer.reset()
        dataSource.updateStatus(NodeStatus.OFFLINE)
    }

    private suspend fun startService(): NodeServiceBinder = suspendCancellableCoroutine { continuation ->
        val intent = Intent(context, NodeService::class.java)
        ContextCompat.startForegroundService(context, intent)

        // Drop a connection left over from an earlier failed attempt: it would still
        // be bound, and its callbacks would belong to a continuation that is gone.
        serviceConnection?.let { stale -> runCatching { context.unbindService(stale) } }

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                // The system can call this again after a reconnect; only the first
                // call may resume the continuation (resuming twice crashes).
                if (!continuation.isActive) return

                val service = binder as? NodeServiceBinder

                if (service == null) {
                    continuation.resumeWithException(IllegalStateException("Unable to create service"))
                    return
                }
                continuation.resume(service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // The service died unexpectedly (process kill) — the STICKY
                // restart will bring it back; nothing to do here.
            }
        }
        serviceConnection = connection

        val didBind = context.bindService(
            intent,
            connection,
            Context.BIND_AUTO_CREATE
        )
        if (!didBind) {
            // Even a failed bind has to be released with unbindService().
            runCatching { context.unbindService(connection) }
            serviceConnection = null
            continuation.resumeWithException(IllegalStateException("Unable to bind NodeService"))
            return@suspendCancellableCoroutine
        }

        continuation.invokeOnCancellation {
            runCatching { context.unbindService(connection) }
            if (serviceConnection === connection) serviceConnection = null
        }
    }
}

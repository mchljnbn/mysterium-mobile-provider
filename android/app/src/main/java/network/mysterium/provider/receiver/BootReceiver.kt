package network.mysterium.provider.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import network.mysterium.node.Storage
import network.mysterium.node.core.NodeService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Restarts the node foreground service after the device reboots — but only if
 * the node was running before the reboot (i.e. the user did not explicitly
 * shut it down).
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val storage: Storage by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!storage.shouldRun) return

        // This is a background start; the battery-optimization exemption
        // (requested during onboarding) allows it.
        ContextCompat.startForegroundService(
            context,
            Intent(context, NodeService::class.java)
        )
    }
}

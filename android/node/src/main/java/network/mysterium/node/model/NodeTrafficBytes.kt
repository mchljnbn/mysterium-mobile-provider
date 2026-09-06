package network.mysterium.node.model

/**
 * Cumulative traffic counters for the node, in bytes.
 */
data class NodeTrafficBytes(
    val bytesReceived: Long,
    val bytesSent: Long
) {
    companion object {
        fun empty() = NodeTrafficBytes(bytesReceived = 0, bytesSent = 0)
    }
}

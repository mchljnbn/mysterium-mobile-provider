package network.mysterium.provider.ui.screens.home.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import network.mysterium.node.model.NodeTrafficBytes
import network.mysterium.provider.ui.theme.Colors
import network.mysterium.provider.ui.theme.Corners
import network.mysterium.provider.ui.theme.Paddings
import network.mysterium.provider.ui.theme.TextStyles
import network.mysterium.provider.utils.formatBytes

@Composable
fun TrafficItem(
    modifier: Modifier = Modifier,
    traffic: NodeTrafficBytes
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Colors.balanceBg,
                shape = RoundedCornerShape(Corners.default)
            )
            .padding(Paddings.card),
        verticalArrangement = Arrangement.spacedBy(Paddings.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "↓ Download",
                style = TextStyles.body3Bold,
                color = Colors.blue700
            )
            Text(
                modifier = Modifier.weight(1f),
                text = formatBytes(traffic.bytesReceived),
                style = TextStyles.body3Bold,
                color = Colors.blue700,
                textAlign = TextAlign.End
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "↑ Upload",
                style = TextStyles.body3Bold,
                color = Colors.blue700
            )
            Text(
                modifier = Modifier.weight(1f),
                text = formatBytes(traffic.bytesSent),
                style = TextStyles.body3Bold,
                color = Colors.blue700,
                textAlign = TextAlign.End
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrafficItemPreview() {
    Column {
        TrafficItem(traffic = NodeTrafficBytes(bytesReceived = 0, bytesSent = 0))
        TrafficItem(traffic = NodeTrafficBytes(bytesReceived = 1_048_576, bytesSent = 52_428_800))
        TrafficItem(traffic = NodeTrafficBytes(bytesReceived = 3_221_225_472, bytesSent = 8_589_934_592))
    }
}

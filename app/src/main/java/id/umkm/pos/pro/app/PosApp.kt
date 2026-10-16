package id.umkm.pos.pro.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val RAIL_BREAKPOINT = 600.dp

@Composable
fun PosApp(modifier: Modifier = Modifier) {
    var selectedDestination by rememberSaveable { mutableStateOf(PosDestination.CASHIER) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedWindow = maxWidth >= RAIL_BREAKPOINT
        if (isExpandedWindow) {
            ExpandedLayout(selectedDestination) { selectedDestination = it }
        } else {
            CompactLayout(selectedDestination) { selectedDestination = it }
        }
    }
}

@Composable
private fun CompactLayout(
    selectedDestination: PosDestination,
    onSelectDestination: (PosDestination) -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                PosDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == selectedDestination,
                        onClick = { onSelectDestination(destination) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { contentPadding ->
        DestinationContent(selectedDestination, Modifier.padding(contentPadding))
    }
}

@Composable
private fun ExpandedLayout(
    selectedDestination: PosDestination,
    onSelectDestination: (PosDestination) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            PosDestination.entries.forEach { destination ->
                NavigationRailItem(
                    selected = destination == selectedDestination,
                    onClick = { onSelectDestination(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(destination.label) },
                )
            }
        }
        DestinationContent(selectedDestination, Modifier.fillMaxSize())
    }
}

@Composable
private fun DestinationContent(
    destination: PosDestination,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Text(
            text = destination.label,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

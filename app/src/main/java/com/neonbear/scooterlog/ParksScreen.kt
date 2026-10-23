@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.neonbear.scooterlog

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private fun distanceText(m: Float): String =
    if (m < 1000f) "${m.toInt()} m away" else String.format(Locale.US, "%.1f km away", m / 1000f)

@Composable
fun ParksScreen(store: AppStore, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var newName by remember { mutableStateOf("") }
    val fmt = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val total = store.parks.sumOf { it.visits.size }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        if (res.values.any { it }) refreshNearby(ctx, store) else store.nearby = NearbyState.NoPermission
    }
    val askOrLoad: () -> Unit = {
        if (ctx.hasLocationPermission()) {
            refreshNearby(ctx, store)
        } else {
            permLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }
    // Parks near you appear automatically the first time this tab opens.
    LaunchedEffect(Unit) {
        if (store.nearby is NearbyState.Idle) askOrLoad()
    }

    ScreenScaffold(
        title = "Skateparks",
        actions = {
            IconButton(onClick = askOrLoad) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh nearby parks") }
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Near you", style = MaterialTheme.typography.titleLarge)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 25, 50).forEach { km ->
                        FilterChip(
                            selected = store.radiusKm == km,
                            onClick = { store.radiusKm = km; askOrLoad() },
                            label = { Text("$km km") }
                        )
                    }
                }
            }

            val s = store.nearby
            when (s) {
                is NearbyState.Idle, is NearbyState.Locating, is NearbyState.Loading -> item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(if (s is NearbyState.Loading) "Searching for skateparks…" else "Finding your location…")
                    }
                }
                is NearbyState.NoPermission -> item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Allow location to see skateparks around you.")
                            Button(onClick = askOrLoad) { Text("Allow location") }
                        }
                    }
                }
                is NearbyState.Error -> item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.errorContainer)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(s.message, color = cs.onErrorContainer)
                            Button(onClick = askOrLoad) { Text("Try again") }
                        }
                    }
                }
                is NearbyState.Done -> {
                    if (s.parks.isEmpty()) {
                        item {
                            Text(
                                "No skateparks found within ${s.radiusKm} km. Try a wider radius or open Google Maps below.",
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                    items(s.parks.take(10)) { p ->
                        Card(
                            onClick = { showInMaps(ctx, p.name, p.lat, p.lon) },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(44.dp).clip(CircleShape).background(cs.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = cs.onSecondaryContainer)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            distanceText(p.meters),
                                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = { directionsInMaps(ctx, p.lat, p.lon) }) {
                                        Text("Directions")
                                    }
                                    Button(onClick = {
                                        store.checkIn(p.name, p.lat, p.lon)
                                        toast("Checked in at ${p.name}")
                                    }) { Text("Check in") }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "Park data © OpenStreetMap contributors, so it may be incomplete.",
                            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { searchSkateparksInMaps(ctx) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Search skateparks in Google Maps") }
            }

            item {
                Column(Modifier.padding(top = 14.dp)) {
                    Text("My parks", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "$total visits across ${store.parks.size} parks",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        label = { Text("Add a park by name") }, singleLine = true,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = { store.addPark(newName); newName = "" },
                        enabled = newName.isNotBlank()
                    ) { Icon(Icons.Filled.Add, contentDescription = "Add park") }
                }
            }
            if (store.parks.isEmpty()) {
                item {
                    Text(
                        "No parks yet. Check in at one above, or add one by name.",
                        color = cs.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(store.parks) { i, p ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Visited ${p.visits.size} time${if (p.visits.size == 1) "" else "s"}",
                            color = cs.primary, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (p.visits.isEmpty()) "No visits logged yet"
                            else "Last visit: ${fmt.format(Date(p.visits.last()))}",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = { store.visit(i) }) { Text("I was here +1") }
                            TextButton(onClick = { store.undoVisit(i) }) { Text("Undo") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { store.deletePark(i) }) { Text("Remove") }
                        }
                        if (p.lat != null && p.lon != null) {
                            TextButton(onClick = { directionsInMaps(ctx, p.lat, p.lon) }) { Text("Directions in Google Maps") }
                        }
                    }
                }
            }
        }
    }
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.neonbear.scooterlog

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(store: AppStore, profile: Profile, onExport: () -> Unit, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val points = store.points
    val level = levelFor(points)
    val next = nextLevel(points)
    val frac = (points.toFloat() / PRO_POINTS).coerceIn(0f, 1f)
    val badges = store.landed.entries.sortedByDescending { it.value }.mapNotNull { TRICK_BY_ID[it.key] }
    val visits = store.parks.sumOf { it.visits.size }

    ScreenScaffold(
        title = "Profile",
        actions = {
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit profile") }
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer
                    )
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).background(cs.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                profile.name.take(1).uppercase(),
                                color = cs.onPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(profile.name, style = MaterialTheme.typography.headlineSmall)
                            Text("Age ${profile.age} · ${profile.style} rider")
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Level", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                        Text(
                            level.name, style = MaterialTheme.typography.headlineMedium,
                            color = cs.primary, fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                            color = cs.primary,
                            trackColor = cs.surfaceContainerHighest
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            LEVELS.forEach { lv ->
                                Box(
                                    Modifier.size(10.dp).clip(CircleShape)
                                        .background(if (points >= lv.from) cs.primary else cs.outlineVariant)
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Beginner", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                            Text("Pro", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        }
                        Text(
                            if (next != null) "$points pts · ${next.from - points} more to ${next.name}"
                            else "$points pts · you made it to Pro 🏆",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${store.landed.size}/${TRICKS.size}", "Tricks", Modifier.weight(1f))
                    StatTile("$points", "Points", Modifier.weight(1f))
                    StatTile("$visits", "Park visits", Modifier.weight(1f))
                }
            }
            item { Text("Badges", style = MaterialTheme.typography.titleLarge) }
            if (badges.isEmpty()) {
                item {
                    Text(
                        "Land a trick to earn your first badge. Open any trick in the Tricks tab.",
                        color = cs.onSurfaceVariant
                    )
                }
            } else {
                items(badges.chunked(3)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { t -> Box(Modifier.weight(1f)) { TrickBadge(t) } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            item {
                FilledTonalButton(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Export all data to JSON") }
            }
        }
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = cs.surfaceContainerHigh) {
        Column(Modifier.padding(vertical = 14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
fun TrickBadge(t: Trick) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(20.dp), color = cs.secondaryContainer) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(badgeFor(t.difficulty), fontSize = 32.sp)
            Text(
                t.name, style = MaterialTheme.typography.labelMedium,
                color = cs.onSecondaryContainer, textAlign = TextAlign.Center, maxLines = 2
            )
        }
    }
}

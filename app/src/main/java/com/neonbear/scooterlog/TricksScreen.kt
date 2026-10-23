@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.neonbear.scooterlog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Tricks list with search ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TricksScreen(store: AppStore, onOpen: (Trick) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf<String?>(null) }
    val cats = remember { TRICKS.map { it.category }.distinct() }
    val q = query.trim()
    val filtered = TRICKS.filter { t ->
        (cat == null || (cat == "Landed" && store.landed.containsKey(t.id)) || t.category == cat) &&
            (q.isEmpty() || t.name.contains(q, ignoreCase = true) || t.category.contains(q, ignoreCase = true))
    }
    val grouped = filtered.groupBy { it.category }

    ScreenScaffold(title = "Tricks") { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = CircleShape,
                    placeholder = { Text("Search ${TRICKS.size} tricks") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = cs.surfaceContainerHigh,
                        unfocusedContainerColor = cs.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = cat == null, onClick = { cat = null }, label = { Text("All") }) }
                    item {
                        FilterChip(
                            selected = cat == "Landed",
                            onClick = { cat = if (cat == "Landed") null else "Landed" },
                            label = { Text("Landed (${store.landed.size})") }
                        )
                    }
                    items(cats) { c ->
                        FilterChip(
                            selected = cat == c,
                            onClick = { cat = if (cat == c) null else c },
                            label = { Text(c) }
                        )
                    }
                }
            }
            item {
                Text(
                    "${filtered.size} tricks · % of riders are community estimates",
                    style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No tricks match. Try a different name or clear the filter.",
                        color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            grouped.forEach { (c, list) ->
                item(key = "h_$c") {
                    Text(
                        c, style = MaterialTheme.typography.titleMedium, color = cs.primary,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp)
                    )
                }
                items(list, key = { it.id }) { t ->
                    TrickRow(t, store.landed.containsKey(t.id)) { onOpen(t) }
                }
            }
        }
    }
}

@Composable
fun TrickRow(t: Trick, landed: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(if (landed) cs.primaryContainer else cs.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (landed) badgeFor(t.difficulty) else t.difficulty.toString(),
                    fontSize = if (landed) 22.sp else 16.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(t.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${difficultyLabel(t.difficulty)} · ${t.knownPercent}% of riders",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = cs.onSurfaceVariant)
        }
    }
}

// ---------- Trick detail: animation, description, steps ----------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrickDetailScreen(
    store: AppStore,
    t: Trick,
    snack: SnackbarHostState,
    toast: (String) -> Unit,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val landed = store.landed.containsKey(t.id)
    var slow by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = cs.surface,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(t.name, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    TrickAnimation(t.anim, slow, Modifier.fillMaxWidth().height(230.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Simplified animation of the motion",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant, modifier = Modifier.weight(1f)
                        )
                        FilterChip(selected = slow, onClick = { slow = !slow }, label = { Text("Slow motion") })
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(t.category) })
                AssistChip(
                    onClick = {},
                    label = { Text("${"★".repeat(t.difficulty)} ${difficultyLabel(t.difficulty)}") }
                )
                AssistChip(onClick = {}, label = { Text("+${pointsFor(t.difficulty)} pts") })
            }

            Surface(shape = RoundedCornerShape(24.dp), color = cs.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${t.knownPercent}% of riders know this trick",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(
                        progress = { t.knownPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = cs.secondary,
                        trackColor = cs.surfaceContainerHighest
                    )
                    Text("Community estimate", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About", style = MaterialTheme.typography.titleLarge)
                Text(t.desc, style = MaterialTheme.typography.bodyLarge)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How to", style = MaterialTheme.typography.titleLarge)
                t.steps.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(cs.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${i + 1}", color = cs.onPrimaryContainer,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(s, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
            }

            Surface(shape = RoundedCornerShape(20.dp), color = cs.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Safety: wear a helmet and pads, start on smooth ground, and learn big tricks with a coach or in a foam pit.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium, color = cs.onTertiaryContainer
                )
            }

            if (landed) {
                FilledTonalButton(
                    onClick = { store.toggleTrick(t.id); toast("Removed ${t.name}") },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("${badgeFor(t.difficulty)}  Landed · tap to undo") }
            } else {
                Button(
                    onClick = {
                        store.toggleTrick(t.id)
                        toast("Badge earned: ${t.name} ${badgeFor(t.difficulty)}")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("I landed it!") }
            }
        }
    }
}

package com.neonbear.scooterlog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(store: AppStore) {
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use {
                    it.write(store.toJson().toString(2).toByteArray())
                }
            }.isSuccess
            scope.launch { snack.showSnackbar(if (ok) "Exported to JSON ✔" else "Export failed") }
        }
    }

    val profile = store.profile
    if (profile == null || editing) {
        CreateProfileScreen(
            initial = profile,
            onSave = { store.saveProfile(it); editing = false },
            onCancel = if (profile != null) ({ editing = false }) else null
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val items = listOf("👤" to "Profile", "🛴" to "Tricks", "🏟️" to "Parks")
                items.forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(icon, fontSize = 20.sp) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> ProfileScreen(
                    store, profile,
                    onExport = { exportLauncher.launch("scooterlog-data.json") },
                    onEdit = { editing = true }
                )
                1 -> TricksScreen(store) { msg -> scope.launch { snack.showSnackbar(msg) } }
                else -> ParksScreen(store)
            }
        }
    }
}

// ---------- Create / edit profile ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(initial: Profile?, onSave: (Profile) -> Unit, onCancel: (() -> Unit)?) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var age by remember { mutableStateOf(initial?.age?.toString() ?: "") }
    var style by remember { mutableStateOf(initial?.style ?: "Street") }
    val ageNum = age.toIntOrNull()
    val valid = name.isNotBlank() && ageNum != null && ageNum in 1..99

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🛴", fontSize = 56.sp)
        Text(
            if (initial == null) "Create your profile" else "Edit profile",
            fontSize = 28.sp, fontWeight = FontWeight.Bold
        )
        Text("Tell us who's riding.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = age, onValueChange = { v -> age = v.filter { it.isDigit() }.take(2) },
            label = { Text("Age") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Text("I ride", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Street", "Park").forEach { s ->
                FilterChip(selected = style == s, onClick = { style = s }, label = { Text(s) })
            }
        }
        Button(
            onClick = { onSave(Profile(name.trim(), ageNum!!, style)) },
            enabled = valid, modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (initial == null) "Create profile" else "Save", fontSize = 16.sp) }
        if (onCancel != null) {
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

// ---------- Profile tab ----------

@Composable
fun ProfileScreen(store: AppStore, profile: Profile, onExport: () -> Unit, onEdit: () -> Unit) {
    val points = store.points
    val level = levelFor(points)
    val next = nextLevel(points)
    val frac = (points.toFloat() / PRO_POINTS).coerceIn(0f, 1f)
    val badges = store.landed.entries.sortedByDescending { it.value }.mapNotNull { e ->
        TRICK_BY_ID[e.key]
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) { Text("🛴", fontSize = 30.sp) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Age ${profile.age} · ${profile.style} rider",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onEdit) { Text("Edit") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Level", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text(level.name, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(
                        progress = { frac },
                        modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Beginner", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Pro", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (next != null) "$points pts · ${next.from - points} more to ${next.name}"
                        else "$points pts · you made it to Pro 🏆",
                        fontSize = 14.sp
                    )
                    Text(
                        "${store.landed.size} of ${TRICKS.size} tricks landed",
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Text("Badges", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (badges.isEmpty()) {
            item {
                Text(
                    "Land a trick in the Tricks tab to earn your first badge.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(badges.chunked(3)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { t -> Box(Modifier.weight(1f)) { Badge(t) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item {
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Export all data to JSON")
            }
        }
    }
}

@Composable
fun Badge(t: Trick) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(badgeFor(t.difficulty), fontSize = 32.sp)
            Text(t.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

// ---------- Tricks tab ----------

@Composable
fun TricksScreen(store: AppStore, toast: (String) -> Unit) {
    val groups = remember { TRICKS.groupBy { it.category } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Tricks", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${store.landed.size} / ${TRICKS.size} landed · rider % are community estimates",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
            }
        }
        groups.forEach { (cat, list) ->
            item {
                Text(
                    cat, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            items(list, key = { it.id }) { t ->
                val isLanded = store.landed.containsKey(t.id)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isLanded) { Text(badgeFor(t.difficulty), fontSize = 20.sp); Spacer(Modifier.width(6.dp)) }
                                Text(t.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "★".repeat(t.difficulty) + "☆".repeat(5 - t.difficulty),
                                color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp
                            )
                            Text(
                                "${t.knownPercent}% of riders know this",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { t.knownPercent / 100f },
                                modifier = Modifier.fillMaxWidth(0.8f).height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        if (isLanded) {
                            Button(onClick = {
                                store.toggleTrick(t.id); toast("Removed ${t.name}")
                            }) { Text("Landed ✓") }
                        } else {
                            OutlinedButton(onClick = {
                                store.toggleTrick(t.id)
                                toast("Badge earned: ${t.name} ${badgeFor(t.difficulty)}")
                            }) { Text("Landed it") }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Skatepark tracker ----------

@Composable
fun ParksScreen(store: AppStore) {
    var newName by remember { mutableStateOf("") }
    val fmt = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val total = store.parks.sumOf { it.visits.size }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Skateparks", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$total visits across ${store.parks.size} parks",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("Park name") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { store.addPark(newName); newName = "" },
                    enabled = newName.isNotBlank()
                ) { Text("Add") }
            }
        }
        if (store.parks.isEmpty()) {
            item {
                Text("No parks yet. Add the first one you ride.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        itemsIndexed(store.parks) { i, p ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Visited ${p.visits.size} time${if (p.visits.size == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (p.visits.isEmpty()) "No visits logged yet"
                        else "Last visit: ${fmt.format(Date(p.visits.last()))}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { store.visit(i) }) { Text("I was here +1") }
                        TextButton(onClick = { store.undoVisit(i) }) { Text("Undo") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { store.deletePark(i) }) { Text("Remove") }
                    }
                }
            }
        }
    }
}

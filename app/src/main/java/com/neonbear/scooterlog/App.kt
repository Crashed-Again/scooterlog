@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.neonbear.scooterlog

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun App(store: AppStore) {
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }

    val toast: (String) -> Unit = { m -> scope.launch { snack.showSnackbar(m) } }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use {
                    it.write(store.toJson().toString(2).toByteArray())
                }
            }.isSuccess
            toast(if (ok) "Exported to JSON" else "Export failed")
        }
    }

    BackHandler(enabled = openId != null) { openId = null }

    val profile = store.profile
    val oid = openId
    val openTrick = if (oid != null) TRICK_BY_ID[oid] else null

    if (profile == null || editing) {
        CreateProfileScreen(
            initial = profile,
            onSave = { store.saveProfile(it); editing = false },
            onCancel = if (profile != null) ({ editing = false }) else null
        )
    } else if (openTrick != null) {
        TrickDetailScreen(store, openTrick, snack, toast) { openId = null }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snack) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Profile") }
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { tab = 1 },
                        icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                        label = { Text("Tricks") }
                    )
                    NavigationBarItem(
                        selected = tab == 2, onClick = { tab = 2 },
                        icon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        label = { Text("Parks") }
                    )
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
                    1 -> TricksScreen(store) { openId = it.id }
                    else -> ParksScreen(store, toast)
                }
            }
        }
    }
}

/** Large collapsing top bar used by the three main tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val cs = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(behavior.nestedScrollConnection),
        containerColor = cs.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Medium) },
                actions = actions,
                scrollBehavior = behavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = cs.surface,
                    scrolledContainerColor = cs.surfaceContainer
                )
            )
        }
    ) { content(it) }
}

// ---------- Create / edit profile ----------

@Composable
fun CreateProfileScreen(initial: Profile?, onSave: (Profile) -> Unit, onCancel: (() -> Unit)?) {
    val cs = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var age by remember { mutableStateOf(initial?.age?.toString() ?: "") }
    var style by remember { mutableStateOf(initial?.style ?: "Street") }
    val ageNum = age.toIntOrNull()
    val valid = name.isNotBlank() && ageNum != null && ageNum in 1..99

    Column(
        Modifier
            .fillMaxSize()
            .background(cs.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🛴", fontSize = 56.sp)
        Text(
            if (initial == null) "Create your profile" else "Edit profile",
            style = MaterialTheme.typography.headlineLarge
        )
        Text("Tell us who is riding.", color = cs.onSurfaceVariant)

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Name") }, singleLine = true,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = age, onValueChange = { v -> age = v.filter { it.isDigit() }.take(2) },
            label = { Text("Age") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        )
        Text("I ride", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Street", "Park").forEach { s ->
                FilterChip(selected = style == s, onClick = { style = s }, label = { Text(s) })
            }
        }
        Button(
            onClick = { onSave(Profile(name.trim(), ageNum!!, style)) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(if (initial == null) "Create profile" else "Save", fontSize = 16.sp) }
        if (onCancel != null) {
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

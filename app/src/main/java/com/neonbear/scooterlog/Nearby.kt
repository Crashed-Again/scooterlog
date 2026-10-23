package com.neonbear.scooterlog

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class NearbyPark(val name: String, val lat: Double, val lon: Double, val meters: Float)

sealed interface NearbyState {
    object Idle : NearbyState
    object NoPermission : NearbyState
    object Locating : NearbyState
    object Loading : NearbyState
    data class Done(val parks: List<NearbyPark>, val radiusKm: Int) : NearbyState
    data class Error(val message: String) : NearbyState
}

fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
fun getLocation(ctx: Context, cb: (Location?) -> Unit) {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = lm.getProviders(true)
    val last = providers
        .mapNotNull { p -> try { lm.getLastKnownLocation(p) } catch (e: Exception) { null } }
        .maxByOrNull { it.time }
    if (last != null && System.currentTimeMillis() - last.time < 10 * 60 * 1000L) {
        cb(last)
        return
    }
    val provider = when {
        providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        else -> null
    }
    if (provider == null) {
        cb(last)
        return
    }
    val consumer = androidx.core.util.Consumer<Location?> { loc -> cb(loc ?: last) }
    // Two overloads differ only by the CancellationSignal type, so give the null an explicit type.
    val noCancel: android.os.CancellationSignal? = null
    LocationManagerCompat.getCurrentLocation(
        lm, provider, noCancel, ContextCompat.getMainExecutor(ctx), consumer
    )
}

private fun overpassQuery(lat: Double, lon: Double, radiusM: Int): String {
    val la = String.format(Locale.US, "%.5f", lat)
    val lo = String.format(Locale.US, "%.5f", lon)
    return "[out:json][timeout:25];(" +
        "nwr[\"sport\"=\"skateboard\"](around:$radiusM,$la,$lo);" +
        "nwr[\"leisure\"=\"skatepark\"](around:$radiusM,$la,$lo);" +
        ");out center 80;"
}

/** Looks up skateparks from OpenStreetMap (Overpass API). Blocking: call from Dispatchers.IO. */
fun findSkateparks(lat: Double, lon: Double, radiusKm: Int): List<NearbyPark> {
    val q = overpassQuery(lat, lon, radiusKm * 1000)
    val endpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )
    var last: Exception? = null
    for (ep in endpoints) {
        try {
            val conn = URL(ep).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.setRequestProperty("User-Agent", "ScooterLog/1.1 (Android)")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(("data=" + URLEncoder.encode(q, "UTF-8")).toByteArray()) }
            if (conn.responseCode != 200) throw RuntimeException("HTTP " + conn.responseCode)
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return parseParks(body, lat, lon)
        } catch (e: Exception) {
            last = e
        }
    }
    throw last ?: RuntimeException("Search failed")
}

private fun parseParks(body: String, lat: Double, lon: Double): List<NearbyPark> {
    val els = JSONObject(body).optJSONArray("elements") ?: return emptyList()
    val out = ArrayList<NearbyPark>()
    for (i in 0 until els.length()) {
        val e = els.getJSONObject(i)
        var la = e.optDouble("lat", Double.NaN)
        var lo = e.optDouble("lon", Double.NaN)
        if (la.isNaN()) {
            val c = e.optJSONObject("center")
            if (c != null) {
                la = c.optDouble("lat", Double.NaN)
                lo = c.optDouble("lon", Double.NaN)
            }
        }
        if (la.isNaN() || lo.isNaN()) continue
        val name = e.optJSONObject("tags")?.optString("name", "")?.takeIf { it.isNotBlank() } ?: "Skatepark"
        val res = FloatArray(1)
        Location.distanceBetween(lat, lon, la, lo, res)
        out.add(NearbyPark(name, la, lo, res[0]))
    }
    return out.sortedBy { it.meters }
        .distinctBy { it.name + String.format(Locale.US, "%.3f,%.3f", it.lat, it.lon) }
        .take(30)
}

fun refreshNearby(ctx: Context, store: AppStore) {
    store.nearby = NearbyState.Locating
    getLocation(ctx) { loc ->
        if (loc == null) {
            store.nearby = NearbyState.Error("Couldn't get your location. Make sure location is turned on, then try again.")
        } else {
            store.nearby = NearbyState.Loading
            val km = store.radiusKm
            store.scope.launch {
                try {
                    val list = withContext(Dispatchers.IO) { findSkateparks(loc.latitude, loc.longitude, km) }
                    store.nearby = NearbyState.Done(list, km)
                } catch (e: Exception) {
                    store.nearby = NearbyState.Error("Couldn't load skateparks. Check your internet connection and try again.")
                }
            }
        }
    }
}

/** Opens Google Maps when installed, otherwise falls back to the web link. */
fun openMaps(ctx: Context, uri: String, web: String) {
    try {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage("com.google.android.apps.maps")
        )
    } catch (e: Exception) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
        } catch (_: Exception) { }
    }
}

fun directionsInMaps(ctx: Context, lat: Double, lon: Double) =
    openMaps(ctx, "google.navigation:q=$lat,$lon", "https://www.google.com/maps/dir/?api=1&destination=$lat,$lon")

fun showInMaps(ctx: Context, name: String, lat: Double, lon: Double) =
    openMaps(
        ctx, "geo:$lat,$lon?q=$lat,$lon(" + Uri.encode(name) + ")",
        "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
    )

fun searchSkateparksInMaps(ctx: Context) =
    openMaps(ctx, "geo:0,0?q=skatepark", "https://www.google.com/maps/search/skatepark")

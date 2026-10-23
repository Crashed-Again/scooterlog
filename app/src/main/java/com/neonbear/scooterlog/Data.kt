package com.neonbear.scooterlog

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Profile(val name: String, val age: Int, val style: String)
data class Park(
    val name: String,
    val visits: List<Long>,
    val lat: Double? = null,
    val lon: Double? = null
)

class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("scooterlog", Context.MODE_PRIVATE)

    /** App-wide scope so a nearby-parks search survives switching tabs. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var profile by mutableStateOf<Profile?>(null)
        private set
    val landed = mutableStateMapOf<String, Long>()
    val parks = mutableStateListOf<Park>()

    // Not saved: nearby skateparks are re-fetched each time.
    var nearby by mutableStateOf<NearbyState>(NearbyState.Idle)
    var radiusKm by mutableStateOf(25)

    init { load() }

    val points: Int
        get() = landed.keys.sumOf { pointsFor(TRICK_BY_ID[it]?.difficulty ?: 0) }

    fun saveProfile(p: Profile) { profile = p; save() }

    /** Returns true if the trick is now landed (badge earned). */
    fun toggleTrick(id: String): Boolean {
        val nowLanded = if (landed.containsKey(id)) { landed.remove(id); false }
        else { landed[id] = System.currentTimeMillis(); true }
        save()
        return nowLanded
    }

    fun addPark(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        parks.add(Park(n, emptyList())); save()
    }

    fun visit(i: Int) {
        parks[i] = parks[i].copy(visits = parks[i].visits + System.currentTimeMillis()); save()
    }

    fun undoVisit(i: Int) {
        val v = parks[i].visits
        if (v.isNotEmpty()) { parks[i] = parks[i].copy(visits = v.dropLast(1)); save() }
    }

    fun deletePark(i: Int) { parks.removeAt(i); save() }

    /** Log a visit at a park, adding it to "My parks" first if needed. */
    fun checkIn(name: String, lat: Double?, lon: Double?) {
        var i = parks.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (i < 0) {
            parks.add(Park(name, emptyList(), lat, lon))
            i = parks.size - 1
        }
        visit(i)
    }

    private fun iso(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date(ms))

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("app", "ScooterLog")
        o.put("exportedAt", iso(System.currentTimeMillis()))
        profile?.let {
            o.put("profile", JSONObject().put("name", it.name).put("age", it.age).put("style", it.style))
        }
        o.put(
            "progress",
            JSONObject()
                .put("points", points)
                .put("proPoints", PRO_POINTS)
                .put("level", levelFor(points).name)
                .put("tricksLanded", landed.size)
                .put("tricksTotal", TRICKS.size)
        )
        val tricks = JSONArray()
        landed.entries.sortedBy { it.value }.forEach { e ->
            val t = TRICK_BY_ID[e.key]
            tricks.put(
                JSONObject()
                    .put("id", e.key)
                    .put("name", t?.name ?: e.key)
                    .put("category", t?.category ?: "")
                    .put("difficulty", t?.difficulty ?: 0)
                    .put("ridersKnowPercentEstimate", t?.knownPercent ?: 0)
                    .put("landedAt", iso(e.value))
                    .put("landedAtMillis", e.value)
            )
        }
        o.put("landedTricks", tricks)
        val ps = JSONArray()
        parks.forEach { p ->
            val po = JSONObject()
                .put("name", p.name)
                .put("totalVisits", p.visits.size)
                .put("visitDates", JSONArray(p.visits.map { iso(it) }))
                .put("visitsMillis", JSONArray(p.visits))
            if (p.lat != null && p.lon != null) {
                po.put("lat", p.lat)
                po.put("lon", p.lon)
            }
            ps.put(po)
        }
        o.put("skateparks", ps)
        return o
    }

    private fun save() {
        prefs.edit().putString("data", toJson().toString()).apply()
    }

    private fun load() {
        val s = prefs.getString("data", null) ?: return
        try {
            val o = JSONObject(s)
            o.optJSONObject("profile")?.let {
                profile = Profile(it.getString("name"), it.getInt("age"), it.getString("style"))
            }
            val t = o.optJSONArray("landedTricks") ?: JSONArray()
            for (i in 0 until t.length()) {
                val x = t.getJSONObject(i)
                landed[x.getString("id")] = x.getLong("landedAtMillis")
            }
            val ps = o.optJSONArray("skateparks") ?: JSONArray()
            for (i in 0 until ps.length()) {
                val x = ps.getJSONObject(i)
                val vm = x.optJSONArray("visitsMillis") ?: JSONArray()
                val lat = if (x.has("lat")) x.getDouble("lat") else null
                val lon = if (x.has("lon")) x.getDouble("lon") else null
                parks.add(Park(x.getString("name"), List(vm.length()) { vm.getLong(it) }, lat, lon))
            }
        } catch (_: Exception) { }
    }
}

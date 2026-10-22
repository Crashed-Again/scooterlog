package com.neonbear.scooterlog

data class Trick(
    val name: String,
    val category: String,
    val difficulty: Int,      // 1 (easy) .. 5 (insane)
    val knownPercent: Int     // estimated % of riders who can land it
) {
    val id: String
        get() = (category + "_" + name).lowercase().replace(Regex("[^a-z0-9]+"), "_")
}

val TRICKS = listOf(
    // Basics
    Trick("Bunny Hop", "Basics", 1, 96),
    Trick("Curb Hop", "Basics", 1, 90),
    Trick("Kick Turn", "Basics", 1, 90),
    Trick("Manual", "Basics", 1, 88),
    Trick("Fakie", "Basics", 1, 82),
    Trick("Drop In", "Basics", 1, 76),
    Trick("Tic Tac", "Basics", 1, 72),
    Trick("Nose Manual", "Basics", 2, 50),
    Trick("One-Foot Manual", "Basics", 3, 30),

    // Air tricks
    Trick("Barspin", "Air tricks", 2, 66),
    Trick("X-Up", "Air tricks", 2, 48),
    Trick("No Footer", "Air tricks", 2, 42),
    Trick("Superman", "Air tricks", 2, 38),
    Trick("Tailwhip", "Air tricks", 3, 52),
    Trick("Decade", "Air tricks", 3, 36),
    Trick("Can-Can", "Air tricks", 3, 34),
    Trick("No Hander", "Air tricks", 3, 33),
    Trick("Turndown", "Air tricks", 3, 30),
    Trick("Tuck No Hander", "Air tricks", 4, 18),

    // Spins
    Trick("180", "Spins", 2, 72),
    Trick("360", "Spins", 3, 46),
    Trick("540", "Spins", 4, 14),
    Trick("720", "Spins", 5, 5),
    Trick("900", "Spins", 5, 1),

    // Flips & combos
    Trick("Backflip", "Flips & combos", 4, 18),
    Trick("360 Tailwhip", "Flips & combos", 4, 16),
    Trick("Double Barspin", "Flips & combos", 4, 14),
    Trick("Fufanu", "Flips & combos", 4, 8),
    Trick("Frontflip", "Flips & combos", 5, 8),
    Trick("Double Tailwhip", "Flips & combos", 5, 4),
    Trick("Flair", "Flips & combos", 5, 4),
    Trick("Bri Flip", "Flips & combos", 5, 2),
    Trick("Cork", "Flips & combos", 5, 2),

    // Grinds & street
    Trick("50-50", "Grinds & street", 2, 70),
    Trick("Wallride", "Grinds & street", 2, 52),
    Trick("Wall Tap", "Grinds & street", 2, 40),
    Trick("Stair Gap", "Grinds & street", 3, 45),
    Trick("5-0", "Grinds & street", 3, 44),
    Trick("Smith", "Grinds & street", 3, 38),
    Trick("Feeble", "Grinds & street", 3, 36),
    Trick("Nosegrind", "Grinds & street", 3, 32),
    Trick("Willy Grind", "Grinds & street", 3, 28),
    Trick("Crooked", "Grinds & street", 4, 20),
    Trick("Handrail 50-50", "Grinds & street", 4, 25),
    Trick("Overcrook", "Grinds & street", 4, 14),

    // Park
    Trick("Quarter Pipe Air", "Park", 2, 70),
    Trick("Bowl Carve", "Park", 2, 50),
    Trick("Rock to Fakie", "Park", 2, 55),
    Trick("Hip Jump", "Park", 2, 60),
    Trick("Axle Stall", "Park", 3, 45),
    Trick("Spine Transfer", "Park", 3, 40),
)

val TRICK_BY_ID: Map<String, Trick> = TRICKS.associateBy { it.id }

fun pointsFor(difficulty: Int): Int = intArrayOf(0, 1, 2, 3, 5, 8)[difficulty.coerceIn(0, 5)]

fun badgeFor(difficulty: Int): String = when (difficulty) {
    1 -> "🛴"
    2 -> "🥉"
    3 -> "🥈"
    4 -> "🥇"
    else -> "🏆"
}

const val PRO_POINTS = 120

data class Level(val name: String, val from: Int)

val LEVELS = listOf(
    Level("Beginner", 0),
    Level("Rookie", 8),
    Level("Intermediate", 25),
    Level("Advanced", 55),
    Level("Expert", 90),
    Level("Pro", PRO_POINTS),
)

fun levelFor(points: Int): Level = LEVELS.last { points >= it.from }
fun nextLevel(points: Int): Level? = LEVELS.firstOrNull { it.from > points }

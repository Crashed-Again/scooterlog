package com.neonbear.scooterlog

// ---------- animation description (used by TrickAnimation.kt) ----------

enum class Kind { MANUAL, AIR, GRIND, WALL, RAMP }
enum class Obstacle { NONE, CURB, STAIRS }
enum class AirPose { NONE, NO_HAND, NO_FOOT, ONE_FOOT, SUPERMAN, CANCAN, TURNDOWN, XUP, TUCK, TUCK_NO_HAND }
enum class RampStyle { AIR, STALL, ROCK, CARVE, DROPIN }

data class Anim(
    val kind: Kind,
    val obstacle: Obstacle = Obstacle.NONE,
    val yaw: Float = 0f,       // full body turns (0.5 = 180)
    val whip: Float = 0f,      // full deck turns (tailwhip)
    val bar: Float = 0f,       // full handlebar turns (barspin)
    val flip: Float = 0f,      // + = backflip, - = frontflip
    val swing: Float = 0f,     // deck swings out and back (decade)
    val pose: AirPose = AirPose.NONE,
    val pitch: Float = 0f,     // manual / grind angle in degrees (+ = nose up)
    val wiggle: Float = 0f,    // tic tac rocking
    val reverse: Boolean = false,
    val ramp: RampStyle = RampStyle.AIR,
    val tap: Boolean = false,
    val high: Boolean = false
)

data class Trick(
    val name: String,
    val category: String,
    val difficulty: Int,       // 1 (easy) .. 5 (insane)
    val knownPercent: Int,     // estimated % of riders who can land it
    val desc: String,
    val steps: List<String>,
    val anim: Anim
) {
    // Keep this formula stable: saved progress is keyed by it.
    val id: String
        get() = (category + "_" + name).lowercase().replace(Regex("[^a-z0-9]+"), "_")
}

private fun air(
    obstacle: Obstacle = Obstacle.NONE, yaw: Float = 0f, whip: Float = 0f, bar: Float = 0f,
    flip: Float = 0f, swing: Float = 0f, pose: AirPose = AirPose.NONE
) = Anim(Kind.AIR, obstacle = obstacle, yaw = yaw, whip = whip, bar = bar, flip = flip, swing = swing, pose = pose)

private fun manual(
    pitch: Float = 0f, yaw: Float = 0f, wiggle: Float = 0f, reverse: Boolean = false, pose: AirPose = AirPose.NONE
) = Anim(Kind.MANUAL, pitch = pitch, yaw = yaw, wiggle = wiggle, reverse = reverse, pose = pose)

private fun grind(pitch: Float, high: Boolean = false) = Anim(Kind.GRIND, pitch = pitch, high = high)
private fun wall(tap: Boolean) = Anim(Kind.WALL, tap = tap)
private fun ramp(style: RampStyle, yaw: Float = 0f, whip: Float = 0f, bar: Float = 0f, flip: Float = 0f) =
    Anim(Kind.RAMP, ramp = style, yaw = yaw, whip = whip, bar = bar, flip = flip)

private fun t(name: String, cat: String, d: Int, pct: Int, desc: String, steps: String, anim: Anim) =
    Trick(name, cat, d, pct, desc, steps.split("|"), anim)

private const val BASICS = "Basics"
private const val AIR = "Air tricks"
private const val SPINS = "Spins"
private const val FLIPS = "Flips & combos"
private const val STREET = "Grinds & street"
private const val PARK = "Park"

val TRICKS: List<Trick> = listOf(
    // ---------------- Basics ----------------
    t("Bunny Hop", BASICS, 1, 96,
        "The foundation of almost every trick: lift the whole scooter off the ground in one smooth jump.",
        "Roll slowly and bend your knees|Spring up and pull the bars toward your chest|Lift your feet so the deck stays level in the air|Bend your knees to soak up the landing",
        air()),
    t("Curb Hop", BASICS, 1, 90,
        "Hop up onto a curb or low ledge and keep rolling.",
        "Roll toward the curb at a slow, steady speed|Bunny hop just before the front wheel reaches it|Bring the back wheel up and over after the front|Land on top and roll away",
        air(obstacle = Obstacle.CURB)),
    t("Kick Turn", BASICS, 1, 90,
        "Turn the scooter around on its back wheel. Great for changing direction in a skatepark.",
        "Roll slowly and lean back to lift the front wheel|Look over your shoulder where you want to go|Pivot on the back wheel using the bars and your hips|Set the front wheel down and ride off",
        manual(pitch = 28f, yaw = 0.5f)),
    t("Manual", BASICS, 1, 88,
        "Balance on the back wheel while rolling, with the front wheel in the air.",
        "Roll slowly on flat, smooth ground|Lean back and lift the bars to raise the front wheel|Find the balance point and keep your arms steady|Push the bars slightly away to come back down",
        manual(pitch = 25f)),
    t("Nose Manual", BASICS, 2, 50,
        "Roll on the front wheel only, with the back wheel lifted.",
        "Roll slowly with your weight over the front foot|Shift your weight forward and lift the tail|Hold the balance point with soft arms|Lower the tail gently to finish",
        manual(pitch = -22f)),
    t("Fakie", BASICS, 1, 82,
        "Roll backwards while staying balanced and in control.",
        "Find a smooth, flat spot or the bottom of a ramp|Stop pushing and let the scooter roll backwards|Look over your shoulder in the direction you are rolling|Keep your knees bent and the bars straight",
        manual(pitch = 0f, reverse = true)),
    t("Drop In", BASICS, 1, 76,
        "Roll down a ramp from the top (the coping) into the transition. Start on a small ramp.",
        "Stand at the top with the back wheel on the platform and the front wheel over the edge|Lean forward over the bars and commit your weight|Let the scooter roll down the curve with bent knees|Roll out on the flat and keep your balance",
        Anim(Kind.RAMP, ramp = RampStyle.DROPIN)),
    t("Tic Tac", BASICS, 1, 72,
        "Rock the front and back of the scooter alternately to keep moving without pushing.",
        "Roll slowly in a straight line|Lift the front wheel slightly and turn it a little|Set it down, then lift the back and turn the other way|Repeat in a rhythm to keep momentum",
        manual(wiggle = 14f)),
    t("One-Foot Manual", BASICS, 3, 30,
        "A manual with one foot lifted off the deck.",
        "Get comfortable holding a steady manual|Shift most of your weight onto the front foot|Lift the back foot slightly while keeping balance|Put the foot back before you lose the manual",
        manual(pitch = 25f, pose = AirPose.ONE_FOOT)),

    // ---------------- Air tricks ----------------
    t("Barspin", AIR, 2, 66,
        "Spin the handlebars a full 360 degrees in the air and catch them again.",
        "Bunny hop high enough to have time|Let go with one hand and flick the bars around with the other|Catch the bars with both hands|Land with the bars straight",
        air(bar = 1f)),
    t("X-Up", AIR, 2, 48,
        "Twist the bars to one side so your arms cross in an X, then untwist before landing.",
        "Pop a bunny hop|Twist the bars while crossing your arms|Hold the X for a moment in the air|Untwist and land with the bars straight",
        air(pose = AirPose.XUP)),
    t("No Footer", AIR, 2, 42,
        "Take both feet off the deck in the air, then put them back before landing.",
        "Pop a good bunny hop|Lift both feet while gripping the bars firmly|Keep the deck level with your arms|Put your feet back down before landing",
        air(pose = AirPose.NO_FOOT)),
    t("One Footer", AIR, 2, 45,
        "Take one foot off the deck in the air.",
        "Pop a bunny hop|Lift the back foot off the deck|Keep the deck level|Put the foot back and land",
        air(pose = AirPose.ONE_FOOT)),
    t("No Hander", AIR, 3, 33,
        "Let go of the bars with both hands in the air and grab them again.",
        "Bunny hop high|Release both hands briefly while your legs hold the deck level|Reach back for the bars|Catch them with both hands and land balanced",
        air(pose = AirPose.NO_HAND)),
    t("Superman", AIR, 2, 38,
        "Stretch your body and legs out behind you like Superman while holding the bars.",
        "Jump high with the bars pulled in|Push your feet back and out behind the scooter|Stretch your hips and legs out|Pull back in before landing",
        air(pose = AirPose.SUPERMAN)),
    t("Tailwhip", AIR, 3, 52,
        "Kick the deck out and spin it a full 360 degrees around the bars while you stay in the air.",
        "Bunny hop and keep your weight on the bars|Kick the deck with your back foot to start it spinning|Let it go all the way around|Catch it with your feet and land on both",
        air(whip = 1f)),
    t("Decade", AIR, 3, 36,
        "Swing the deck out to the side and bring it back without going all the way around.",
        "Pop a bunny hop|Kick the deck out sideways while holding the bars|Pull it back under you|Land with both feet on the deck",
        air(swing = 0.3f)),
    t("Can-Can", AIR, 3, 34,
        "Swing one leg over the front of the deck in the air and bring it back before landing.",
        "Bunny hop high|Take one foot off and swing that leg forward over the deck|Bring it straight back|Land with both feet on the deck",
        air(pose = AirPose.CANCAN)),
    t("Turndown", AIR, 3, 30,
        "Turn the bars down and forward in the air, then bring them back before landing.",
        "Jump high with a firm grip|Rotate the bars down and forward while the deck stays under you|Hold briefly|Bring the bars back up and land",
        air(pose = AirPose.TURNDOWN)),
    t("Tuck No Hander", AIR, 4, 18,
        "A no hander with your legs tucked up under you.",
        "Be comfortable with no handers first|Jump high and pull your knees up|Let go of the bars|Catch the bars and put your feet back before landing",
        air(pose = AirPose.TUCK_NO_HAND)),
    t("Cannonball", AIR, 3, 20,
        "Pull both knees up toward your chest in the air, then extend to land.",
        "Jump high with the bars close|Pull your knees up and tuck tight|Hold the tuck briefly|Extend your legs and land on the deck",
        air(pose = AirPose.TUCK)),

    // ---------------- Spins ----------------
    t("180", SPINS, 2, 72,
        "Turn your body and scooter half a rotation in the air and land facing backwards.",
        "Bunny hop with your weight centered|Turn your head and shoulders the way you want to spin|Rotate the bars and deck together|Spot your landing and land balanced",
        air(yaw = 0.5f)),
    t("360", SPINS, 3, 46,
        "A full rotation in the air. Build up from clean 180s first.",
        "Land clean 180s in both directions|Jump higher and turn your shoulders earlier|Keep the scooter close to your body|Spot the landing and ride away",
        air(yaw = 1f)),
    t("540", SPINS, 4, 14,
        "One and a half rotations in the air, landing backwards.",
        "Be consistent with 360s|Get more height and commit your shoulders earlier|Keep your arms tight to the body|Look for the landing before it comes around",
        air(yaw = 1.5f)),
    t("720", SPINS, 5, 5,
        "Two full rotations. Needs a lot of air and control.",
        "Land 540s regularly|Pop as high as you can and start the spin early|Stay compact so you rotate fast|Spot and land with bent knees",
        air(yaw = 2f)),
    t("900", SPINS, 5, 1,
        "Two and a half rotations. Pro level.",
        "Land clean 720s first|Maximum height and early rotation|Stay tight through the whole spin|Land backwards with soft knees",
        air(yaw = 2.5f)),
    t("1080", SPINS, 5, 1,
        "Three full rotations. Only a handful of riders have landed one.",
        "Master 900s first|Use the biggest air you can control|Spin fast and stay compact|Spot the landing and absorb it",
        air(yaw = 3f)),

    // ---------------- Flips & combos ----------------
    t("Backflip", FLIPS, 4, 18,
        "Flip backwards a full rotation in the air. Learn it in a foam pit or with an experienced coach.",
        "Master high airs and ramp airs first|Practice into a foam pit or airbag|Throw your head and shoulders back and bring the bars up and over|Look for the landing and put the scooter back under you",
        air(flip = 1f)),
    t("Frontflip", FLIPS, 5, 8,
        "Flip forwards a full rotation in the air. Needs a foam pit and a coach to learn safely.",
        "Be solid on backflips and big airs|Practice into a foam pit or airbag|Tuck your head and rotate forward over the bars|Spot the landing and open up",
        air(flip = -1f)),
    t("Flair", FLIPS, 5, 4,
        "A backflip with a half rotation at the same time.",
        "Land clean backflips and 180s|Practice with a coach into a foam pit|Add the half turn as you rotate|Spot the landing and open up",
        air(flip = 1f, yaw = 0.5f)),
    t("Cork", FLIPS, 5, 2,
        "A flip with an off-axis spin, like a corkscrew.",
        "Be consistent with flips and 360s|Practice into a foam pit or airbag|Tilt the rotation off-axis as you spin|Spot the landing and open up",
        air(flip = 1f, yaw = 1f)),
    t("Bri Flip", FLIPS, 5, 2,
        "An advanced flip variation. Learn it only after you are solid on flips and spins.",
        "Be consistent with backflips and frontflips|Watch tutorials of the exact form|Practice in a foam pit with a coach|Work on it in small steps",
        air(flip = -1f, yaw = 0.5f)),
    t("Fufanu", FLIPS, 4, 8,
        "A technical flip variation. Learn it in a foam pit and check tutorials for the exact form.",
        "Be consistent with backflips|Watch tutorials of the exact form|Practice in a foam pit with a coach|Work on it in small steps",
        air(flip = 1f, whip = 0.5f)),
    t("Double Tailwhip", FLIPS, 5, 4,
        "Kick the deck for two full spins before catching it.",
        "Land clean tailwhips every time|Jump higher and kick harder|Let the deck spin twice|Look for the deck and catch it with both feet",
        air(whip = 2f)),
    t("Triple Tailwhip", FLIPS, 5, 1,
        "Three full deck rotations in one jump.",
        "Be consistent with double tailwhips|Get maximum height|Kick hard and let it spin three times|Catch the deck and absorb the landing",
        air(whip = 3f)),
    t("Double Barspin", FLIPS, 4, 14,
        "Spin the handlebars two full rotations before catching them.",
        "Land clean single barspins every time|Jump higher and spin the bars faster|Watch the bars come around twice|Catch them with both hands and land",
        air(bar = 2f)),
    t("Triple Barspin", FLIPS, 5, 3,
        "Three full handlebar rotations in one jump.",
        "Be consistent with double barspins|Get maximum height|Flick the bars hard to spin three times|Catch them and land balanced",
        air(bar = 3f)),
    t("360 Tailwhip", FLIPS, 4, 16,
        "A 360 body spin with a tailwhip at the same time.",
        "Land 360s and tailwhips separately|Start the 360 turn first|Add the whip kick as you rotate|Catch the deck and spot the landing",
        air(yaw = 1f, whip = 1f)),
    t("360 Barspin", FLIPS, 4, 20,
        "A 360 body spin with a barspin at the same time.",
        "Land 360s and barspins separately|Start the 360 turn|Flick the bars around as you rotate|Catch them and spot the landing",
        air(yaw = 1f, bar = 1f)),
    t("Barspin Tailwhip", FLIPS, 4, 12,
        "A barspin and a tailwhip in the same jump.",
        "Land both tricks cleanly on their own|Start with the barspin|Kick the whip right after you catch the bars|Catch the deck and land",
        air(bar = 1f, whip = 1f)),
    t("No Footer Barspin", FLIPS, 3, 25,
        "Take your feet off the deck and spin the bars in the same jump.",
        "Land no footers and barspins separately|Pop high and lift your feet|Flick the bars around|Catch them and put your feet back",
        air(bar = 1f, pose = AirPose.NO_FOOT)),
    t("Superman Barspin", FLIPS, 4, 15,
        "Stretch into a Superman and spin the bars while you are out.",
        "Be consistent with Superman and barspin|Pop high and stretch out|Flick the bars around|Catch them and pull back in",
        air(bar = 1f, pose = AirPose.SUPERMAN)),
    t("540 Tailwhip", FLIPS, 5, 5,
        "A 540 spin with a tailwhip.",
        "Be consistent with 540s and 360 tailwhips|Get plenty of height|Add the whip kick mid-spin|Catch the deck and spot the landing",
        air(yaw = 1.5f, whip = 1f)),
    t("540 Barspin", FLIPS, 5, 6,
        "A 540 spin with a barspin.",
        "Be consistent with 540s and 360 barspins|Get plenty of height|Flick the bars around mid-spin|Catch them and spot the landing",
        air(yaw = 1.5f, bar = 1f)),
    t("Backflip Tailwhip", FLIPS, 5, 3,
        "A backflip with a tailwhip on the way around.",
        "Be consistent with backflips and tailwhips|Practice only in a foam pit with a coach|Kick the whip at the top of the flip|Catch the deck and land",
        air(flip = 1f, whip = 1f)),
    t("Backflip Barspin", FLIPS, 5, 3,
        "A backflip with a barspin.",
        "Be consistent with backflips and barspins|Practice only in a foam pit with a coach|Spin the bars as you rotate|Catch them and land",
        air(flip = 1f, bar = 1f)),
    t("Backflip No Hander", FLIPS, 5, 3,
        "A backflip where you let go of the bars while you rotate.",
        "Be consistent with backflips and no handers|Practice in a foam pit with a coach|Let go of the bars during the flip|Catch them before landing",
        air(flip = 1f, pose = AirPose.NO_HAND)),
    t("Frontflip Tailwhip", FLIPS, 5, 2,
        "A frontflip with a tailwhip.",
        "Be solid on frontflips and tailwhips|Practice only in a foam pit with a coach|Kick the whip during the flip|Catch the deck and land",
        air(flip = -1f, whip = 1f)),
    t("Frontflip Barspin", FLIPS, 5, 2,
        "A frontflip with a barspin.",
        "Be solid on frontflips and barspins|Practice only in a foam pit with a coach|Spin the bars during the flip|Catch them and land",
        air(flip = -1f, bar = 1f)),
    t("Double Backflip", FLIPS, 5, 1,
        "Two full backflip rotations in one jump. Pro level, with an airbag or foam pit.",
        "Master single backflips with variations|Only try it into an airbag or foam pit|Get maximum height and tuck tight|Spot the landing and open up",
        air(flip = 2f)),

    // ---------------- Grinds & street ----------------
    t("50-50", STREET, 2, 70,
        "Land with the deck on a ledge or rail and slide along it in a straight line.",
        "Roll up at a slow, steady speed|Bunny hop so the deck lines up over the ledge|Land with both axles or pegs on the edge and keep your weight centered|Slide across and hop off straight",
        grind(0f)),
    t("5-0", STREET, 3, 44,
        "Grind on the back axle only, with the front end lifted.",
        "Be comfortable with 50-50s|Hop up and lean your weight back|Land on the back axle with the front lifted|Hold the balance and come off straight",
        grind(18f)),
    t("Smith", STREET, 3, 38,
        "A grind with the back on top of the ledge and the front end hanging down to one side.",
        "Be comfortable with 50-50s|Hop up at a slight angle|Land with the back on the ledge and drop the front side|Straighten up and hop off",
        grind(-14f)),
    t("Feeble", STREET, 3, 36,
        "Like the Smith, but the front end hangs off the opposite side of the ledge.",
        "Be comfortable with Smith grinds|Hop up at an angle toward the other side|Land with the back on the ledge and the front hanging off|Straighten up and hop off",
        grind(-14f)),
    t("Nosegrind", STREET, 3, 32,
        "Grind on the front axle only, with the tail lifted.",
        "Be comfortable with 50-50s|Hop up and shift your weight forward|Land on the front axle with the tail lifted|Hold it and come off straight",
        grind(-18f)),
    t("Crooked", STREET, 4, 20,
        "A nosegrind variation with the back end angled off the ledge.",
        "Be consistent with nosegrinds|Hop up at an angle|Land on the front axle with the tail angled out|Straighten up and hop off",
        grind(-18f)),
    t("Overcrook", STREET, 4, 14,
        "An angled nosegrind variation with the deck crossing the ledge.",
        "Be consistent with crooked grinds|Hop up at a steeper angle|Land on the front axle with the deck crossing over|Straighten up and hop off",
        grind(-18f)),
    t("Willy Grind", STREET, 3, 28,
        "A technical ledge grind variation.",
        "Be comfortable with 50-50s and 5-0s|Watch tutorials of the exact form|Practice on a low ledge|Build up speed and height gradually",
        grind(10f)),
    t("Handrail 50-50", STREET, 4, 25,
        "A 50-50 grind on a handrail instead of a ledge.",
        "Be consistent with ledge 50-50s|Start on a low, flat rail|Hop up and land centered on the rail|Hold your balance and hop off straight",
        grind(0f, high = true)),
    t("Stair Gap", STREET, 3, 45,
        "Jump over a set of stairs and land on the ground below.",
        "Start with 2 or 3 steps|Roll up at medium speed|Bunny hop at the edge with your weight centered|Land with both wheels together and bent knees",
        air(obstacle = Obstacle.STAIRS)),
    t("Wallride", STREET, 2, 52,
        "Ride up onto a wall with your wheels on it, then push back down.",
        "Roll toward a smooth wall at a slight angle|Lean the scooter toward the wall as the wheels touch|Ride it up a short distance|Push off and land on the ground",
        wall(tap = false)),
    t("Wall Tap", STREET, 2, 40,
        "Touch the wall with your front wheel in a hop and roll away.",
        "Roll toward the wall at a slight angle|Bunny hop and tap the wall with the front wheel|Push off right away|Land and keep rolling",
        wall(tap = true)),

    // ---------------- Park ----------------
    t("Quarter Pipe Air", PARK, 2, 70,
        "Ride up a quarter pipe, launch off the lip, and land back into the ramp.",
        "Start on a small ramp and roll up with medium speed|Straighten your legs at the top and lift off the lip|Stay centered in the air|Land with the wheels on the ramp and ride down",
        ramp(RampStyle.AIR)),
    t("Bowl Carve", PARK, 2, 50,
        "Carve up and down the transition of a bowl while keeping your speed.",
        "Roll in with a little speed|Lean into the curve and go up the wall|Shift your weight to come back down|Repeat to keep your flow",
        ramp(RampStyle.CARVE)),
    t("Rock to Fakie", PARK, 2, 55,
        "Ride up the ramp, rock your front wheels over the coping, then roll back down fakie.",
        "Roll up toward the top with controlled speed|Rock the front wheels over the coping|Shift your weight back to turn around|Roll back down fakie",
        ramp(RampStyle.ROCK)),
    t("Hip Jump", PARK, 2, 60,
        "Jump over a hip, using the transition to launch and land on the other side.",
        "Start with a small hip|Roll up the transition at medium speed|Lift off at the top|Land on the far transition and roll away",
        ramp(RampStyle.AIR)),
    t("Axle Stall", PARK, 3, 45,
        "Ride up and stall on the coping with your axle, then drop back in.",
        "Be comfortable with rock to fakies|Roll up with just enough speed|Land the axle on the coping and hold your balance|Lean back and drop back in",
        ramp(RampStyle.STALL)),
    t("Spine Transfer", PARK, 3, 40,
        "Ride up one ramp, air over the spine, and land on the other side.",
        "Be consistent with quarter pipe airs|Roll up the first ramp at medium speed|Air over the spine and look to the landing|Land on the opposite transition and ride out",
        ramp(RampStyle.AIR)),
    t("Tombstone", PARK, 4, 12,
        "Stall at the top of a ramp with the scooter pointing straight up before dropping in.",
        "Be comfortable with axle stalls|Roll up with just enough speed|Stall with the scooter nearly vertical|Drop back in with control",
        ramp(RampStyle.STALL)),
    t("Quarter Pipe 180", PARK, 3, 40,
        "Launch off a quarter pipe and turn 180 degrees in the air.",
        "Be consistent with quarter pipe airs and ground 180s|Launch and turn your shoulders|Land going backwards on the transition|Ride out fakie",
        ramp(RampStyle.AIR, yaw = 0.5f)),
    t("Quarter Pipe Barspin", PARK, 3, 35,
        "Launch off a quarter pipe and spin the bars.",
        "Be consistent with quarter pipe airs and barspins|Launch with a good lift|Spin the bars in the air|Catch them and land on the transition",
        ramp(RampStyle.AIR, bar = 1f)),
    t("Quarter Pipe Tailwhip", PARK, 4, 25,
        "Launch off a quarter pipe and kick a tailwhip.",
        "Be consistent with quarter pipe airs and tailwhips|Launch with a good lift|Kick the whip once you are above the lip|Catch the deck and land on the transition",
        ramp(RampStyle.AIR, whip = 1f)),
    t("Quarter Pipe 360", PARK, 4, 12,
        "Launch off a quarter pipe and spin a full 360.",
        "Be consistent with quarter pipe 180s and ground 360s|Launch with a good lift|Turn your shoulders early and spot the landing|Land on the transition and ride out",
        ramp(RampStyle.AIR, yaw = 1f)),
    t("Quarter Pipe Backflip", PARK, 5, 3,
        "A backflip off a quarter pipe. Learn it in a foam pit or with a coach.",
        "Be solid on backflips in a foam pit|Roll up with controlled speed|Rotate at the lip and look for the landing|Land back on the transition",
        ramp(RampStyle.AIR, flip = 1f)),
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

fun difficultyLabel(d: Int): String =
    arrayOf("", "Easy", "Moderate", "Hard", "Very hard", "Insane")[d.coerceIn(1, 5)]

/** Pro = landing about 55% of all points available. */
val PRO_POINTS: Int = TRICKS.sumOf { pointsFor(it.difficulty) } * 55 / 100

data class Level(val name: String, val from: Int)

val LEVELS = listOf(
    Level("Beginner", 0),
    Level("Rookie", PRO_POINTS * 7 / 100),
    Level("Intermediate", PRO_POINTS * 20 / 100),
    Level("Advanced", PRO_POINTS * 40 / 100),
    Level("Expert", PRO_POINTS * 68 / 100),
    Level("Pro", PRO_POINTS),
)

fun levelFor(points: Int): Level = LEVELS.last { points >= it.from }
fun nextLevel(points: Int): Level? = LEVELS.firstOrNull { it.from > points }

package com.neonbear.scooterlog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val PIf = Math.PI.toFloat()
private val TAU = (2.0 * Math.PI).toFloat()

private fun mix(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun sm(x: Float) = x * x * (3f - 2f * x)
private fun ph(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)

private class SceneColors(
    val scooter: Color, val rider: Color, val ground: Color, val obstacle: Color, val spark: Color
)

private class Frame {
    var cx = 0f
    var cy = 0f
    var rotDeg = 0f
    var pivotX = 0f
    var pivotY = 0f
    var yaw = 0f
    var whip = 0f
    var bar = 0f
    var env = 0f
}

/**
 * Simplified side-view animation of a trick. It is a diagram-style loop that is
 * generated from the trick's [Anim] description, not a video of real technique.
 */
@Composable
fun TrickAnimation(anim: Anim, slow: Boolean, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "trick")
    val tState = inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (slow) 9000 else 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )
    val cs = MaterialTheme.colorScheme
    val colors = SceneColors(cs.primary, cs.onSurface, cs.outline, cs.secondaryContainer, Color(0xFFFFB300))
    Canvas(modifier) {
        drawScene(anim, (tState.value / 0.92f).coerceAtMost(1f), colors)
    }
}

private fun DrawScope.drawScene(a: Anim, t: Float, c: SceneColors) {
    val w = size.width
    val h = size.height
    val u = h * 0.105f
    val ground = h * 0.86f
    val deckG = ground - 0.42f * u

    drawLine(c.ground, Offset(0f, ground), Offset(w, ground), 3f)

    val f = Frame()
    f.pivotX = 0.2f * u
    f.pivotY = -1.0f * u
    var sparkAt: Offset? = null

    when (a.kind) {
        Kind.AIR -> {
            val big = a.flip != 0f || a.yaw >= 1.5f || a.whip >= 2f || a.bar >= 2f
            val peak = (if (big) 3.3f else 2.0f) * u
            val s = ph(t, 0.28f, 0.72f)
            var lift = peak * 4f * s * (1f - s)
            when (a.obstacle) {
                Obstacle.CURB -> {
                    val hh = 0.9f * u
                    drawRect(c.obstacle, Offset(0.5f * w, ground - hh), Size(w * 0.5f, hh))
                    lift += hh * sm(s)
                }
                Obstacle.STAIRS -> {
                    val hh = 1.35f * u
                    drawRect(c.obstacle, Offset(0f, ground - hh), Size(0.4f * w, hh))
                    for (i in 1..3) {
                        val sh = hh * (1f - i / 4f)
                        drawRect(
                            c.obstacle,
                            Offset(0.4f * w + (i - 1) * 0.07f * w, ground - sh),
                            Size(0.07f * w, sh)
                        )
                    }
                    lift += hh * (1f - sm(s))
                }
                Obstacle.NONE -> {}
            }
            f.cx = mix(0.1f * w, 0.9f * w, t)
            f.cy = deckG - lift
            f.rotDeg = -a.flip * 360f * sm(s)
            f.yaw = a.yaw * TAU * sm(s)
            f.whip = a.whip * TAU * sm(s) + a.swing * TAU * sin(PIf * s)
            f.bar = a.bar * TAU * sm(s)
            f.env = min(1f, sin(PIf * s) * 1.6f)
        }

        Kind.MANUAL -> {
            f.cx = if (a.reverse) mix(0.8f * w, 0.2f * w, t) else mix(0.15f * w, 0.85f * w, t)
            f.cy = deckG
            val m = sm(ph(t, 0.1f, 0.3f)) * (1f - sm(ph(t, 0.72f, 0.92f)))
            val wig = a.wiggle * sin(TAU * 3f * t) * sm(ph(t, 0.05f, 0.2f))
            f.rotDeg = -a.pitch * m + wig
            f.pivotY = 0.42f * u
            f.pivotX = when {
                a.wiggle != 0f -> if (wig >= 0f) 1.1f * u else -1.0f * u
                a.pitch >= 0f -> -1.0f * u
                else -> 1.1f * u
            }
            f.yaw = a.yaw * TAU * sm(ph(t, 0.35f, 0.7f))
            f.env = m
        }

        Kind.GRIND -> {
            val hL = (if (a.high) 1.9f else 0.95f) * u
            drawRect(c.obstacle, Offset(0.4f * w, ground - hL), Size(0.24f * w, hL))
            val a1 = ph(t, 0.22f, 0.40f)
            val a2 = ph(t, 0.66f, 0.84f)
            val g = sm(ph(t, 0.36f, 0.44f)) * (1f - sm(ph(t, 0.62f, 0.70f)))
            val lift = hL * (sm(a1) - sm(a2)) +
                0.9f * u * 4f * a1 * (1f - a1) +
                0.7f * u * 4f * a2 * (1f - a2) +
                0.12f * u * g
            f.cx = mix(0.1f * w, 0.9f * w, t)
            f.cy = deckG - lift
            f.rotDeg = -a.pitch * g
            f.pivotY = 0.42f * u
            f.pivotX = if (a.pitch >= 0f) -1.0f * u else 1.1f * u
            if (g > 0.9f) {
                val sx = when {
                    a.pitch > 0f -> f.cx - 1.0f * u
                    a.pitch < 0f -> f.cx + 1.1f * u
                    else -> f.cx
                }
                sparkAt = Offset(sx, ground - hL)
            }
        }

        Kind.WALL -> {
            val wallX = 0.8f * w
            drawRect(c.obstacle, Offset(wallX, 0f), Size(w - wallX, ground))
            val hold = if (a.tap) 0.5f else 0.68f
            val p1 = sm(ph(t, 0.25f, 0.42f))
            val p3 = ph(t, hold, hold + 0.2f)
            val rotP = p1 * (1f - sm(p3))
            val xRoll = mix(0.08f * w, wallX - 3.2f * u, sm(ph(t, 0f, 0.25f)))
            val climb = (if (a.tap) 1.0f else 2.6f) * u
            f.rotDeg = -90f * rotP
            f.pivotX = 0f
            f.pivotY = 0f
            f.cx = mix(xRoll, wallX - 0.42f * u, rotP)
            f.cy = deckG - (1.35f * u * rotP + climb * sm(ph(t, 0.4f, hold)) * (1f - p3 * p3))
        }

        Kind.RAMP -> {
            val r = 3.2f * u
            val x0 = 0.5f * w
            val lipX = x0 + r
            val lipY = ground - r
            val half = PIf / 2f
            fun surf(th: Float) = Offset(x0 + r * sin(th), ground - r * (1f - cos(th)))
            fun ctr(th: Float): Offset {
                val p = surf(th)
                return Offset(p.x - 0.42f * u * sin(th), p.y - 0.42f * u * cos(th))
            }

            val path = Path()
            path.moveTo(x0, ground)
            for (i in 0..24) {
                val p = surf(i / 24f * half)
                path.lineTo(p.x, p.y)
            }
            path.lineTo(lipX + 1.8f * u, lipY)
            path.lineTo(lipX + 1.8f * u, ground)
            path.close()
            drawPath(path, c.obstacle)
            drawLine(c.ground, Offset(lipX - 0.1f * u, lipY), Offset(lipX + 1.8f * u, lipY), 4f)

            f.pivotX = 0f
            f.pivotY = 0f
            val style = a.ramp
            val xFlat = mix(0.08f * w, x0, sm(ph(t, 0f, 0.2f)))

            if (style == RampStyle.DROPIN) {
                f.yaw = PIf
                val pA = sm(ph(t, 0f, 0.3f))
                val pB = sm(ph(t, 0.3f, 0.48f))
                val th = half * (1f - sm(ph(t, 0.48f, 0.85f)))
                val base = ctr(half)
                if (t < 0.3f) {
                    f.cx = mix(lipX + 1.0f * u, lipX + 0.2f * u, pA)
                    f.cy = lipY - 0.42f * u
                    f.rotDeg = 0f
                } else if (t < 0.48f) {
                    f.cx = mix(lipX + 0.2f * u, base.x, pB)
                    f.cy = mix(lipY - 0.42f * u, base.y, pB)
                    f.rotDeg = -90f * pB
                } else {
                    val p = ctr(th)
                    f.cx = p.x - 3f * u * sm(ph(t, 0.85f, 1f))
                    f.cy = p.y
                    f.rotDeg = -th * 180f / PIf
                }
            } else {
                val thMax = if (style == RampStyle.CARVE) 1.1f else half
                val th = if (style == RampStyle.CARVE) {
                    thMax * sm(ph(t, 0.2f, 0.5f)) * (1f - sm(ph(t, 0.5f, 0.92f)))
                } else {
                    thMax * sm(ph(t, 0.2f, 0.45f)) * (1f - sm(ph(t, 0.75f, 0.97f)))
                }
                val inMid = style != RampStyle.CARVE && t > 0.45f && t < 0.75f
                if (!inMid) {
                    val p = ctr(th)
                    f.cx = p.x + (xFlat - x0)
                    f.cy = p.y
                    f.rotDeg = -th * 180f / PIf
                    if (style == RampStyle.AIR && t >= 0.75f) f.yaw = a.yaw * TAU
                } else {
                    val base = ctr(half)
                    if (style == RampStyle.AIR) {
                        val s = ph(t, 0.45f, 0.75f)
                        f.cx = base.x - 0.6f * u * sin(PIf * s)
                        f.cy = base.y - 2.0f * u * 4f * s * (1f - s)
                        f.rotDeg = -90f - a.flip * 360f * sm(s)
                        f.yaw = a.yaw * TAU * sm(s)
                        f.whip = a.whip * TAU * sm(s)
                        f.bar = a.bar * TAU * sm(s)
                        f.env = min(1f, sin(PIf * s) * 1.6f)
                    } else {
                        val rock = style == RampStyle.ROCK
                        val holdRot = if (rock) -25f else 0f
                        val dx = if (rock) 0.5f * u else -0.5f * u
                        val hold = Offset(lipX + dx, lipY - 0.45f * u)
                        val g = sm(ph(t, 0.45f, 0.55f)) * (1f - sm(ph(t, 0.67f, 0.75f)))
                        f.cx = mix(base.x, hold.x, g)
                        f.cy = mix(base.y, hold.y, g)
                        f.rotDeg = mix(-90f, holdRot, g)
                    }
                }
            }
        }
    }

    withTransform({
        translate(f.cx, f.cy)
        rotate(f.rotDeg, Offset(f.pivotX, f.pivotY))
        scale(cos(f.yaw), 1f, Offset.Zero)
    }) {
        drawFigure(u, f, a, c)
    }

    sparkAt?.let { sp ->
        for (i in 0..5) {
            val ang = sin(t * 70f + i * 1.9f) * 1.2f - PIf / 2f
            val len = (0.3f + 0.4f * abs(sin(t * 50f + i * 2.7f))) * u
            drawLine(
                c.spark, sp, Offset(sp.x + cos(ang) * len, sp.y + sin(ang) * len),
                0.1f * u, StrokeCap.Round
            )
        }
    }
}

/** Draws the scooter and rider in local units (origin = middle of the deck, y up is negative). */
private fun DrawScope.drawFigure(u: Float, f: Frame, a: Anim, c: SceneColors) {
    val e = f.env
    fun p(o: Offset) = Offset(o.x * u, o.y * u)
    fun mixO(a0: Offset, b0: Offset, k: Float) = Offset(mix(a0.x, b0.x, k), mix(a0.y, b0.y, k))

    val pose = a.pose
    val px = 1.1f
    val cw = cos(f.whip)
    fun dx(x: Float) = px + (x - px) * cw

    // stem and handlebar position
    val turn = if (pose == AirPose.TURNDOWN) 105f * e else 0f
    val phi = turn * PIf / 180f
    val d = Offset(-0.25f, -2.2f)
    val topU = Offset(
        px + d.x * cos(phi) - d.y * sin(phi),
        0.2f + d.x * sin(phi) + d.y * cos(phi)
    )
    val barAng = f.bar + (if (pose == AirPose.XUP) 2.2f * e else 0f)

    // rider joints
    var hip = Offset(0f, -1.25f)
    var sh = Offset(0.25f, -2.2f)
    var head = Offset(0.32f, -2.75f)
    var f1 = Offset(0.6f, -0.12f)
    var f2 = Offset(-0.5f, -0.12f)
    var k1 = Offset(0.55f, -0.75f)
    var k2 = Offset(-0.15f, -0.7f)
    var hand = topU
    when (pose) {
        AirPose.NO_HAND -> hand = mixO(topU, sh + Offset(0.6f, -0.9f), e)
        AirPose.NO_FOOT -> {
            f1 = mixO(f1, Offset(0.45f, -0.85f), e)
            f2 = mixO(f2, Offset(-0.35f, -0.85f), e)
            k1 = mixO(k1, Offset(0.65f, -1.0f), e)
            k2 = mixO(k2, Offset(-0.05f, -1.05f), e)
        }
        AirPose.ONE_FOOT -> {
            f2 = mixO(f2, Offset(-0.35f, -0.85f), e)
            k2 = mixO(k2, Offset(-0.05f, -1.05f), e)
        }
        AirPose.TUCK, AirPose.TUCK_NO_HAND -> {
            f1 = mixO(f1, Offset(0.5f, -1.5f), e)
            f2 = mixO(f2, Offset(0.1f, -1.5f), e)
            k1 = mixO(k1, Offset(0.85f, -1.4f), e)
            k2 = mixO(k2, Offset(0.5f, -1.5f), e)
            if (pose == AirPose.TUCK_NO_HAND) hand = mixO(topU, sh + Offset(0.6f, -0.9f), e)
        }
        AirPose.SUPERMAN -> {
            hip = mixO(hip, Offset(-0.55f, -1.55f), e)
            sh = mixO(sh, Offset(0.45f, -1.85f), e)
            head = mixO(head, Offset(0.85f, -2.0f), e)
            f1 = mixO(f1, Offset(-1.5f, -1.45f), e)
            f2 = mixO(f2, Offset(-1.6f, -1.25f), e)
            k1 = mixO(k1, Offset(-0.5f, -1.5f), e)
            k2 = mixO(k2, Offset(-0.6f, -1.4f), e)
        }
        AirPose.CANCAN -> {
            f2 = mixO(f2, Offset(1.45f, -1.0f), e)
            k2 = mixO(k2, Offset(0.75f, -1.3f), e)
        }
        else -> {}
    }

    val legW = 0.2f * u
    val back = c.rider.copy(alpha = 0.7f)

    // back leg
    drawLine(back, p(hip), p(k2), legW, StrokeCap.Round)
    drawLine(back, p(k2), p(f2), legW, StrokeCap.Round)

    // scooter
    drawLine(c.scooter, p(Offset(dx(-1.25f), 0f)), p(Offset(px + 0.05f, 0f)), 0.32f * u, StrokeCap.Round)
    val bw = p(Offset(dx(-1.0f), 0.2f))
    val fw = p(Offset(px, 0.2f))
    drawCircle(c.rider, 0.27f * u, bw)
    drawCircle(c.scooter, 0.11f * u, bw)
    drawCircle(c.rider, 0.27f * u, fw)
    drawCircle(c.scooter, 0.11f * u, fw)
    drawLine(c.scooter, p(Offset(px, 0.05f)), p(topU), 0.2f * u, StrokeCap.Round)
    val hl = 0.55f * sin(barAng)
    drawLine(
        c.scooter, p(Offset(topU.x - hl, topU.y)), p(Offset(topU.x + hl, topU.y)),
        0.16f * u, StrokeCap.Round
    )
    drawCircle(c.scooter, 0.15f * u, p(topU))

    // front leg, torso, head, arms
    drawLine(c.rider, p(hip), p(k1), legW, StrokeCap.Round)
    drawLine(c.rider, p(k1), p(f1), legW, StrokeCap.Round)
    drawLine(c.rider, p(hip), p(sh), 0.3f * u, StrokeCap.Round)
    drawCircle(c.rider, 0.27f * u, p(head))
    val elbow = mixO(sh, hand, 0.5f) + Offset(0f, 0.2f)
    drawLine(c.rider, p(sh), p(elbow), 0.16f * u, StrokeCap.Round)
    drawLine(c.rider, p(elbow), p(hand), 0.16f * u, StrokeCap.Round)
}

package com.wingedsheep.engine.mechanics.dexterity

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The release meter the flipping player plays against, sent with the decision so the client draws
 * exactly the zones and scatter the engine scores. Strength runs 0..1; inside
 * [cleanFrom]..[cleanTo] the flip is as accurate as a hand gets, and below [tooSoftBelow] the card
 * is unlikely to turn over. The scatter terms are [FlipPhysics.scatter]'s, for the aim preview.
 */
@Serializable
data class FlipReleaseProfile(
    val cleanFrom: Double = FlipPhysics.CLEAN_FROM,
    val cleanTo: Double = FlipPhysics.CLEAN_TO,
    val tooSoftBelow: Double = FlipPhysics.TOO_SOFT_BELOW,
    val baseScatter: Double = FlipPhysics.BASE_SCATTER,
    val scatterPerMm: Double = FlipPhysics.SCATTER_PER_MM,
    val sloppyScatter: Double = FlipPhysics.SLOPPY_SCATTER,
    val slopRange: Double = FlipPhysics.SLOP_RANGE,
)

/**
 * How a flipped card flies and where it lands, given the player's intent.
 *
 * The player supplies an aim point and a release strength; the engine supplies the hand's
 * unsteadiness. Scatter grows with distance from the hand and with how far the release strayed
 * from the clean zone; the number of half-turns grows with strength. All randomness comes from the
 * game's seeded RNG, so a flip replays identically.
 */
object FlipPhysics {
    const val CLEAN_FROM = 0.66
    const val CLEAN_TO = 0.82
    const val TOO_SOFT_BELOW = 0.2

    /** Scatter (one standard deviation, mm) of a clean release, before distance. */
    const val BASE_SCATTER = 5.0
    /** Extra scatter per mm between the hand and the aim point. */
    const val SCATTER_PER_MM = 0.028
    /** Extra scatter of the worst possible release. */
    const val SLOPPY_SCATTER = 80.0
    /** How far outside the clean zone a release must stray to be as sloppy as it gets. */
    const val SLOP_RANGE = 0.4

    /** Half-turns a release of strength 0 / 1 averages, and the spread around that. */
    private const val HALF_TURNS_AT_ZERO = 0.9
    private const val HALF_TURNS_PER_STRENGTH = 7.5
    private const val HALF_TURN_NOISE = 0.55

    /** Aim is clamped to this far beyond the table's edges; past it, a throw is still a miss. */
    private const val AIM_SLACK = 400.0

    /** 1 for a clean release, falling to 0 as it strays [SLOP_RANGE] outside the clean zone. */
    fun releaseQuality(strength: Double): Double {
        val s = strength.coerceIn(0.0, 1.0)
        val miss = when {
            s < CLEAN_FROM -> CLEAN_FROM - s
            s > CLEAN_TO -> s - CLEAN_TO
            else -> 0.0
        }
        return 1.0 - min(1.0, miss / SLOP_RANGE)
    }

    /** Standard deviation (mm) of where a throw at ([aimX], [aimY]) with [strength] lands. */
    fun scatter(table: FlipTable, aimX: Double, aimY: Double, strength: Double): Double {
        val distance = hypot(aimX - table.handX, aimY - table.handY)
        return BASE_SCATTER + SCATTER_PER_MM * distance + SLOPPY_SCATTER * (1.0 - releaseQuality(strength)).pow(1.6)
    }

    /** Roll where the flip lands. Returns the landing and the advanced RNG. */
    fun land(table: FlipTable, aimX: Double, aimY: Double, strength: Double, rng: GameRng): Pair<FlipLanding, GameRng> {
        val ax = aimX.coerceIn(-AIM_SLACK, table.width + AIM_SLACK)
        val ay = aimY.coerceIn(-AIM_SLACK, table.height + AIM_SLACK)
        val s = strength.coerceIn(0.0, 1.0)
        val sigma = scatter(table, ax, ay, s)

        var r = rng
        fun uniform(): Double { val (d, next) = r.nextDouble(); r = next; return d }
        fun gaussian(): Double {
            // Box–Muller; 1 - u keeps the log argument away from zero.
            val u1 = 1.0 - uniform()
            val u2 = uniform()
            return sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
        }

        val x = ax + gaussian() * sigma
        val y = ay + gaussian() * sigma
        val rotation = uniform() * 360.0
        val halfTurns = floor(max(0.0, HALF_TURNS_AT_ZERO + HALF_TURNS_PER_STRENGTH * s + gaussian() * HALF_TURN_NOISE)).toInt()
        return FlipLanding(x = x, y = y, rotation = rotation, halfTurns = halfTurns) to r
    }

    /**
     * Every card on [table] the flipped card, resting at [landing], overlaps — the touchable part of
     * it (an attachment only where it shows from under its host). Rotated-rectangle overlap by the
     * separating-axis test.
     */
    fun touched(table: FlipTable, landing: FlipLanding): List<EntityId> =
        table.cards.filter { overlaps(landing, it) }.map { it.entityId }

    fun overlaps(landing: FlipLanding, card: FlipTableCard): Boolean {
        val theta = Math.toRadians(landing.rotation)
        val c = cos(theta)
        val s = sin(theta)
        val hw = FlipTableLayout.CARD_WIDTH / 2
        val hh = FlipTableLayout.CARD_HEIGHT / 2
        // The landed card's corners.
        val orb = listOf(-hw to -hh, hw to -hh, hw to hh, -hw to hh).map { (dx, dy) ->
            (landing.x + dx * c - dy * s) to (landing.y + dx * s + dy * c)
        }
        val left = card.x - card.width / 2
        val right = card.x + card.width / 2
        val rect = listOf(left to card.hitTop, right to card.hitTop, right to card.hitBottom, left to card.hitBottom)
        val axes = listOf(1.0 to 0.0, 0.0 to 1.0, c to s, -s to c)
        return axes.all { (axisX, axisY) ->
            val a = orb.map { (px, py) -> px * axisX + py * axisY }
            val b = rect.map { (px, py) -> px * axisX + py * axisY }
            // Touching edges count as touching; strict separation is a miss.
            a.max() >= b.min() && b.max() >= a.min()
        }
    }
}

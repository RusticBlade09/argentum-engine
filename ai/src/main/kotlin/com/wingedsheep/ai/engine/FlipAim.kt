package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.engine.core.FlipOntoBattlefieldDecision
import com.wingedsheep.engine.core.FlipThrowResponse
import com.wingedsheep.engine.mechanics.dexterity.FlipLanding
import com.wingedsheep.engine.mechanics.dexterity.FlipPhysics
import com.wingedsheep.engine.mechanics.dexterity.FlipTableCard
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where the AI throws a dexterity flip (Chaos Orb).
 *
 * The AI's hand is as steady as the clean release zone allows, but it still scatters like anyone
 * else's, so it scores each candidate aim point by its *expected* haul: the value of the opponents'
 * permanents it would destroy minus a little more than the value of its own, averaged over a fixed
 * spread of landings drawn from the engine's own scatter model. Candidates are each counted
 * permanent and the midpoints of nearby pairs (an Orb on the seam between two cards takes both).
 * When nothing scores above zero it throws the Orb off the table rather than hurt itself.
 */
object FlipAim {
    private const val STRENGTH = (FlipPhysics.CLEAN_FROM + FlipPhysics.CLEAN_TO) / 2
    private const val OWN_LOSS_WEIGHT = 1.25
    private const val PAIR_REACH = 140.0
    private val ROTATIONS = listOf(0.0, 45.0, 90.0, 135.0)

    /** [samples] landings per candidate per rotation; fewer for playouts, more for the real move. */
    fun respond(state: GameState, decision: FlipOntoBattlefieldDecision, playerId: EntityId, samples: Int = 16): FlipThrowResponse {
        val table = decision.table
        val projected = state.projectedState
        val worth = table.cards.filter { it.eligible }.associate { card ->
            val value = 1.0 + (state.getEntity(card.entityId)?.get<CardComponent>()
                ?.let { BoardPresence.permanentValue(state, projected, card.entityId, it) } ?: 0.0)
            val mine = card.controllerId == playerId || !state.isOpponentTo(card.controllerId, playerId)
            card.entityId to if (mine) -OWN_LOSS_WEIGHT * value else value
        }
        val targets = table.cards.filter { (worth[it.entityId] ?: 0.0) > 0.0 }
        val candidates = targets.map { centre(it) } + targets.flatMapIndexed { i, a ->
            targets.drop(i + 1).mapNotNull { b ->
                val (ax, ay) = centre(a)
                val (bx, by) = centre(b)
                if (hypot(ax - bx, ay - by) <= PAIR_REACH) (ax + bx) / 2 to (ay + by) / 2 else null
            }
        }

        val offsets = spread(samples)
        fun expected(x: Double, y: Double): Double {
            val sigma = FlipPhysics.scatter(table, x, y, STRENGTH)
            var total = 0.0
            for ((dx, dy) in offsets) for (rotation in ROTATIONS) {
                val landing = FlipLanding(x + dx * sigma, y + dy * sigma, rotation, halfTurns = 2)
                for (card in table.cards) {
                    val w = worth[card.entityId] ?: continue
                    if (FlipPhysics.overlaps(landing, card)) total += w
                }
            }
            return total / (offsets.size * ROTATIONS.size)
        }

        val best = candidates.map { it to expected(it.first, it.second) }.maxByOrNull { it.second }
        val (x, y) = if (best != null && best.second > 0.0) best.first else table.width / 2 to -2 * table.height
        return FlipThrowResponse(decision.id, x, y, STRENGTH)
    }

    private fun centre(card: FlipTableCard) = card.x to (card.hitTop + card.hitBottom) / 2

    /** A deterministic, roughly unit-normal spread of [n] points (a sunflower over the normal's CDF). */
    private fun spread(n: Int): List<Pair<Double, Double>> = List(n) { i ->
        val u = (i + 0.5) / n
        val r = sqrt(-2.0 * kotlin.math.ln(1.0 - u))
        val angle = i * PI * (3.0 - sqrt(5.0))
        r * cos(angle) to r * sin(angle)
    }
}

package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlin.math.ln

/**
 * What a creature's power and toughness are worth *against the table it is facing* —
 * [CreatureValuation.statsReadTheBoard].
 *
 * The linear `power × 1.0 + toughness × 0.4` makes a 1000/1000 worth fifty 20/20s. It is not: both
 * kill anything they fight, both win the game in a hit or two, and both die to the same Murder.
 * Stats are worth what they *do*, and what they do is cross thresholds:
 *
 * - **Diminishing returns** ([soften]). Power is linear up to [POWER_KNEE] and toughness up to
 *   [TOUGHNESS_KNEE]; past the knee each further point is worth less (a logarithmic tail), so a
 *   1000/1000 prices at a little under twice a 20/20 rather than fifty times. The knees are fixed
 *   numbers on purpose: a knee that moved with the opponent's life total or board would make
 *   *damaging the opponent* or *killing their creature* lower the value of our own big creature,
 *   and the search would learn to avoid both.
 * - **Board thresholds** ([thresholdBonus]). A 5/5 facing 4/4s kills whatever blocks it and
 *   survives whatever it blocks; a 4/4 facing the same board only trades. Each threshold — power at
 *   least the biggest opposing toughness ([OUTSIZES]), toughness above the biggest opposing power
 *   ([OUTLASTS], void against deathtouch), toughness above the biggest size-limited removal the
 *   opponent has *shown* ([OUT_OF_REACH]) — is a step in value. Both combat thresholds read the
 *   leaf's board, and both move the right way: removing the creature that walls ours raises our
 *   creatures' worth, which is exactly the removal target we want the search to find. An empty
 *   opposing board satisfies both, so killing their last creature never costs us a bonus.
 *
 * "Shown" removal is public information only: damage and -N/-N spells in the opponent's graveyard
 * and repeatable ones on their battlefield (a Prodigal Sorcerer), read through
 * [com.wingedsheep.ai.engine.knowledge.CardIntent.removalReach]. It is read at the decision *root*
 * ([StatContext.shownReachAt]) rather than at each leaf: read at the leaf, casting our own Shock
 * would put it in our graveyard and raise every big opposing creature's value mid-line — the AI
 * would be charged for casting removal. Destruction and exile have no reach, and toughness is no
 * defence against them, so they leave this term alone.
 */
object StatRelevance {
    /** Power past which each further point is worth less — two hits of it kill from 20. */
    const val POWER_KNEE = 10

    /** Toughness past which each further point is worth less — few attackers or burn spells reach it. */
    const val TOUGHNESS_KNEE = 8

    /** How slowly the tail flattens: the first point past the knee is still worth almost a full point. */
    private const val TAIL = 3.0

    /** Power at least the biggest opposing toughness: it kills anything that blocks it or that it blocks. */
    const val OUTSIZES = 1.0

    /** Toughness above the biggest opposing power: no creature across the table kills it in a fight. */
    const val OUTLASTS = 1.0

    /** Toughness above the biggest damage or -N/-N removal the opponent has shown. */
    const val OUT_OF_REACH = 0.75

    /** [stat] with diminishing returns past [knee]. Identity at or below the knee. */
    fun soften(stat: Int, knee: Int): Double =
        if (stat <= knee) stat.toDouble() else knee + TAIL * ln(1.0 + (stat - knee) / TAIL)

    fun softPower(power: Int): Double = soften(power, POWER_KNEE)

    fun softToughness(toughness: Int): Double = soften(toughness, TOUGHNESS_KNEE)

    /**
     * The step bonuses for a creature with [power]/[toughness] and [keywords] facing [context].
     * Its own deathtouch kills whatever it fights, and indestructible survives whatever it fights,
     * whatever the numbers say.
     */
    fun thresholdBonus(power: Int, toughness: Int, keywords: Set<String>, context: StatContext): Double {
        val indestructible = Keyword.INDESTRUCTIBLE.name in keywords
        var bonus = 0.0
        if (power > 0 && (power >= context.opposingToughness || Keyword.DEATHTOUCH.name in keywords)) {
            bonus += OUTSIZES
        }
        if (indestructible || (!context.opposingDeathtouch && toughness > context.opposingPower)) {
            bonus += OUTLASTS
        }
        val reach = context.shownReach
        if (reach != null && (indestructible || toughness > reach)) bonus += OUT_OF_REACH
        return bonus
    }
}

/**
 * The opposing side's numbers that [StatRelevance.thresholdBonus] compares a creature against.
 *
 * @property opposingPower the biggest power among the opposing creatures, 0 when there are none.
 * @property opposingToughness the biggest toughness among them, 0 when there are none.
 * @property opposingDeathtouch whether any of them has deathtouch and power to use it.
 * @property shownReach the biggest size-limited removal the opponents have shown at the decision
 *   root, or null when they have shown none (or the evaluation was not rooted).
 */
class StatContext(
    val opposingPower: Int,
    val opposingToughness: Int,
    val opposingDeathtouch: Boolean,
    val shownReach: Int?,
) {
    companion object {
        /** The context for creatures controlled by [controller], read off this leaf's board. */
        fun facing(
            state: GameState,
            projected: ProjectedState,
            controller: EntityId,
            shownReach: Map<EntityId, Int>?,
        ): StatContext {
            var power = 0
            var toughness = 0
            var deathtouch = false
            var reach: Int? = null
            for (opponent in state.getOpponents(controller)) {
                for (id in projected.getBattlefieldControlledBy(opponent)) {
                    if (!projected.isCreature(id)) continue
                    val p = projected.getPower(id) ?: 0
                    power = maxOf(power, p)
                    toughness = maxOf(toughness, projected.getToughness(id) ?: 0)
                    if (p > 0 && Keyword.DEATHTOUCH.name in projected.getKeywords(id)) deathtouch = true
                }
                shownReach?.get(opponent)?.let { reach = maxOf(reach ?: 0, it) }
            }
            return StatContext(power, toughness, deathtouch, reach)
        }

        /**
         * Per player, the biggest [com.wingedsheep.ai.engine.knowledge.CardIntent.removalReach] they
         * have shown at [root]: instants and sorceries in their graveyard, and repeatable removal on
         * their battlefield. Players who have shown none are absent.
         */
        fun shownReachAt(root: GameState, intents: IntentCatalog): Map<EntityId, Int> {
            if (intents === IntentCatalog.NONE) return emptyMap()
            val projected = root.projectedState
            val result = HashMap<EntityId, Int>()
            for (player in root.turnOrder) {
                var best: Int? = null
                for (id in root.getZone(player, Zone.GRAVEYARD)) {
                    val card = root.getEntity(id)?.get<CardComponent>() ?: continue
                    if (!card.typeLine.isInstant && !card.typeLine.isSorcery) continue
                    val intent = intents.forName(card.name) ?: continue
                    if (IntentTag.REMOVAL !in intent.tags) continue
                    intent.removalReach?.let { best = maxOf(best ?: 0, it) }
                }
                for (id in projected.getBattlefieldControlledBy(player)) {
                    val container = root.getEntity(id) ?: continue
                    val card = container.get<CardComponent>() ?: continue
                    for (intent in intents.forPermanent(container, card.name)) {
                        if (!intent.repeatable || IntentTag.REMOVAL !in intent.tags) continue
                        intent.removalReach?.let { best = maxOf(best ?: 0, it) }
                    }
                }
                best?.let { result[player] = it }
            }
            return result
        }
    }
}

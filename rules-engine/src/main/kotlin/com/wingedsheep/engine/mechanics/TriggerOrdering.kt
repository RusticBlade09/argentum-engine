package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OrderTriggersDecision
import com.wingedsheep.engine.core.TriggerOrderContinuation
import com.wingedsheep.engine.core.TriggerToOrder
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.stack.triggerIdentityFromCurrentCardDefinition
import com.wingedsheep.engine.state.faceDownDisplayName
import com.wingedsheep.engine.view.projection.StackTextRenderer
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * CR 603.3b: each player, in APNAP order, puts the triggered abilities they control on the stack
 * in any order they choose. A player who turned automatic ordering off
 * ([GameState.manualTriggerOrdering]) is asked for that order, before any of the abilities chooses
 * targets (CR 603.3d); everyone else's abilities keep the order they triggered in.
 *
 * The [com.wingedsheep.engine.core.Settler] asks once per wave, so each of 603.3b's two parts (the
 * abilities that trigger on an attack-caused ability triggering come second) gets its own question.
 * The wave waits, already in APNAP order, in [GameState.triggersBeingPlaced]: an answer only
 * rearranges its player's part of it, and the drain after the last answer places it unchanged.
 */
object TriggerOrdering {

    const val PROMPT = "Order your triggered abilities"

    /**
     * Ask the first player after [after] in the waiting wave's APNAP order who orders their own
     * triggers and has a choice to make, or null when nobody is left to ask.
     */
    fun ask(state: GameState, after: EntityId? = null): ExecutionResult? {
        val wave = state.triggersBeingPlaced
        val players = wave.map { it.controllerId }.distinct()
        val player = players.drop(if (after == null) 0 else players.indexOf(after) + 1).firstOrNull { player ->
            state.ordersOwnTriggers(player) &&
                state.getEntity(player)?.has<PlayerLostComponent>() != true &&
                hasChoice(state, wave.filter { it.controllerId == player })
        } ?: return null
        val resolutionOrder = wave.filter { it.controllerId == player }.asReversed()
        return state.suspendForDecision(
            { id ->
                OrderTriggersDecision(
                    id = id,
                    playerId = player,
                    prompt = PROMPT,
                    context = DecisionContext(phase = DecisionPhase.TRIGGER),
                    triggers = resolutionOrder.map { describe(state, it) }
                )
            },
            TriggerOrderContinuation(player)
        )
    }

    /**
     * Put [player]'s abilities in the waiting wave in [resolutionOrder]: positions in their
     * [OrderTriggersDecision.triggers], the first to resolve first and so to be placed last. Their
     * slots in the wave stay where they are, so the other players' abilities don't move.
     */
    fun reorder(state: GameState, player: EntityId, resolutionOrder: List<Int>): GameState {
        val wave = state.triggersBeingPlaced
        val slots = wave.indices.filter { wave[it].controllerId == player }
        val asked = slots.map { wave[it] }.asReversed()
        val placement = resolutionOrder.map { asked[it] }.asReversed()
        val reordered = wave.toMutableList()
        slots.forEachIndexed { i, slot -> reordered[slot] = placement[i] }
        return state.copy(triggersBeingPlaced = reordered)
    }

    /**
     * Whether ordering [triggers] can change anything. Copies of one card's ability that choose no
     * targets or modes are interchangeable, so every order of them is the same stack — the guard
     * Magic Online uses to stack identical triggers without asking. It also keeps identical
     * triggers contiguous, which the batch may-question relies on.
     */
    internal fun hasChoice(state: GameState, triggers: List<PendingTrigger>): Boolean {
        if (triggers.size < 2) return false
        val identities = triggers.map {
            state.triggerIdentityFromCurrentCardDefinition(it.sourceId, it.ability.id) ?: return true
        }
        return identities.distinct().size > 1 ||
            triggers.any { it.ability.allTargetRequirements.isNotEmpty() || it.ability.effect is ModalEffect }
    }

    private fun describe(state: GameState, trigger: PendingTrigger): TriggerToOrder {
        val ability = trigger.ability
        val faceDown = faceDownDisplayName(state, trigger.sourceId) != null
        return TriggerToOrder(
            sourceId = trigger.sourceId,
            sourceName = trigger.sourceName,
            text = ability.descriptionOverride ?: StackTextRenderer.effectDisplayText(
                ability.effect, StackTextRenderer.selfNounFor(state, trigger.sourceId)
            ),
            imageUri = if (faceDown) null else state.getEntity(trigger.sourceId)?.get<CardComponent>()?.imageUri,
            triggeringEntityId = trigger.triggerContext.triggeringEntityId?.takeIf { it != trigger.sourceId },
            choosesTargets = ability.allTargetRequirements.isNotEmpty()
        )
    }
}

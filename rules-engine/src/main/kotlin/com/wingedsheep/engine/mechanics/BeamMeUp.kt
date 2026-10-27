package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.liveStaticAbilities
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveCastKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility

/** Shared permission, pricing and return pools for printed and continuously granted beam me up. */
object BeamMeUp {
    data class Offer(val cost: ManaCost, val creatures: List<EntityId>)

    fun selected(state: GameState, action: CastSpell, registry: CardRegistry, predicates: PredicateEvaluator): Offer? =
        options(state, action.cardId, action.playerId, registry, predicates).getOrNull(action.beamMeUpAbility)

    fun options(state: GameState, cardId: EntityId, playerId: EntityId, registry: CardRegistry, predicates: PredicateEvaluator): List<Offer> {
        if (cardId !in state.getGraveyard(playerId)) return emptyList()
        val card = state.getEntity(cardId)?.get<CardComponent>() ?: return emptyList()
        if (!card.typeLine.isInstant && !card.typeLine.isSorcery) return emptyList()
        val definition = registry.getCard(card.cardDefinitionId) ?: return emptyList()
        val projected = state.projectedState
        val creatures = state.controlledBattlefield(playerId).filter {
            projected.isCreature(it) && !projected.hasKeyword(it, AbilityFlag.CANT_BE_BEAMED_UP)
        }
        return buildList {
            definition.keywordAbilities.filterIsInstance<KeywordAbility.BeamMeUp>().forEach {
                add(Offer(it.cost, creatures))
            }
            for (sourceId in state.controlledBattlefield(playerId)) {
                if (projected.hasLostAllAbilities(sourceId)) continue
                val source = state.getEntity(sourceId)?.faceUpCardDefinition(registry) ?: continue
                for (grant in source.liveStaticAbilities(state, sourceId).filterIsInstance<GraveyardCardsHaveCastKeyword>()) {
                    if (grant.keyword != Keyword.BEAM_ME_UP) continue
                    if (grant.cost == null && definition.hasNoManaCost) continue
                    if (grant.duringYourTurnOnly && !state.isActiveTurnFor(playerId)) continue
                    val context = EffectContext(sourceId = sourceId, controllerId = playerId)
                    val condition = grant.condition
                    if (condition != null && !predicates.conditions.evaluate(state, condition, context)) continue
                    val predicateContext = PredicateContext(sourceId = sourceId, controllerId = playerId)
                    if (!predicates.matches(state, projected, cardId, grant.filter, predicateContext)) continue
                    add(Offer(grant.cost ?: card.manaCost, creatures.filter {
                        predicates.matches(state, projected, it, grant.returnFilter, predicateContext)
                    }))
                }
            }
        }
    }
}

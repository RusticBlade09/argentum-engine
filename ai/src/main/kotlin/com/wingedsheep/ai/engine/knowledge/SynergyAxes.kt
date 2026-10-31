package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * A resource one card makes and another card cashes in: the unit of synergy.
 *
 * Every axis has a **payoff** side (a card that rewards the event happening — Blood Artist, Ajani's
 * Pridemate, a prowess creature) and a **feeder** side (a card that makes the event happen — a sac
 * outlet, a lifelinker, an instant). A payoff with nothing to feed it is a vanilla body; the same
 * card beside three feeders is an engine. [SynergyAxes] reads both sides off a card's structure.
 */
enum class SynergyAxis {
    /** Creatures dying or being sacrificed. Fed by sacrifice outlets and creature-token makers. */
    DEATH,

    /** Tokens entering or being created. Fed by token makers. */
    TOKENS,

    /** Creatures entering under your control. Fed by creatures and creature-token makers. */
    CREATURES_ENTER,

    /** You gaining life. Fed by life gain and lifelink. */
    LIFEGAIN,

    /** You casting instants and sorceries (or noncreature spells). Fed by instants and sorceries. */
    SPELLS,

    /** +1/+1 counters being put on permanents. Fed by anything that puts them. */
    PLUS_COUNTERS,

    /** Cards in your graveyard. Rewarded per [GraveyardSynergy]; fed by self-mill and surveil. */
    GRAVEYARD,
}

/** Which axes a card [feeds] and which it [rewards]. Empty on both sides for most cards. */
data class SynergyProfile(val feeds: Set<SynergyAxis>, val rewards: Set<SynergyAxis>) {
    val isEmpty: Boolean get() = feeds.isEmpty() && rewards.isEmpty()

    /** Axes on which this card and [other] complete each other, in either direction. */
    fun sharedWith(other: SynergyProfile): Set<SynergyAxis> =
        (rewards intersect other.feeds) + (feeds intersect other.rewards)

    companion object {
        val NONE = SynergyProfile(emptySet(), emptySet())
    }
}

/**
 * The one structural reading of "what does this card make, and what does it cash in" that every
 * synergy consumer shares — the in-game evaluator's payoff term
 * ([com.wingedsheep.ai.engine.evaluation.SynergyPresence]) and the Commander deck generator's theme
 * score ([com.wingedsheep.ai.engine.deck.CommanderSynergy]).
 *
 * It is read off the card's SDK structure, never its name or its Oracle text:
 *
 *  - **Rewards** are triggered abilities whose [EventPattern] watches an axis's event happening to
 *    *something other than the card itself* ([TriggerBinding.SELF] "when this dies" is not a payoff),
 *    and not scoped to an opponent's objects. Prowess reads as a [SynergyAxis.SPELLS] payoff, and
 *    [GraveyardSynergy.isPayoff] is the [SynergyAxis.GRAVEYARD] payoff reading — that axis is
 *    graveyard-shaped enough (keywords, cast-from-graveyard, threshold counts) that it keeps its own
 *    reader.
 *  - **Feeds** come from what the card's effects do ([EffectWalker] leaves, the same walk
 *    [CardIntentAnalyzer] tags with), its activation and additional costs (a sacrifice), its
 *    keywords (lifelink) and its type line (a creature enters; an instant is cast).
 *
 * Deliberately narrow: static abilities that reward a resource ("creatures you control with +1/+1
 * counters have trample") and keyword mechanics that make tokens on their own (afterlife,
 * fabricate) are not read, so they score as no synergy rather than a guessed one. A false negative
 * costs a missed bonus; a false positive would pull a deck toward cards that do nothing together.
 *
 * Memoized per card name, like [CardIntentAnalyzer] and [GraveyardSynergy].
 */
object SynergyAxes {

    private val cache = ConcurrentHashMap<String, SynergyProfile>()

    fun of(card: CardDefinition): SynergyProfile = cache.getOrPut(card.name) { compute(card) }

    private fun compute(card: CardDefinition): SynergyProfile {
        val scripts = listOf(card.script) + card.cardFaces.map { it.script }
        val rewards = buildSet {
            scripts.flatMap { it.triggeredAbilities }.forEach { ability ->
                addAll(rewardsOf(ability.trigger, ability.binding))
            }
            if (Keyword.PROWESS in card.keywords) add(SynergyAxis.SPELLS)
            if (GraveyardSynergy.isPayoff(card)) add(SynergyAxis.GRAVEYARD)
        }
        val feeds = buildSet {
            scripts.forEach { addAll(feedsOf(it)) }
            if (card.typeLine.isCreature) add(SynergyAxis.CREATURES_ENTER)
            if (card.typeLine.isInstant || card.typeLine.isSorcery) add(SynergyAxis.SPELLS)
            if (Keyword.LIFELINK in card.keywords) add(SynergyAxis.LIFEGAIN)
        }
        return if (rewards.isEmpty() && feeds.isEmpty()) SynergyProfile.NONE else SynergyProfile(feeds, rewards)
    }

    // ── Rewards ────────────────────────────────────────────────────────────────

    private fun rewardsOf(pattern: EventPattern, binding: TriggerBinding): Set<SynergyAxis> = when (pattern) {
        is EventPattern.AnyOf -> pattern.events.flatMapTo(mutableSetOf()) { rewardsOf(it, binding) }
        is EventPattern.CountersPlacedEvent ->
            // "Whenever +1/+1 counters are put on this creature" rewards a feeder aimed at it, so the
            // self binding still counts here — unlike a death or an entry, which happen once.
            setOfNotNull(SynergyAxis.PLUS_COUNTERS.takeIf { pattern.counterType == CounterType.PLUS_ONE_PLUS_ONE })
        else -> if (binding == TriggerBinding.SELF) emptySet() else otherRewards(pattern)
    }

    private fun otherRewards(pattern: EventPattern): Set<SynergyAxis> = when (pattern) {
        is EventPattern.ZoneChangeEvent -> when {
            opponents(pattern.filter) -> emptySet()
            pattern.from == Zone.BATTLEFIELD && pattern.to == Zone.GRAVEYARD && couldBeCreature(pattern.filter) ->
                setOf(SynergyAxis.DEATH)
            pattern.to == Zone.BATTLEFIELD -> enterAxes(pattern.filter)
            else -> emptySet()
        }
        is EventPattern.PermanentsEnteredEvent ->
            if (opponents(pattern.filter)) emptySet() else enterAxes(pattern.filter)
        is EventPattern.CreaturesYouControlDiedEvent ->
            if (opponents(pattern.filter)) emptySet() else setOf(SynergyAxis.DEATH)
        is EventPattern.PermanentsSacrificedEvent ->
            if (pattern.sacrificedBy == Player.You) setOf(SynergyAxis.DEATH) else emptySet()
        is EventPattern.TokenCreationEvent ->
            if (pattern.controller == Player.You) setOf(SynergyAxis.TOKENS) else emptySet()
        is EventPattern.LifeGainEvent ->
            if (pattern.player == Player.You) setOf(SynergyAxis.LIFEGAIN) else emptySet()
        is EventPattern.SpellCastEvent ->
            if (pattern.player == Player.You && rewardsSpells(pattern.spellFilter)) setOf(SynergyAxis.SPELLS)
            else emptySet()
        else -> emptySet()
    }

    private fun enterAxes(filter: GameObjectFilter): Set<SynergyAxis> = when {
        predicates(filter).any { it == CardPredicate.IsToken } -> setOf(SynergyAxis.TOKENS)
        couldBeCreature(filter) && predicates(filter).any { it == CardPredicate.IsCreature } ->
            setOf(SynergyAxis.CREATURES_ENTER)
        else -> emptySet()
    }

    /** "Whenever you cast an instant or sorcery / a noncreature spell" — not "whenever you cast a spell". */
    private fun rewardsSpells(filter: GameObjectFilter): Boolean =
        predicates(filter).any { it in SPELL_PREDICATES }

    private fun opponents(filter: GameObjectFilter): Boolean =
        filter.controllerPredicate == ControllerPredicate.ControlledByOpponent

    /** False only when the filter names a card type that excludes creatures (a land, an artifact). */
    private fun couldBeCreature(filter: GameObjectFilter): Boolean {
        val preds = predicates(filter)
        return CardPredicate.IsCreature in preds || preds.none { it in NONCREATURE_TYPES }
    }

    /** Every card predicate the filter names, with `Or` unions ("instant or sorcery") flattened. */
    private fun predicates(filter: GameObjectFilter): List<CardPredicate> =
        (filter.cardPredicates + filter.anyOf.flatMap { it.cardPredicates }).flatMap(::flatten)

    private fun flatten(predicate: CardPredicate): List<CardPredicate> =
        if (predicate is CardPredicate.Or) predicate.predicates.flatMap(::flatten) else listOf(predicate)

    // ── Feeds ──────────────────────────────────────────────────────────────────

    private fun feedsOf(script: CardScript): Set<SynergyAxis> = buildSet {
        script.spellEffect?.let { addAll(effectFeeds(it)) }
        script.triggeredAbilities.forEach { addAll(effectFeeds(it.effect)) }
        for (ability in script.activatedAbilities.filterNot { it.isManaAbility }) {
            addAll(effectFeeds(ability.effect))
            if (sacrifices(ability.cost)) add(SynergyAxis.DEATH)
        }
        if (script.additionalCosts.any(::sacrifices)) add(SynergyAxis.DEATH)
    }

    private fun effectFeeds(effect: Effect): Set<SynergyAxis> = buildSet {
        for (leaf in EffectWalker.leaves(effect)) {
            addAll(leafFeeds(leaf))
            val tags = CardIntentAnalyzer.analyzeEffect(leaf).tags
            if (IntentTag.LIFEGAIN in tags) add(SynergyAxis.LIFEGAIN)
            if (IntentTag.TOKEN_MAKER in tags) add(SynergyAxis.TOKENS)
        }
    }

    private fun leafFeeds(leaf: Any): Set<SynergyAxis> = when (leaf) {
        // A creature token is a body that enters and a body to sacrifice.
        is CreateTokenEffect, is CreateTokenCopyOfSourceEffect, is CreateTokenCopyOfTargetEffect ->
            setOf(SynergyAxis.TOKENS, SynergyAxis.CREATURES_ENTER, SynergyAxis.DEATH)
        is AddCountersEffect -> plusCounters(leaf.counterType)
        is AddDynamicCountersEffect -> plusCounters(leaf.counterType)
        is SurveilEffect -> setOf(SynergyAxis.GRAVEYARD)
        is GatherCardsEffect -> {
            val source = leaf.source
            if (source is CardSource.TopOfLibrary && source.isMill && source.player == Player.You) {
                setOf(SynergyAxis.GRAVEYARD)
            } else {
                emptySet()
            }
        }
        else -> emptySet()
    }

    private fun plusCounters(type: CounterType): Set<SynergyAxis> =
        if (type == CounterType.PLUS_ONE_PLUS_ONE) setOf(SynergyAxis.PLUS_COUNTERS) else emptySet()

    private fun sacrifices(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Atom -> cost.atom is CostAtom.Sacrifice
        is AbilityCost.Composite -> cost.costs.any(::sacrifices)
        else -> false
    }

    private fun sacrifices(cost: AdditionalCost): Boolean = when (cost) {
        is AdditionalCost.Atom -> cost.atom is CostAtom.Sacrifice
        is AdditionalCost.Composite -> cost.steps.any(::sacrifices)
        else -> false
    }

    private val SPELL_PREDICATES: Set<CardPredicate> =
        setOf(CardPredicate.IsInstant, CardPredicate.IsSorcery, CardPredicate.IsNoncreature)

    private val NONCREATURE_TYPES: Set<CardPredicate> = setOf(
        CardPredicate.IsLand, CardPredicate.IsArtifact, CardPredicate.IsEnchantment, CardPredicate.IsPlaneswalker,
        CardPredicate.IsBattle, CardPredicate.IsInstant, CardPredicate.IsSorcery, CardPredicate.IsNoncreature,
    )
}

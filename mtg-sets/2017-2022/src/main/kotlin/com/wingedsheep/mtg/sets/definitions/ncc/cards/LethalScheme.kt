package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lethal Scheme — {2}{B}{B} Instant (New Capenna Commander #36).
 *
 * Convoke
 * Destroy target creature or planeswalker. Each creature that convoked this spell connives.
 *
 * "Each creature that convoked this spell" (CR 702.51c) is the Zephyr Singer group,
 * `GameObjectFilter.Creature.thatConvokedSource()`, read off the spell's own convoke record while
 * it resolves on the stack. Each one still on the battlefield connives as itself (draw, discard, a
 * +1/+1 counter on it for a nonland discard).
 *
 * A creature that convoked the spell but has since left the battlefield — including one destroyed
 * by this very spell — still connives (2022-04-29 ruling): its controller draws and discards, and
 * the counter has nowhere to go. That leg is the convoked count (which keeps counting departed
 * creatures) minus the convokers still present, each a plain draw-then-discard.
 */
val LethalScheme = card("Lethal Scheme") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Destroy target creature or planeswalker. Each creature that convoked this spell connives. " +
        "(Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on that creature.)"

    keywords(Keyword.CONVOKE)

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        val convokers = GameObjectFilter.Creature.thatConvokedSource()
        effect = Effects.Destroy(t) then
            Effects.ForEachInGroup(
                GroupFilter(convokers),
                Effects.Connive(EffectTarget.IterationEntity)
            ) then
            Effects.Repeat(
                DynamicAmounts.convokedCreatureCount() -
                    DynamicAmounts.battlefield(Player.Each, convokers).count(),
                Patterns.Hand.loot()
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65864680-9520-4eb3-9774-fa478e54a290.jpg?1783923366"
        ruling("2022-04-29", "When multiple creatures are instructed to connive simultaneously, they connive one at a time in the order of their controller's choice.")
        ruling("2022-04-29", "If a resolving spell or ability instructs a specific creature to connive but that creature has left the battlefield, the creature still connives. If you discard a nonland card this way, you won't put a +1/+1 counter on anything. Abilities that trigger \"when [that creature] connives\" will trigger.")
    }
}

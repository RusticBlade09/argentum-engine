package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Overseer of Vault 76 — Fallout #19.
 * {2}{W} · Legendary Creature — Human Advisor · 3/3
 *
 * First Contact — Whenever Overseer of Vault 76 or another creature you control with power 3 or
 * less enters, put a quest counter on Overseer of Vault 76.
 * At the beginning of combat on your turn, you may remove three quest counters from among
 * permanents you control. When you do, put a +1/+1 counter on each creature you control and they
 * gain vigilance until end of turn.
 *
 * **First Contact.** "with power 3 or less" qualifies only "another creature", so Overseer's own
 * entry counts whatever its power (an anthem can make it enter as a 4/4). That is two halves: a
 * self-enters trigger and an `another(...)` enters trigger with the power filter — together one
 * trigger per entering object, so Overseer entering alongside other small creatures triggers once
 * for each of them (the 2024-03-08 ruling).
 *
 * **The combat ability.** "You may remove three quest counters from among permanents you control"
 * is the shared counter-removal cost atom paid at resolution (`Costs.pay.RemoveCounters`, which the
 * player distributes across their permanents; an empty selection declines, and with fewer than
 * three quest counters available nothing is asked). Exactly three — the ruling says you can't
 * remove more to multiply the effect. [SuccessCriterion.CountersRemoved] gates the payoff on the
 * removal actually having happened, and the payoff is a reflexive ability ("when you
 * do"), a separate stack object. Its group is locked as it resolves, and "they gain vigilance" is
 * the same creatures that got counters.
 */
val OverseerOfVault76 = card("Overseer of Vault 76") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Creature — Human Advisor"
    power = 3
    toughness = 3
    oracleText = "First Contact — Whenever Overseer of Vault 76 or another creature you control with power 3 or less enters, put a quest counter on Overseer of Vault 76.\n" +
        "At the beginning of combat on your turn, you may remove three quest counters from among permanents you control. When you do, put a +1/+1 counter on each creature you control and they gain vigilance until end of turn."

    // First Contact — Overseer of Vault 76 itself entering.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
        description = "First Contact — Whenever Overseer of Vault 76 or another creature you control with power " +
            "3 or less enters, put a quest counter on Overseer of Vault 76."
    }

    // First Contact — another creature you control with power 3 or less entering.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().powerAtMost(3)).enters()
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
        description = "First Contact — Whenever Overseer of Vault 76 or another creature you control with power " +
            "3 or less enters, put a quest counter on Overseer of Vault 76."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.IfYouDo(
            action = Effects.PayOrSuffer(
                cost = Costs.pay.RemoveCounters(3, CounterType.QUEST, GameObjectFilter.Permanent),
                suffer = Effects.Nothing,
                consequenceDescription = "don't remove them",
            ),
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                reflexiveEffect = Effects.ForEachInGroup(
                    filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity) then
                        Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity, Duration.EndOfTurn)
                ),
                descriptionOverride = "Put a +1/+1 counter on each creature you control and they gain " +
                    "vigilance until end of turn.",
            ),
            successCriterion = SuccessCriterion.CountersRemoved,
            descriptionOverride = "You may remove three quest counters from among permanents you control. " +
                "When you do, put a +1/+1 counter on each creature you control and they gain vigilance " +
                "until end of turn.",
        )
        description = "At the beginning of combat on your turn, you may remove three quest counters from among " +
            "permanents you control. When you do, put a +1/+1 counter on each creature you control and they " +
            "gain vigilance until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35848917-d658-4c3b-b675-4f0185f76dc4.jpg?1783912418"
        ruling("2024-03-08", "If Overseer of Vault 76 and one or more other creatures with power 3 or less enter the battlefield under your control at the same time, Overseer of Vault 76's first ability will trigger for each of them.")
        ruling("2024-03-08", "Overseer of Vault 76's last ability lets you choose to remove exactly three quest counters from among permanents you control when it resolves. You can't remove more quest counters to multiply the effect.")
    }
}

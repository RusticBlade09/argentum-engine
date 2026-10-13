package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yes Man, Personal Securitron — Fallout #29.
 * {2}{W} · Legendary Artifact Creature — Robot · 2/2
 *
 * {T}: Target opponent gains control of Yes Man. When they do, you draw two cards and put a quest
 * counter on Yes Man. Activate only during your turn.
 * Wild Card — When Yes Man leaves the battlefield, its owner creates a tapped 1/1 white Soldier
 * creature token for each quest counter on it.
 *
 * **The gift.** "When they do" is a reflexive ability that triggers only if the opponent
 * really gains control — the 2024-03-08 ruling: if Yes Man has left the battlefield by then, you
 * don't draw. So the control change is the action of an `IfYouDo` gated on
 * [SuccessCriterion.ControlChanged], and its branch emits the reflexive payoff. "You" in the
 * payoff is the player who activated the ability (the reflexive ability's controller), not Yes
 * Man's new controller; the quest counter goes on Yes Man wherever it now sits.
 *
 * **Wild Card.** The quest counters are read as last-known information (Yes Man is already gone),
 * and the tokens go to Yes Man's *owner* — which is not the trigger's controller when Yes Man
 * leaves while an opponent controls it.
 */
val YesManPersonalSecuritron = card("Yes Man, Personal Securitron") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Artifact Creature — Robot"
    power = 2
    toughness = 2
    oracleText = "{T}: Target opponent gains control of Yes Man. When they do, you draw two cards and put a quest counter on Yes Man. Activate only during your turn.\n" +
        "Wild Card — When Yes Man leaves the battlefield, its owner creates a tapped 1/1 white Soldier creature token for each quest counter on it."

    activatedAbility {
        cost = Costs.Tap
        val opponent = target(Targets.Opponent)
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        effect = Effects.IfYouDo(
            action = Effects.GiveControl(permanent = EffectTarget.Self, newController = opponent),
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                reflexiveEffect = Effects.DrawCards(2) then
                    Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self),
                descriptionOverride = "You draw two cards and put a quest counter on Yes Man.",
            ),
            successCriterion = SuccessCriterion.ControlChanged,
            descriptionOverride = "Target opponent gains control of Yes Man. When they do, you draw two cards " +
                "and put a quest counter on Yes Man.",
        )
        description = "{T}: Target opponent gains control of Yes Man. When they do, you draw two cards and put " +
            "a quest counter on Yes Man. Activate only during your turn."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.CreateToken(
            count = DynamicAmounts.lastKnownSourceCounters(CounterType.QUEST),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            controller = EffectTarget.PlayerRef(Player.OwnerOfSource),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/a/f/af191656-5e62-440f-b63a-705cfed314e7.jpg?1783912045",
        )
        description = "Wild Card — When Yes Man leaves the battlefield, its owner creates a tapped 1/1 white " +
            "Soldier creature token for each quest counter on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e2fac8c-a574-4414-ac68-632fc822ddbb.jpg?1783912412"
        ruling("2024-03-08", "If Yes Man isn't on the battlefield as its first ability resolves, or if the target opponent is an illegal target as the ability tries to resolve, nothing will happen. You won't draw two cards, because that effect is part of a reflexive triggered ability that triggers only if the target opponent gains control of Yes Man.")
        ruling("2024-03-08", "When Yes Man's controller leaves the game, if that player wasn't its owner, the effect giving that player control of Yes Man ends. Yes Man will return to the control of the player still in the game who most recently controlled it, ignoring any control effects whose durations have already expired.")
        ruling("2024-03-08", "When Yes Man's owner leaves the game, Yes Man leaves the game along with that player. It's just doing what it's told.")
    }
}

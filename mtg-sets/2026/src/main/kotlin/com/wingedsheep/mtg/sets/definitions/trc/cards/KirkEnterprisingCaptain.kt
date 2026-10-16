package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step

/**
 * Kirk, Enterprising Captain
 * {3}{R}
 * Legendary Creature — Human Officer
 * 4/3
 *
 * "At the beginning of combat on your turn, if creatures you control have total
 * power 8 or greater, ... (something happens)" copied from Surrak, the Hunt Caller
 *
 * "(when something happens)... exile the top card of your library.
 *  You may play that card this turn" copied from Laelia, the Blade Reforged
 */
val KirkEnterprisingCaptain = card("Kirk, Enterprising Captain") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Officer"
    oracleText = "Menace (This creature can’t be blocked " +
            "except by two or more creatures.) " +
            "Whenever Kirk attacks while creatures " +
            "you control have total power 8 or " +
            "greater, exile the top card of your " +
            "library. You may play that card this turn."
    power = 4
    toughness = 3

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.TopOfLibrary(1))
            exile(exiledCard)
            run(Effects.GrantMayPlayFromExile(exiledCard))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "198"
        artist = "Nathaniel Himawan"
        flavorText = "\"Risk is our business.\""
        imageUri = "https://cards.scryfall.io/display/front/e/1/e1d339e1-3433-4630-965e-1841e46b40a4.webp?1784036716"
    }
}

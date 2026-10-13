package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.effects.EmitExploitedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Fallout #98. Granting exploit includes both its keyword and its sacrifice trigger. */
val ColonelAutumn = card("Colonel Autumn") {
    manaCost = "{1}{W}{B}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Lifelink\nExploit (When this creature enters, you may sacrifice a creature.)\n" +
        "Other legendary creatures you control have exploit.\n" +
        "Whenever a creature you control exploits a creature, put a +1/+1 counter on each creature you control."

    keywords(Keyword.LIFELINK)
    exploit()

    val otherLegends = GroupFilter(GameObjectFilter.Creature.legendary().youControl(), excludeSelf = true)
    staticAbility {
        ability = GrantKeyword(Keyword.EXPLOIT, otherLegends)
    }
    staticAbility {
        ability = GrantTriggeredAbility(
            filter = otherLegends,
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.enters()
                effect = Effects.ReflexiveTrigger(
                    action = Effects.SacrificeOwn(GameObjectFilter.Creature) then EmitExploitedEventEffect,
                    optional = true,
                    reflexiveEffect = Effects.Nothing,
                    descriptionOverride = "You may sacrifice a creature",
                )
                description = "Exploit (When this creature enters, you may sacrifice a creature.)"
            },
        )
    }

    triggeredAbility {
        trigger = Triggers.you.exploits()
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "Whenever a creature you control exploits a creature, put a +1/+1 counter on each creature you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Daniel Romanovsky"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca0c3e5a-64f3-40b5-8080-1c53be45aa40.jpg?1783912390"
        ruling("2024-03-08", "You can sacrifice the creature with exploit if it’s still on the battlefield. If it has an ability that triggers when it exploits a creature (or, in this case, when any creature you control exploits a creature), that ability will trigger.")
        ruling("2024-03-08", "If Colonel Autumn isn’t on the battlefield as an exploit ability of a creature you control resolves, Colonel Autumn’s last ability won’t trigger. You won’t get +1/+1 counters, even if you sacrifice a creature.")
    }
}

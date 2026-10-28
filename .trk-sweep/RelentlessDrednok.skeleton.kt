package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val RelentlessDrednok = card("Relentless Drednok") {
    manaCost = "{B}"
    typeLine = "Artifact Creature — Robot"
    colorIdentity = "B"
    oracleText = "Lifelink\n{2}{B}, Exile this card from your graveyard: You draw a card and lose 1 life."
    power = 1
    toughness = 1
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Edouard"
        flavorText = "The Vau N'Akat created the Watchers to observe and the Drednoks to enforce."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce538db8-373d-4554-821e-cd96e795e37a.jpg?1786005690"
    }
}

package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val TeaEarlGreyHot = card("Tea, Earl Grey, Hot") {
    manaCost = "{2}"
    typeLine = "Artifact — Food"
    oracleText = "When this artifact enters, draw a card.\n{2}, {T}, Sacrifice this artifact: Choose one —\n• This artifact deals 2 damage to target opponent.\n• You gain 3 life."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "269"
        artist = "Raymond Bonilla"
        flavorText = "Lapsang souchong for smoky body. Bergamot for sharpness. An insightful captain's cup."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/289e8ebe-ab0f-466b-a4e6-d5cf8411e724.jpg?1792591098"
    }
}

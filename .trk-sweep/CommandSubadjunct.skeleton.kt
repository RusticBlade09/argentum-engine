package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val CommandSubadjunct = card("Command Subadjunct") {
    manaCost = "{W}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "W"
    oracleText = "Artifact creatures you control have vigilance."
    power = 2
    toughness = 1
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Alex Negrea"
        flavorText = "The elegance of the Borg lies in how readily they integrate other species' solutions."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/853bf74d-0b61-43df-9018-0b40b624a9d1.jpg?1792753641"
    }
}

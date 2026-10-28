package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val PrimaryUnimatrixAdjunct = card("Primary Unimatrix Adjunct") {
    manaCost = "{3}{W}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "W"
    oracleText = "Artifact creatures you control have double strike."
    power = 2
    toughness = 3
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Jodie Muir"
        flavorText = "Each drone is defined by its role, but even authority provides no individual self. A brain is more useful than a toe, but neither is the body whole."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b94a582c-11bb-4463-a826-bd0c79366752.jpg?1792753859"
    }
}

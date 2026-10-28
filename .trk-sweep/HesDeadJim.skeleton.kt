package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val HesDeadJim = card("He's Dead, Jim") {
    manaCost = "{3}{B}"
    typeLine = "Instant"
    colorIdentity = "B"
    oracleText = "Destroy target creature. Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Crystal Fae"
        flavorText = "\"How can we be powerful enough to wipe out a planet and still be so helpless?\"\n—Dr. Leonard McCoy"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c309b0d-94b4-405e-be6c-18b407323e51.jpg?1785981492"
    }
}

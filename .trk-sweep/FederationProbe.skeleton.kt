package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val FederationProbe = card("Federation Probe") {
    manaCost = "{U}"
    typeLine = "Artifact Creature — Construct"
    colorIdentity = "U"
    oracleText = "Flying\nWhen this creature enters, choose one —\n• Target creature gets +1/+1 until end of turn.\n• Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    power = 1
    toughness = 1
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Racrufi"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f988ba84-fc78-41a2-b2f2-eebf78cee591.jpg?1785981405"
    }
}

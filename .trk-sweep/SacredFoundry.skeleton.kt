package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val SacredFoundry = card("Sacred Foundry") {
    typeLine = "Land — Mountain Plains"
    colorIdentity = "RW"
    oracleText = "({T}: Add {R} or {W}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "295"
        artist = "Jessica Rossier"
        flavorText = "Sooner or later, every Starfleet vessel passes through Spacedock, whether for resupply, repairs, or complete retrofit."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7758cc6-4e18-48a5-8720-5f42b5cd9d31.jpg?1784036842"
    }
}

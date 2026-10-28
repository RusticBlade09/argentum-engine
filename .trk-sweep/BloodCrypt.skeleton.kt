package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val BloodCrypt = card("Blood Crypt") {
    typeLine = "Land — Swamp Mountain"
    colorIdentity = "BR"
    oracleText = "({T}: Add {B} or {R}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "278"
        artist = "Jonas De Ro"
        flavorText = "The hellish mines of Remus fueled the war efforts of the Romulan Star Empire, until the Remans found a champion."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b7eb998-3ec0-4cbc-a416-5a1e3e5a7316.jpg?1784036817"
    }
}

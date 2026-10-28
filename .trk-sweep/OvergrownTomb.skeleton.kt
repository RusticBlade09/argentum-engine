package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val OvergrownTomb = card("Overgrown Tomb") {
    typeLine = "Land — Swamp Forest"
    colorIdentity = "BG"
    oracleText = "({T}: Add {B} or {G}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "289"
        artist = "Pavel Kolomeyets"
        flavorText = "The Zakdorn run Surplus Depot Zed-15, salvaging decommissioned ships to supply future Federation projects."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad7e18e2-c033-4b6c-86e8-d0e5cc824cfd.jpg?1784036836"
    }
}

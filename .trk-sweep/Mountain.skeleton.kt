package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Mountain = card("Mountain") {
    typeLine = "Basic Land — Mountain"
    colorIdentity = "R"
    oracleText = "({T}: Add {R}.)"
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "310"
        artist = "Rytis Sabaliauskas"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87908c55-4ac4-4b57-a8ee-a28640a18f0f.jpg?1792508574"
    }
}

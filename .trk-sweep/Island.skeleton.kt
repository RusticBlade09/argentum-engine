package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Island = card("Island") {
    typeLine = "Basic Land — Island"
    colorIdentity = "U"
    oracleText = "({T}: Add {U}.)"
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "308"
        artist = "Guglielmo Bonis"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c5b534a-128f-4235-8f69-646a55ae4d67.jpg?1792508563"
    }
}

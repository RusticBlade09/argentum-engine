package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Forest = card("Forest") {
    typeLine = "Basic Land — Forest"
    colorIdentity = "G"
    oracleText = "({T}: Add {G}.)"
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "311"
        artist = "Bengt Hampus Viklander"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f026379b-f00f-4897-b03f-dd164d69b925.jpg?1792508583"
    }
}

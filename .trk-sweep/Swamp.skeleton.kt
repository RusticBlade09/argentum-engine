package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Swamp = card("Swamp") {
    typeLine = "Basic Land — Swamp"
    colorIdentity = "B"
    oracleText = "({T}: Add {B}.)"
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "309"
        artist = "Rytis Sabaliauskas"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4e1965a-dbab-4edb-ada9-43898c57c5fe.jpg?1792508572"
    }
}

package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Plains = card("Plains") {
    typeLine = "Basic Land — Plains"
    colorIdentity = "W"
    oracleText = "({T}: Add {W}.)"
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "307"
        artist = "Paulius Daščioras"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b2fba64-b85f-42bc-991e-4bb6201b78fb.jpg?1792550972"
    }
}

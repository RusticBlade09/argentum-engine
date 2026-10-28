package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val UnexploredTerritory = card("Unexplored Territory") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{1}, {T}, Sacrifice this land: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "303"
        artist = "Raymond Bonilla"
        flavorText = "\"Does it count as 'explored' if everyone who was here died like four hundred years ago?\"\n—Lieutenant junior grade Beckett Mariner"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca1e60f1-11a9-4bb8-9f6e-5b652b49c517.jpg?1792591180"
    }
}

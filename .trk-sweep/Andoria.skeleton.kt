package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Andoria = card("Andoria") {
    typeLine = "Land — Planet"
    colorIdentity = "RU"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {U} or {R}."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "275"
        artist = "Josu Hernaiz"
        flavorText = "\"Imperial commander's log: It's refreshing to see the icy drifts of Andoria again. The warmth of Federation vessels has made me soft, and I prefer strength.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e9e72ef-3e34-4d19-aec0-b5e1ba84d244.jpg?1792632926"
    }
}

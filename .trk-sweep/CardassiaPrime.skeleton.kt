package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val CardassiaPrime = card("Cardassia Prime") {
    typeLine = "Land — Planet"
    colorIdentity = "BW"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {W} or {B}."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "280"
        artist = "Alex Negrea"
        flavorText = "\"Gul's log: I return to find the entire city of Lakat wiped away, our Dominion allies licking wounds, and the damned Federation offering support. I have swallowed many a rancid meal in this war, but nothing so bitter as my own pride.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d634424f-9148-46a5-95e3-4965a9226bb4.jpg?1792632933"
    }
}

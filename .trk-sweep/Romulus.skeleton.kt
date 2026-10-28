package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Romulus = card("Romulus") {
    typeLine = "Land — Planet"
    colorIdentity = "BU"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {U} or {B}."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "294"
        artist = "Carlos Dattoli"
        flavorText = "\"Centurion's log: With the commander's detainment at the hands of the Tal Shiar, it seems likely I will see a promotion upon our return. A shame she did not conceal her treachery better.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bce111b-b07c-4589-9b6d-f0b5f320808c.jpg?1792632971"
    }
}

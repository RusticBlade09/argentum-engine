package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Ferenginar = card("Ferenginar") {
    typeLine = "Land — Planet"
    colorIdentity = "BG"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {B} or {G}."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "283"
        artist = "Colin Searle"
        flavorText = "\"Operation chief's log: Heading back to the Ferengi homeworld to pick up more salvage. Raining again. The tubeworms aren't bad, but I just want to bite into something crisp again.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17fab75f-7ff9-4c50-a6f7-03a85cd275bf.jpg?1792632949"
    }
}

package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val EarthSector001 = card("Earth, Sector 001") {
    typeLine = "Land — Planet"
    colorIdentity = "GU"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {G} or {U}."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "281"
        artist = "Colin Searle"
        flavorText = "\"Subcommander's log: We approach the human homeworld. I am curious to witness it myself. Perhaps it will yield insight into their illogical yet accomplished culture.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5278d354-97c4-4c21-a902-2f9252b151eb.jpg?1792632943"
    }
}

package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val QonoS = card("Qo'noS") {
    typeLine = "Land — Planet"
    colorIdentity = "BR"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {B} or {R}."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "291"
        artist = "Rockey Chen"
        flavorText = "\"General's log: We return triumphant! Let the streets of Qo'noS quake, for her warriors return with a hunger for new battle!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1558d93-8a8a-45e0-bcdc-e6cd42f716dc.jpg?1792632963"
    }
}

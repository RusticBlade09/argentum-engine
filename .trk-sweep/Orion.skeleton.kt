package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Orion = card("Orion") {
    typeLine = "Land — Planet"
    colorIdentity = "GR"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {R} or {G}."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "288"
        artist = "Simon Dominic"
        flavorText = "\"Ensign's log: Whoa-oa-oa! I've never been banned from a whole planet before! I've gotta start more beefs with pirates!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dec37ff4-d6e9-4b87-865b-125b17162465.jpg?1792632952"
    }
}

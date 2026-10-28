package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Vulcan = card("Vulcan") {
    typeLine = "Land — Planet"
    colorIdentity = "UW"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {W} or {U}."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "305"
        artist = "John Di Giovanni"
        flavorText = "\"Chief engineer's log: Departing Vulcan. Good riddance. A whole planet hotter'n a Panhandle summer really tells you how every last Vulcan ended up makin' that face.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc3fe0da-8d93-4f1c-9aad-c2fdc9fe34fc.jpg?1792632981"
    }
}

package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val TempleGarden = card("Temple Garden") {
    typeLine = "Land — Forest Plains"
    colorIdentity = "GW"
    oracleText = "({T}: Add {G} or {W}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "301"
        artist = "Shahab Alizadeh"
        flavorText = "The Edo view their homeworld as a paradise, though outsiders find their strict justice system senselessly brutal."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9b0589d-f327-46a7-8bac-06b7654c547a.jpg?1784036854"
    }
}

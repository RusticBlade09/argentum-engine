package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val BreedingPool = card("Breeding Pool") {
    typeLine = "Land — Forest Island"
    colorIdentity = "GU"
    oracleText = "({T}: Add {G} or {U}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "279"
        artist = "Cliff Nielsen"
        flavorText = "In the milky pools permeating the caves of Mak'ala, Trill symbionts await their new lives."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63e4dc07-c742-41bd-8301-861637908fd1.jpg?1784036820"
    }
}

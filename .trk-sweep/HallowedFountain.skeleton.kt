package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val HallowedFountain = card("Hallowed Fountain") {
    typeLine = "Land — Plains Island"
    colorIdentity = "UW"
    oracleText = "({T}: Add {W} or {U}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "286"
        artist = "Piotr Dura"
        flavorText = "The metaphasic radiation of the Ba'ku planet promised medical marvels, as long as the locals could be quietly removed."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7285986-7e08-4969-86ef-452dc5bfdd9f.jpg?1784036830"
    }
}

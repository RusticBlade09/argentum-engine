package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val SteamVents = card("Steam Vents") {
    typeLine = "Land — Island Mountain"
    colorIdentity = "RU"
    oracleText = "({T}: Add {U} or {R}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "298"
        artist = "Shahab Alizadeh"
        flavorText = "As one of the Galaxy's few ectothermic sapient species, Cardassians prefer high temperatures. Many use sweltering saunas to \"charge\" themselves before interacting with other species."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a83903c7-fd51-4526-aed2-359e946fea36.jpg?1784036844"
    }
}

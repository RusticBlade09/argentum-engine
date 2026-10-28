package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val GodlessShrine = card("Godless Shrine") {
    typeLine = "Land — Plains Swamp"
    colorIdentity = "BW"
    oracleText = "({T}: Add {W} or {B}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "285"
        artist = "John Thacker"
        flavorText = "After Gul Dukat aligned the Cardassian Union with the Dominion, the Founders moved their military headquarters to Cardassia Prime to better oversee war efforts."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fbd1ae0-3d4c-492a-a1ea-85a95fa3d7b6.jpg?1784036826"
    }
}

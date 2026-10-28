package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val WateryGrave = card("Watery Grave") {
    typeLine = "Land — Island Swamp"
    colorIdentity = "BU"
    oracleText = "({T}: Add {U} or {B}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "306"
        artist = "Raymond Bonilla"
        flavorText = "Earth's restored cetacean population eventually came to see the wreck of the *Bounty* as a sort of holy site."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5525d6a6-e532-4047-9da4-bfae7927fecc.jpg?1784036860"
    }
}

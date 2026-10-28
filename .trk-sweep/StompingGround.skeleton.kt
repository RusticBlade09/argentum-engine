package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val StompingGround = card("Stomping Ground") {
    typeLine = "Land — Mountain Forest"
    colorIdentity = "GR"
    oracleText = "({T}: Add {R} or {G}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "299"
        artist = "Arthur Haas"
        flavorText = "\"Neural's active mantle and energetic water cycle create staggeringly beautiful rock formations and a variety of tropical microclimates. Don't feed the wildlife.\"\n—*Lawmim Galactopedia*"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ee8255a-44f6-4faa-9843-15432bf751ea.jpg?1784065855"
    }
}

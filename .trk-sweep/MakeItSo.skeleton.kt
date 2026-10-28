package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val MakeItSo = card("Make It So") {
    manaCost = "{3}{U}"
    typeLine = "Sorcery"
    colorIdentity = "U"
    oracleText = "Choose one —\n• Untap target permanent you control. Target player draws two cards.\n• Create a 1/1 red Officer creature token and draw two cards."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/7149df19-4833-47ca-8add-b897f55b930c.jpg?1793013541"
    }
}

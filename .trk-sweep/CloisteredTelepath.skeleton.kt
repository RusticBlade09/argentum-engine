package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val CloisteredTelepath = card("Cloistered Telepath") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Talosian Advisor"
    colorIdentity = "U"
    oracleText = "Flash\nWhen this creature enters, draw a card, then discard a card."
    power = 2
    toughness = 1
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Monztre"
        flavorText = "\"She has an illusion, and you have reality. May you find your way as pleasant.\"\n—The Keeper, magistrate of Talos IV"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/959318c1-3f7e-4418-91ce-be529af27972.jpg?1785981391"
    }
}

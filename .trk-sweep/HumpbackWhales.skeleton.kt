package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val HumpbackWhales = card("Humpback Whales") {
    manaCost = "{4}{U}{U}"
    typeLine = "Creature — Whale"
    colorIdentity = "U"
    oracleText = "Vigilance, ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\nBasic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"
    power = 5
    toughness = 6
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Evan Shipard"
        flavorText = "George and Gracie escaped extinction, and in the process, saved Earth."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/605263f0-08b6-4cc7-aba7-eb49850f8832.jpg?1785981416"
    }
}

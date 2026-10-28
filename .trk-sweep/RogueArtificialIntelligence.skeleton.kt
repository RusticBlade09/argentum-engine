package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val RogueArtificialIntelligence = card("Rogue Artificial Intelligence") {
    manaCost = "{4}{B}{B}"
    typeLine = "Artifact Creature — Construct"
    colorIdentity = "B"
    oracleText = "When this creature enters, choose one —\n• You gain 3 life.\n• Surveil 2. (Look at the top two cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)\nBasic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"
    power = 6
    toughness = 5
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "José Parodi"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edcce061-88c9-40bf-97f8-d0d3018e24dd.jpg?1785981516"
    }
}

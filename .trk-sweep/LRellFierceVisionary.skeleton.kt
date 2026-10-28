package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val LRellFierceVisionary = card("L'Rell, Fierce Visionary") {
    manaCost = "{1}{B}{R}"
    typeLine = "Legendary Creature — Klingon Warrior"
    colorIdentity = "BR"
    oracleText = "At the beginning of combat on your turn, create a 2/1 red Klingon Warrior creature token with haste. Sacrifice it at the beginning of the next end step.\n{2}, Sacrifice another creature or artifact: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    power = 1
    toughness = 4
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Patricia Pria"
        flavorText = "\"Conquer us, or we will never relent.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b06bbb9-7c6d-4889-b55a-1de77c27fd18.jpg?1792508377"
    }
}

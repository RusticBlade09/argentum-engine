package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val ColdBloodedCrew = card("Cold-Blooded Crew") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Gorn Warrior"
    colorIdentity = "G"
    oracleText = "When this creature enters, put two +1/+1 counters on target creature.\nThis creature can't be blocked by creatures with power 2 or less.\nBasic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"
    power = 4
    toughness = 4
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "John Tyler Christopher"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f2d1830-baea-4735-b029-cb8c0b538d83.jpg?1785981563"
    }
}

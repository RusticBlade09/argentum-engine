package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val ConstableOdo = card("Constable Odo") {
    manaCost = "{3}{W/U}{W/U}"
    typeLine = "Legendary Creature — Shapeshifter Detective"
    colorIdentity = "UW"
    oracleText = "When Constable Odo enters, choose one —\n• Tap target creature an opponent controls. Put two stun counters on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)\n• Investigate twice. (To investigate, create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"
    power = 4
    toughness = 3
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3df582e4-3bfb-4413-9b39-898f33a5c1fd.jpg?1792508297"
    }
}

package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val CetaceanOps = card("Cetacean Ops") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Whale Officer"
    colorIdentity = "U"
    oracleText = "Ward {1} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\nWhenever you cast a noncreature spell, put a +1/+1 counter on this creature."
    power = 3
    toughness = 3
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Arthur Haas"
        flavorText = "Various sapient whale species serve in Starfleet as navigators, scientists, and explorers."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8dc7ecd-e6f0-4050-9d5b-8436f60a1395.jpg?1792672912"
    }
}

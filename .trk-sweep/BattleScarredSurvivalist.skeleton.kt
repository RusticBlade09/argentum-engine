package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val BattleScarredSurvivalist = card("Battle-Scarred Survivalist") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Klingon Scout"
    colorIdentity = "B"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\nWhenever another creature dies, put a +1/+1 counter on this creature. This ability triggers only once each turn."
    power = 1
    toughness = 1
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "John Di Giovanni"
        flavorText = "\"I will rest when my blade knows vengeance.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c567253e-a2ee-45de-b3c6-3aeafbf8237b.jpg?1785981454"
    }
}

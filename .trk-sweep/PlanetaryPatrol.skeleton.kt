package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val PlanetaryPatrol = card("Planetary Patrol") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Scout"
    colorIdentity = "R"
    oracleText = "Landfall — Whenever a land you control enters, this creature gets +1/+1 until end of turn."
    power = 2
    toughness = 2
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Runa I. Rosenberger"
        flavorText = "\"Away team to *Enterprise*. No life signs anywhere. It's as if the entire colony simply walked away.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/163f5474-138d-4b47-a6f5-b1a0974e56a4.jpg?1785981552"
    }
}

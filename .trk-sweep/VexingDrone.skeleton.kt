package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val VexingDrone = card("Vexing Drone") {
    manaCost = "{1}{W}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "W"
    oracleText = "When this creature enters, tap target creature an opponent controls."
    power = 2
    toughness = 2
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Kristina WermoongRey"
        flavorText = "\"Freedom is irrelevant. Self-determination is irrelevant. You must comply.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f4b2476-8f93-4d4e-b664-233e82fcf212.jpg?1792591213"
    }
}

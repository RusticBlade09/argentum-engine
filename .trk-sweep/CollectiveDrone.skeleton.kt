package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val CollectiveDrone = card("Collective Drone") {
    manaCost = "{1}{B}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "B"
    oracleText = "Collective Drone's power is equal to the number of artifact creatures you control."
    toughness = 2
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "José Parodi"
        flavorText = "\"You will be assimilated.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65b74daa-4606-4118-9752-b0dcd7a3f486.jpg?1785981469"
    }
}

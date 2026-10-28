package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val AssaultDrone = card("Assault Drone") {
    manaCost = "{3}{B}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "B"
    oracleText = "Artifact creatures you control get +1/+0."
    power = 2
    toughness = 4
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Filipe Pagliuso"
        flavorText = "\"We have analyzed your defensive capabilities as being unable to withstand us. If you defend yourselves, you will be punished.\"\n—The Borg"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5ca2da7a-afbf-4b32-8ff6-ae899b9f70ad.jpg?1785981435"
    }
}

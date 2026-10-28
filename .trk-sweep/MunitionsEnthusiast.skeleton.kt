package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val MunitionsEnthusiast = card("Munitions Enthusiast") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Klingon Warrior"
    colorIdentity = "R"
    oracleText = "Trample\nWhen this creature dies, it deals 1 damage to any target."
    power = 3
    toughness = 2
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Lucio Parrillo"
        flavorText = "\"What is the point of your spikes?\" asked the *boqrat*. \"You cannot attack with them.\"\n\"Perhaps not,\" laughed the *targ*. \"But they bring me great joy!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d73bb1c-39e3-429c-b6bb-96dae2254708.jpg?1785981544"
    }
}

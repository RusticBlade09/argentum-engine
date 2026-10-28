package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Mugato = card("Mugato") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Ape Beast"
    colorIdentity = "G"
    oracleText = "Reach, vigilance, deathtouch"
    power = 6
    toughness = 6
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Chuck Lukacs"
        flavorText = "Despite their terrifying appearance, mugato are loving, social creatures who form strong family bonds, even mourning fallen family members."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3c68b20-f202-4d55-bef1-398d96e55c57.jpg?1785981595"
    }
}

package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Moopsy = card("Moopsy") {
    manaCost = "{G}"
    typeLine = "Creature — Beast"
    colorIdentity = "G"
    oracleText = "Deathtouch\n{2}: This creature gains reach until end of turn."
    power = 1
    toughness = 1
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "207"
        artist = "Brooklyn Smith"
        flavorText = "\"The moopsy is a small mammal known for its endearing appearance. The creature's diet consists of living bone tissue, which it liquefies and drinks via a process witnesses describe as 'upsettingly swift and graphic.'\"\n—*Lawmim Galactopedia*"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfbaf597-6be2-4aa0-88ae-cc4c94878add.jpg?1785981590"
    }
}

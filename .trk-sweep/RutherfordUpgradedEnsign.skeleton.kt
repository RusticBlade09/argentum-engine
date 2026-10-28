package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val RutherfordUpgradedEnsign = card("Rutherford, Upgraded Ensign") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Creature — Human Artificer"
    colorIdentity = "W"
    oracleText = "When Rutherford enters, look at the top six cards of your library. You may reveal an artifact card from among them and put it into your hand. Put the rest on the bottom of your library in a random order.\nWhenever you cast a Spacecraft spell, put a +1/+1 counter on target creature."
    power = 2
    toughness = 2
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Charles Chaisson"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16a4d8ed-a918-4dfe-9976-0b68adb26afa.jpg?1792753861"
    }
}

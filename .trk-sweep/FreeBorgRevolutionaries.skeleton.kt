package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val FreeBorgRevolutionaries = card("Free Borg Revolutionaries") {
    manaCost = "{3}{W}{W}"
    typeLine = "Artifact Creature — Borg Rebel"
    colorIdentity = "W"
    oracleText = "When this creature enters, creatures you control get +1/+1 until end of turn.\nBasic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"
    power = 4
    toughness = 5
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Smirtouille"
        flavorText = "\"Resistance is *not* futile.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6711e910-e5a8-4602-9559-de5374bffff2.jpg?1785981345"
    }
}

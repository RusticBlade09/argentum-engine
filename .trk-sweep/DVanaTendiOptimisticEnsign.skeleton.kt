package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val DVanaTendiOptimisticEnsign = card("D'Vana Tendi, Optimistic Ensign") {
    manaCost = "{1}{G/U}{G/U}"
    typeLine = "Legendary Creature — Orion Scientist"
    colorIdentity = "GU"
    oracleText = "{T}: Untap another target permanent.\n{3}: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    power = 2
    toughness = 4
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Micah Epstein"
        flavorText = "\"I got to hold a heart!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/206239ca-341a-4f07-8149-a24ccf20ee45.jpg?1792508323"
    }
}

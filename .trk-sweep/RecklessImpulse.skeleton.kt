package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val RecklessImpulse = card("Reckless Impulse") {
    manaCost = "{1}{R}"
    typeLine = "Sorcery"
    colorIdentity = "R"
    oracleText = "Exile the top two cards of your library. Until the end of your next turn, you may play those cards."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Simon Dominic"
        flavorText = "Fortune favors the bold.\n—Human proverb"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bca43d8-7c69-47f4-8f82-48dd2b6a7cba.jpg?1785981560"
    }
}

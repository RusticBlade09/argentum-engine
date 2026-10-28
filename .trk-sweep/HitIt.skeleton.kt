package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val HitIt = card("Hit It!") {
    manaCost = "{1}{R}{G}"
    typeLine = "Enchantment"
    colorIdentity = "GR"
    oracleText = "Creatures you control have haste.\nWhen this enchantment enters, look at the top five cards of your library. Put a land card from among them onto the battlefield tapped and the rest into your graveyard."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Aaron J. Riley"
        flavorText = "\"I learned the best way to get into a cold stream was to jump right in.\"\n—Captain Christopher Pike"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/6266506f-fabf-4b53-8f93-6e2115a3e5a4.jpg?1792508342"
    }
}

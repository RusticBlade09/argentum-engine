package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val ChaDIchInvestigator = card("Cha'DIch Investigator") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Klingon Warrior"
    colorIdentity = "B"
    oracleText = "When this creature enters, create a 2/1 red Klingon Warrior creature token with haste."
    power = 4
    toughness = 4
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Javier Charro"
        flavorText = "Klingons who challenge the High Council are denied the right of personal combat. They must select a second—a *cha'DIch*—to defend them."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16281200-4660-4871-afd2-d4720e6a3197.jpg?1785981461"
    }
}

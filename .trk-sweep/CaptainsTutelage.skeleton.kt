package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val CaptainsTutelage = card("Captain's Tutelage") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Aura"
    colorIdentity = "R"
    oracleText = "Flash\nEnchant creature\nWhen this Aura enters, enchanted creature gains first strike until end of turn.\nEnchanted creature gets +2/+0."
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Mark Spears"
        flavorText = "Starfleet tactical training demands cadets learn how to handle any situation—even failure."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c340eba-645d-416a-9ed4-f2303aa8c919.jpg?1785981528"
    }
}

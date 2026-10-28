package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Batleth = card("Bat'leth") {
    manaCost = "{2}{B}"
    typeLine = "Artifact — Equipment"
    colorIdentity = "B"
    oracleText = "When this Equipment enters, target creature an opponent controls gets -2/-2 until end of turn.\nEquipped creature gets +2/+0.\nEquip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"
    // ABILITIES GO HERE
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Tony Foti"
        flavorText = "\"Your tongue is as sharp as a warrior's! Let's see if your blade is as well!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/888e6e0f-43c3-4516-88ff-8a8d814478d4.jpg?1785981448"
    }
}

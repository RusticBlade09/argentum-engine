package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.KeywordAbility

/** Cast this instant or sorcery from your graveyard for mana and returning a creature you control. */
fun CardBuilder.beamMeUp(cost: String) {
    keywordAbilityList.add(KeywordAbility.beamMeUp(cost))
}

package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class BeamMeUpModelTest : FunSpec({
    test("beam me up keyword round trips") {
        val ability = KeywordAbility.beamMeUp("{2}{U}")
        Json.decodeFromString<KeywordAbility>(Json.encodeToString(ability)) shouldBe ability
        ability.keyword shouldBe Keyword.BEAM_ME_UP
    }
    test("a conditional grant with source exclusion round trips") {
        val grant: StaticAbility = GraveyardCardsHaveCastKeyword(
            filter = GameObjectFilter.Any, keyword = Keyword.BEAM_ME_UP,
            condition = Conditions.IsYourMainPhase,
            returnFilter = GameObjectFilter.Creature.notSourceItself()
        )
        Json.decodeFromString<StaticAbility>(Json.encodeToString(grant)) shouldBe grant
    }
    test("beaming prohibition round trips") {
        val ability: StaticAbility = CantBeBeamedUp()
        Json.decodeFromString<StaticAbility>(Json.encodeToString(ability)) shouldBe ability
    }
})

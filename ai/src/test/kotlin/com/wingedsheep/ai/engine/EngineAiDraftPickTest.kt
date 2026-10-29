package com.wingedsheep.ai.engine

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.withFallbackRatings
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * The bot's draft picks: Draftsim-scored, with cards a set's table leaves unrated — every card of a
 * set we ship no ratings file for — rated off their definitions instead of by rarity alone.
 */
class EngineAiDraftPickTest : FunSpec({

    // A set code no ratings file exists for, like a new or custom set.
    val unratedSet = "ZZZ"

    val murder = card("Test Murder") {
        manaCost = "{1}{B}"
        typeLine = "Instant"
        oracleText = "Destroy target creature."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
        metadata { rarity = Rarity.COMMON }
    }
    val overcostedRare = card("Test Overcosted Rare") {
        manaCost = "{4}{W}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
        metadata { rarity = Rarity.RARE }
    }

    fun CardDefinition.summary() = CardSummary(
        name = name,
        manaCost = manaCost.toString(),
        typeLine = typeLine.toString(),
        rarity = metadata.rarity.name,
        power = creatureStats?.basePower,
        toughness = creatureStats?.baseToughness,
        oracleText = oracleText,
        setCode = unratedSet,
    )

    fun bot(vararg cards: CardDefinition): EngineAiPlayerController {
        val registry = CardRegistry().apply { register(cards.toList()) }
        return EngineAiPlayerController(registry, EntityId.of("bot"), { null })
    }

    test("in a set with no ratings file the bot takes common removal over an overcosted rare") {
        val pick = bot(murder, overcostedRare).chooseDraftPick(
            pack = listOf(overcostedRare.summary(), murder.summary()),
            pickedSoFar = emptyList(),
            packNumber = 1,
            pickNumber = 1,
        )

        pick shouldContainExactly listOf("Test Murder")
    }

    test("fallback ratings fill only the cards a set's table leaves unrated") {
        val ltr = MtgSetCatalog.all.first { it.code == "LTR" }
        val ratedCard = ltr.cards.first { !it.typeLine.isLand && DraftsimData.nameKey(it.name) in DraftsimData.tablesFor(listOf("LTR")).ratings }
        val base = DraftsimData.tablesFor(listOf("LTR"))

        val tables = base.withFallbackRatings(listOf(ratedCard, murder, overcostedRare))

        tables.ratings[DraftsimData.nameKey(ratedCard.name)] shouldBe base.ratings[DraftsimData.nameKey(ratedCard.name)]
        tables.ratings.getValue(DraftsimData.nameKey("Test Murder")) shouldBeGreaterThan
            tables.ratings.getValue(DraftsimData.nameKey("Test Overcosted Rare"))
    }
})

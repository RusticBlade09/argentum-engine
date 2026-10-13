package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Caesar, Legion's Emperor (PIP #1).
 *
 * "Whenever you attack, you may sacrifice another creature. When you do, choose two — • two 1/1
 * red and white Soldier tokens with haste, tapped and attacking • you draw a card and you lose 1
 * life • Caesar deals damage equal to the number of creature tokens you control to target
 * opponent."
 *
 * Covers the reflexive gate (no sacrifice → no modes), the choose-two modal chosen as the
 * reflexive ability goes on the stack, the tokens entering tapped and attacking, and the token
 * count read at resolution (the first mode's Soldiers count for the third).
 */
class CaesarLegionsEmperorScenarioTest : ScenarioTestBase() {

    private val caesar = "Caesar, Legion's Emperor"

    private fun board(withFodder: Boolean = true, libraryCards: Int = 2): TestGame {
        val builder = scenario()
            .withPlayers("You", "Opponent")
            .withCardOnBattlefield(1, caesar)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withFodder) builder.withCardOnBattlefield(1, "Grizzly Bears")
        repeat(libraryCards) { builder.withCardInLibrary(1, "Plains") }
        builder.withCardInLibrary(2, "Plains")
        val game = builder.build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf(caesar to 2)).error shouldBe null
        return game
    }

    /** Answer the attack trigger: [sacrifice] or not, then pick the two modes whose labels contain [modes]. */
    private fun TestGame.runTrigger(sacrifice: Boolean, modes: List<String> = emptyList()) {
        val remaining = modes.toMutableList()
        var guard = 0
        while (guard++ < 40) {
            when (val decision = state.pendingDecision) {
                is YesNoDecision -> answerYesNo(sacrifice).error shouldBe null
                is SelectCardsDecision -> selectCards(listOf(findPermanent("Grizzly Bears")!!)).error shouldBe null
                is ChooseOptionDecision -> {
                    val label = remaining.removeFirstOrNull() ?: error("unexpected mode question: ${decision.options}")
                    val index = decision.options.indexOfFirst { it.contains(label, ignoreCase = true) }
                    check(index >= 0) { "$label not offered; options=${decision.options}" }
                    submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
                }
                is ChooseTargetsDecision -> submitDecision(
                    TargetsResponse(decision.id, decision.targetRequirements.associate { it.index to listOf(player2Id) })
                ).error shouldBe null
                null -> {
                    if (state.stack.isEmpty()) return
                    resolveStack()
                }
                else -> error("unexpected decision: $decision")
            }
        }
        error("decision loop did not settle")
    }

    private fun TestGame.soldierTokens(): List<EntityId> =
        state.getBattlefield().filter {
            val entity = state.getEntity(it)
            entity?.has<TokenComponent>() == true && entity.get<CardComponent>()?.name?.contains("Soldier") == true
        }

    init {
        context("the reflexive gate") {
            test("declining the sacrifice chooses no modes and changes nothing") {
                val game = board()
                game.runTrigger(sacrifice = false)
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.soldierTokens().size shouldBe 0
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            test("with no other creature to sacrifice, nothing is asked") {
                val game = board(withFodder = false)
                game.resolveStack()
                withClue("Caesar can't sacrifice itself — 'another creature'") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe false
                }
                game.isOnBattlefield(caesar) shouldBe true
                game.soldierTokens().size shouldBe 0
            }
        }

        context("choose two") {
            test("tokens + damage: two hasty Soldiers enter tapped and attacking and are counted for the damage") {
                val game = board()
                game.runTrigger(sacrifice = true, modes = listOf("Create two", "deals damage"))

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                val soldiers = game.soldierTokens()
                soldiers.size shouldBe 2
                soldiers.forEach { id ->
                    game.state.getEntity(id)!!.has<TappedComponent>() shouldBe true
                    game.state.getEntity(id)!!.has<AttackingComponent>() shouldBe true
                }
                withClue("damage equals the two creature tokens you control") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("draw + damage with no tokens: draw a card, lose 1 life, zero damage") {
                val game = board()
                val handBefore = game.handSize(1)
                game.runTrigger(sacrifice = true, modes = listOf("draw a card", "deals damage"))

                game.handSize(1) shouldBe handBefore + 1
                game.getLifeTotal(1) shouldBe 19
                game.getLifeTotal(2) shouldBe 20
                game.soldierTokens().size shouldBe 0
            }

            test("exactly two modes — tokens + draw leaves the opponent's life untouched") {
                val game = board()
                game.runTrigger(sacrifice = true, modes = listOf("Create two", "draw a card"))
                game.soldierTokens().size shouldBe 2
                game.getLifeTotal(1) shouldBe 19
                game.getLifeTotal(2) shouldBe 20
                game.state.getZone(game.player1Id, Zone.GRAVEYARD).size shouldBe 1
            }
        }
    }
}

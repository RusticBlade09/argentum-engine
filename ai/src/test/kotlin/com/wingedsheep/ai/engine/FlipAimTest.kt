package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.FlipOntoBattlefieldDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.FlipThrowResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The AI's Chaos Orb flip ([FlipAim]): it throws at what it most wants destroyed, never at its own
 * board, and with a clean release.
 */
class FlipAimTest : ScenarioTestBase() {

    private val orbAbility = cardRegistry.getCard("Chaos Orb")!!.activatedAbilities.first().id

    private fun TestGame.flipDecision(): FlipOntoBattlefieldDecision {
        execute(ActivateAbility(playerId = player1Id, sourceId = findPermanent("Chaos Orb")!!, abilityId = orbAbility))
            .error shouldBe null
        resolveStack()
        return getPendingDecision().shouldBeInstanceOf<FlipOntoBattlefieldDecision>()
    }

    init {
        context("FlipAim") {

            test("throws at the opponent's most valuable permanent and destroys it") {
                val game = scenario().withRngSeed(1993L)
                    .withPlayers("AI", "Opponent")
                    .withCardOnBattlefield(1, "Chaos Orb")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Shivan Dragon")
                    .withCardOnBattlefield(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val decision = game.flipDecision()
                val dragon = decision.table.cards.single { it.entityId == game.findPermanent("Shivan Dragon") }

                val response = AIPlayer.create(cardRegistry, game.player1Id)
                    .respondToDecision(game.state, decision)
                    .shouldBeInstanceOf<FlipThrowResponse>()
                // At the dragon, or at the seam between it and the Mountain behind it to take both.
                withClue("aimed within reach of the dragon") {
                    kotlin.math.hypot(response.aimX - dragon.x, response.aimY - dragon.y) shouldBeLessThan 60.0
                }
                FlipAim.respond(game.state, decision, game.player1Id).strength shouldBe response.strength

                game.execute(SubmitDecision(game.player1Id, response)).error shouldBe null
                game.isInGraveyard(2, "Shivan Dragon").shouldBeTrue()
                game.isOnBattlefield("Grizzly Bears").shouldBeTrue()
            }

            test("with nothing of the opponent's worth hitting, it throws the Orb off the table") {
                val game = scenario().withRngSeed(1993L)
                    .withPlayers("AI", "Opponent")
                    .withCardOnBattlefield(1, "Chaos Orb")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val decision = game.flipDecision()

                val response = FlipAim.respond(game.state, decision, game.player1Id)
                withClue("aimed past the far edge") { response.aimY shouldBeLessThan 0.0 }

                game.execute(SubmitDecision(game.player1Id, response)).error shouldBe null
                game.findPermanents("Grizzly Bears").size shouldBe 2
            }
        }
    }
}

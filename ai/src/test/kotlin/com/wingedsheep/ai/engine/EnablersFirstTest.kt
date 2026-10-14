package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.enablersFirst], as [AiProfile.LIVE] ships it — the spell that makes another cheaper is
 * cast first.
 *
 * The misplay is game `8522de5e` turn 10 (2026-10-14, Reality Fracture draft), rebuilt as a minimal
 * position, with the pre-flag answer pinned beside it.
 */
class EnablersFirstTest : ScenarioTestBase() {

    private val before = AiProfile.LIVE.copy(id = "live-without-enablers", enablersFirst = false)

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    /** Let the AI play out our precombat main, the opponent passing, until it leaves the step. */
    private fun TestGame.letAiPlayOutMain(limit: Int = 20) {
        val ai = AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE)
        repeat(limit) {
            if (state.step != Step.PRECOMBAT_MAIN) return
            val decision = state.pendingDecision
            if (decision != null) {
                execute(SubmitDecision(decision.playerId, ai.respondToDecision(state, decision))).error shouldBe null
                return@repeat
            }
            val priority = state.priorityPlayerId ?: return
            execute(if (priority == player1Id) ai.chooseAction(state) else PassPriority(priority)).error shouldBe null
        }
    }

    private fun position() = scenario().withPlayers()
        .withTurnNumber(10)
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInHand(1, "Traxos, Academy Guardian")
        .withCardInHand(1, "Tam's Resistance")
        // As in the game: no creature on either side for the +1/+1 counter to land on.
        .withLandsOnBattlefield(2, "Forest", 5)
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {

        test("T10: the noncreature spell goes first, not Traxos at full price") {
            val game = position()

            game.castName(game.choice(before)) shouldBe "Traxos, Academy Guardian"
            game.castName(game.choice()) shouldBe "Tam's Resistance"
        }

        test("an up-to-one spell with no creature to target is still a candidate") {
            // In the game Tam's Resistance never reached the candidate list: the meaningful-action
            // filter dropped any targeted spell with no legal target, "up to one" included.
            val game = scenario().withPlayers()
                .withTurnNumber(10)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Tam's Resistance")
                .withLandsOnBattlefield(2, "Forest", 5)
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castName(game.choice()) shouldBe "Tam's Resistance"
        }

        test("the same four mana then casts both") {
            val game = position()
            game.letAiPlayOutMain()

            game.isOnBattlefield("Traxos, Academy Guardian") shouldBe true
            game.isInGraveyard(1, "Tam's Resistance") shouldBe true
        }

        test("with no enabler in hand, Traxos is still cast at full price") {
            val game = scenario().withPlayers()
                .withTurnNumber(10)
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInHand(1, "Traxos, Academy Guardian")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castName(game.choice()) shouldBe "Traxos, Academy Guardian"
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.SplitSpellCharacteristicsComponent
import com.wingedsheep.engine.mechanics.SplitSpellCasts
import com.wingedsheep.engine.mechanics.stack.StackPlacement
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class FuseScenarioTest : ScenarioTestBase() {
    private fun split(name: String, pause: Boolean = false, x: Boolean = false) = card(name) {
        layout = CardLayout.SPLIT
        keywords(Keyword.FUSE)
        face("Left $name") {
            manaCost = if (x) "{X}{R}" else "{R}"
            typeLine = "Sorcery"
            spell { effect = if (pause) Effects.Scry(1) else if (x) Effects.GainLife(DynamicAmount.XValue) else Effects.DrawCards(1) }
        }
        face("Right $name") {
            manaCost = if (x) "{X}{W}" else "{1}{W}"
            typeLine = "Sorcery"
            spell { effect = if (x) Effects.GainLife(DynamicAmount.XValue) else Effects.GainLife(DynamicAmount.Count(Player.You, Zone.HAND)) }
        }
    }
    init {
        val ordered = split("Test Fuse")
        val paused = split("Test Paused Fuse", pause = true)
        val xSpell = split("Test X Fuse", x = true)
        val freeHost = card("Test Free Host") {
            manaCost = "{0}"; typeLine = "Sorcery"
            spell { effect = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND))
                val selected = chooseSpell(hand)
                add(CastFromCollectionWithoutPayingCostEffect(selected.name))
            } }
        }
        cardRegistry.register(listOf(ordered, paused, xSpell, freeHost))
        fun board(name: String = ordered.name, extraHand: String? = null): TestGame {
            val builder = scenario().withPlayers("Caster", "Opponent").withCardInHand(1, name)
                .withLandsOnBattlefield(1, "Mountain", 3).withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Plains").withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            extraHand?.let { builder.withCardInHand(1, it) }
            return builder.build()
        }
        fun TestGame.cardId(name: String = ordered.name) = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }
        test("fused instructions run left before right") {
            val game = board(); val id = game.cardId()
            game.execute(CastSpell(game.player1Id, id, castFused = true)).error.shouldBeNull()
            game.resolveStack(); game.state.getHand(game.player1Id).size shouldBe 1
            game.getLifeTotal(1) shouldBe 21
        }
        test("a paused left half resumes the right half and finalizes one card") {
            val game = board(paused.name, "Lightning Bolt"); val id = game.cardId(paused.name)
            game.execute(CastSpell(game.player1Id, id, castFused = true)).error.shouldBeNull()
            game.resolveStack(); game.state.pendingDecision.shouldNotBeNull()
            game.getLifeTotal(1) shouldBe 20
            game.skipSelection().error.shouldBeNull()
            if (game.state.pendingDecision != null) game.keepLibraryOrder().error.shouldBeNull()
            game.resolveStack(); game.getLifeTotal(1) shouldBe 21
            game.state.getGraveyard(game.player1Id).contains(id) shouldBe true
            game.state.continuationStack.isEmpty() shouldBe true
        }
        test("both X symbols use one announced X and pay for it twice") {
            val game = board(xSpell.name); val id = game.cardId(xSpell.name)
            val offer = game.getLegalActions(1).first { (it.action as? CastSpell)?.castFused == true }
            offer.hasXCost shouldBe true; offer.maxAffordableX shouldBe 2
            game.execute(CastSpell(game.player1Id, id, castFused = true, xValue = 2)).error.shouldBeNull()
            game.resolveStack(); game.getLifeTotal(1) shouldBe 24
        }
        test("a copy keeps both halves and combined characteristics without being cast") {
            val game = board(); val id = game.cardId()
            game.execute(CastSpell(game.player1Id, id, castFused = true)).error.shouldBeNull()
            val copied = StackPlacement.putSpellCopy(game.state, id)
            copied.error.shouldBeNull(); game.state = copied.state
            val copyId = game.state.stack.last()
            game.state.getEntity(copyId)!!.get<SpellOnStackComponent>()!!.castFused shouldBe true
            game.state.getEntity(copyId)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.RED, Color.WHITE)
            game.resolveStack(); game.getLifeTotal(1) shouldBe 23
            game.state.getEntity(copyId).shouldBeNull()
            game.state.spellsCastThisTurn shouldBe 1
        }
        test("fuse is rejected from every non-hand zone even with a forged action") {
            for (zone in listOf(Zone.GRAVEYARD, Zone.EXILE, Zone.LIBRARY, Zone.COMMAND)) {
                val game = board(); val id = game.cardId()
                game.state = game.state.removeFromZone(ZoneKey(game.player1Id, Zone.HAND), id).addToZone(ZoneKey(game.player1Id, zone), id)
                game.execute(CastSpell(game.player1Id, id, castFused = true)).error.shouldNotBeNull()
                game.state.stack.isEmpty() shouldBe true
            }
        }
        test("fuse cannot be combined with a face or used on another card") {
            val game = board()
            game.execute(CastSpell(game.player1Id, game.cardId(), castFused = true, faceIndex = 0)).error.shouldNotBeNull()
            val bolt = board("Lightning Bolt")
            bolt.execute(CastSpell(bolt.player1Id, bolt.cardId("Lightning Bolt"), castFused = true)).error.shouldNotBeNull()
        }
        test("sorcery fuse obeys normal timing") {
            val game = board(); game.state = game.state.copy(activePlayerId = game.player2Id)
            game.getLegalActions(1).none { (it.action as? CastSpell)?.castFused == true } shouldBe true
            game.execute(CastSpell(game.player1Id, game.cardId(), castFused = true)).error.shouldNotBeNull()
        }
        test("combined cost reductions apply once") {
            val game = board()
            game.state = game.state.updateEntity(game.cardId()) { it }
            val fused = SplitSpellCasts.fusedDefinition(ordered)
            fused.manaCost.toString() shouldBe "{1}{R}{W}"
            // The three-symbol cost remains one spell for all ordinary cost modifiers.
            fused.script.targetRequirements.size shouldBe 0
        }
        test("the free-cast hand picker offers fuse and resumes both halves") {
            val game = board(ordered.name, freeHost.name); val id = game.cardId()
            game.castSpell(1, freeHost.name).error.shouldBeNull(); game.resolveStack()
            game.selectCards(listOf(id)).error.shouldBeNull()
            val choice = game.state.pendingDecision as ChooseOptionDecision
            val index = choice.options.indexOf("${ordered.name} (fused)"); (index >= 0) shouldBe true
            game.submitDecision(OptionChosenResponse(choice.id, index)).error.shouldBeNull()
            game.resolveStack(); game.getLifeTotal(1) shouldBe 21
            game.state.getEntity(id)!!.get<SplitSpellCharacteristicsComponent>().shouldBeNull()
        }
    }
}

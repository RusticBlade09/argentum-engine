package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.ColonelAutumn
import com.wingedsheep.mtg.sets.definitions.pip.cards.AradeshTheFounder
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ColonelAutumnScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ColonelAutumn, AradeshTheFounder))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.drain(victim: EntityId? = null, accept: Boolean = true) {
        repeat(40) {
            when (pendingDecision) {
                is YesNoDecision -> submitYesNo(player1, accept).error shouldBe null
                is SelectCardsDecision -> submitCardSelection(player1, listOf(victim!!)).error shouldBe null
                null -> if (state.stack.isEmpty()) return else bothPass()
                else -> error("Unexpected decision: $pendingDecision")
            }
        }
        error("Unsettled stack")
    }
    fun GameTestDriver.counters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    fun GameTestDriver.castColonel(): EntityId {
        giveMana(player1, Color.WHITE, 2)
        giveMana(player1, Color.BLACK, 1)
        val card = putCardInHand(player1, ColonelAutumn.name)
        castSpell(player1, card).error shouldBe null
        return card
    }
    test("Colonel exploits another creature and counters every surviving creature") {
        val d = setup()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        val colonel = d.castColonel()
        d.drain(bears)
        (bears in d.getGraveyard(d.player1)) shouldBe true
        d.counters(colonel) shouldBe 1
        d.counters(giant) shouldBe 1
    }
    test("declining exploit puts no counters on creatures") {
        val d = setup()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val colonel = d.castColonel()
        d.drain(accept = false)
        d.counters(colonel) shouldBe 0
        d.counters(bears) shouldBe 0
    }
    test("Colonel sacrificing itself still triggers its exploit observer") {
        val d = setup()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val colonel = d.castColonel()
        d.drain(colonel)
        (colonel in d.getGraveyard(d.player1)) shouldBe true
        d.counters(bears) shouldBe 1
    }
    test("another legendary creature gains exploit on entry and feeds Colonel's observer") {
        val d = setup()
        val colonel = d.putPermanentOnBattlefield(d.player1, ColonelAutumn.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val aradesh = d.putCardInHand(d.player1, AradeshTheFounder.name)
        d.giveMana(d.player1, Color.WHITE, 3)
        d.castSpell(d.player1, aradesh).error shouldBe null
        d.drain(bears)
        (bears in d.getGraveyard(d.player1)) shouldBe true
        d.counters(colonel) shouldBe 1
        d.counters(aradesh) shouldBe 1
    }
    test("Colonel absent when another creature exploits gives no counters") {
        val d = setup()
        val colonel = d.putPermanentOnBattlefield(d.player1, ColonelAutumn.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val aradesh = d.putCardInHand(d.player1, AradeshTheFounder.name)
        d.giveMana(d.player1, Color.WHITE, 3)
        d.castSpell(d.player1, aradesh).error shouldBe null
        d.bothPass()
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, bolt, listOf(colonel)).error shouldBe null
        d.bothPass()
        d.drain(bears)
        (bears in d.getGraveyard(d.player1)) shouldBe true
        d.counters(aradesh) shouldBe 0
    }
    test("ordinary creatures do not gain exploit") {
        val d = setup()
        d.putPermanentOnBattlefield(d.player1, ColonelAutumn.name)
        val bears = d.putCardInHand(d.player1, "Grizzly Bears")
        d.giveMana(d.player1, Color.GREEN, 2)
        d.castSpell(d.player1, bears).error shouldBe null
        d.bothPass()
        d.state.stack.isEmpty() shouldBe true
        d.pendingDecision shouldBe null
    }
})

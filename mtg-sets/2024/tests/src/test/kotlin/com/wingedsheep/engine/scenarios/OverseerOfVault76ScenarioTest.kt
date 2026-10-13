package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.OverseerOfVault76
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OverseerOfVault76ScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + OverseerOfVault76)
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.counters(id: EntityId, type: CounterType) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0
    fun GameTestDriver.drain(selection: List<EntityId> = emptyList(), accept: Boolean = true) {
        repeat(40) {
            when (pendingDecision) {
                is SelectCardsDecision -> submitCardSelection(player1, if (accept) selection else emptyList()).error shouldBe null
                is YesNoDecision -> submitYesNo(player1, accept).error shouldBe null
                null -> if (state.stack.isEmpty()) return else bothPass()
                else -> error("Unexpected decision: $pendingDecision")
            }
        }
        error("Unsettled stack")
    }
    test("its own entry adds a quest counter and another creature must have power at most three") {
        val d = setup()
        val overseer = d.putCardInHand(d.player1, OverseerOfVault76.name)
        d.giveMana(d.player1, Color.WHITE, 3)
        d.castSpell(d.player1, overseer).error shouldBe null
        d.drain()
        d.counters(overseer, CounterType.QUEST) shouldBe 1
        val bear = d.putCardInHand(d.player1, "Grizzly Bears")
        d.giveMana(d.player1, Color.GREEN, 2)
        d.castSpell(d.player1, bear).error shouldBe null
        d.drain()
        d.counters(overseer, CounterType.QUEST) shouldBe 2
        val giant = d.putCardInHand(d.player1, "Hill Giant")
        d.giveMana(d.player1, Color.RED, 4)
        d.castSpell(d.player1, giant).error shouldBe null
        d.drain()
        // Hill Giant is 3/3, so it qualifies despite its mana value being four.
        d.counters(overseer, CounterType.QUEST) shouldBe 3
        val wurm = d.putCardInHand(d.player1, "Craw Wurm")
        d.giveMana(d.player1, Color.GREEN, 6)
        d.castSpell(d.player1, wurm).error shouldBe null
        d.drain()
        d.counters(overseer, CounterType.QUEST) shouldBe 3
    }
    test("the self entry bypasses the power limit while other entries use projected power") {
        val d = setup()
        d.putPermanentOnBattlefield(d.player1, "Glorious Anthem")
        val overseer = d.putCardInHand(d.player1, OverseerOfVault76.name)
        d.giveMana(d.player1, Color.WHITE, 3)
        d.castSpell(d.player1, overseer).error shouldBe null
        d.drain()
        d.counters(overseer, CounterType.QUEST) shouldBe 1
        val giant = d.putCardInHand(d.player1, "Hill Giant")
        d.giveMana(d.player1, Color.RED, 4)
        d.castSpell(d.player1, giant).error shouldBe null
        d.drain()
        d.counters(overseer, CounterType.QUEST) shouldBe 1
    }
    test("combat payment distributes exactly three quest counters and boosts every creature") {
        val d = setup()
        val overseer = d.putPermanentOnBattlefield(d.player1, OverseerOfVault76.name)
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.addComponent(overseer, CountersComponent(mapOf(CounterType.QUEST to 2)))
        d.addComponent(bear, CountersComponent(mapOf(CounterType.QUEST to 2)))
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.drain(listOf(overseer, overseer, bear))
        d.counters(overseer, CounterType.QUEST) shouldBe 0
        d.counters(bear, CounterType.QUEST) shouldBe 1
        listOf(overseer, bear).forEach {
            d.counters(it, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            d.state.projectedState.hasKeyword(it, Keyword.VIGILANCE) shouldBe true
        }
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.hasKeyword(bear, Keyword.VIGILANCE) shouldBe false
    }
    test("declining removal preserves quest counters and gives no payoff") {
        val d = setup()
        val overseer = d.putPermanentOnBattlefield(d.player1, OverseerOfVault76.name)
        d.addComponent(overseer, CountersComponent(mapOf(CounterType.QUEST to 3)))
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.drain(accept = false)
        d.counters(overseer, CounterType.QUEST) shouldBe 3
        d.counters(overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }
    test("insufficient quest counters do not offer partial payment or grant the payoff") {
        val d = setup()
        val overseer = d.putPermanentOnBattlefield(d.player1, OverseerOfVault76.name)
        d.addComponent(overseer, CountersComponent(mapOf(CounterType.QUEST to 2)))
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.drain()
        d.pendingDecision shouldBe null
        d.counters(overseer, CounterType.QUEST) shouldBe 2
        d.counters(overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }
})

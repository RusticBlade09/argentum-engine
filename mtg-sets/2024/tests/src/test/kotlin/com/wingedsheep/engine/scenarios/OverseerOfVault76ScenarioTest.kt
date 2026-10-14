package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.OverseerOfVault76
import com.wingedsheep.mtg.sets.definitions.mrd.cards.RaiseTheAlarm
import com.wingedsheep.mtg.sets.definitions.usg.cards.GloriousAnthem
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class OverseerOfVault76ScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(OverseerOfVault76, RaiseTheAlarm, GloriousAnthem))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        repeat(30) {
            if (d.pendingDecision == null && d.state.stack.isNotEmpty())
                d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
        }
    }
    fun counters(d: GameTestDriver, id: EntityId, kind: CounterType = CounterType.QUEST) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0
    fun setQuest(d: GameTestDriver, id: EntityId, count: Int) =
        d.addComponent(id, CountersComponent().withAdded(CounterType.QUEST, count))
    fun cast(d: GameTestDriver, name: String) : EntityId {
        val id = d.putCardInHand(d.player1, name)
        d.giveMana(d.player1, Color.WHITE, 8)
        d.giveMana(d.player1, Color.GREEN, 8)
        d.castSpell(d.player1, id).error shouldBe null
        resolve(d)
        return id
    }
    fun combat(d: GameTestDriver) {
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        resolve(d)
    }

    test("self enters even above three projected power; other creatures use projected entry power") {
        val d = setup()
        d.putPermanentOnBattlefield(d.player1, GloriousAnthem.name)
        val overseer = cast(d, OverseerOfVault76.name)
        d.state.projectedState.getPower(overseer) shouldBe 4
        counters(d, overseer) shouldBe 1
        cast(d, "Centaur Courser") // Printed three, projected four: excluded.
        counters(d, overseer) shouldBe 1
        cast(d, "Grizzly Bears") // Printed two, projected three: included.
        counters(d, overseer) shouldBe 2
    }

    test("each simultaneous qualifying token entry contributes a quest counter") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        cast(d, RaiseTheAlarm.name)
        counters(d, overseer) shouldBe 2
    }

    test("remove three from one permanent, then respond before the separate team buff resolves") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        setQuest(d, overseer, 3)
        combat(d)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, List(3) { overseer }).error shouldBe null
        counters(d, overseer) shouldBe 0
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.state.stack.size shouldBe 1
        resolve(d)
        counters(d, overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

    test("split removal includes noncreature permanents and never buffs the opponent") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        val land = d.putLandOnBattlefield(d.player1, "Plains")
        val enemy = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        setQuest(d, overseer, 2); setQuest(d, land, 1)
        combat(d)
        d.submitCardSelection(d.player1, listOf(overseer, overseer, land)).error shouldBe null
        resolve(d)
        counters(d, overseer) shouldBe 0
        counters(d, land) shouldBe 0
        counters(d, land, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        counters(d, enemy, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.state.projectedState.hasKeyword(enemy, Keyword.VIGILANCE) shouldBe false
    }

    test("combat trigger pays from another permanent after Overseer leaves before resolution") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val land = d.putLandOnBattlefield(d.player1, "Plains")
        setQuest(d, land, 3)
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, bolt, listOf(overseer)).error shouldBe null
        d.bothPass()
        (overseer in d.getGraveyard(d.player1)) shouldBe true
        resolve(d)
        d.submitCardSelection(d.player1, List(3) { land }).error shouldBe null
        resolve(d)
        counters(d, land) shouldBe 0
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
    }

    test("declining preserves quest counters and creates no reflexive trigger") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        setQuest(d, overseer, 3)
        combat(d)
        d.submitCardSelection(d.player1, emptyList()).error shouldBe null
        resolve(d)
        counters(d, overseer) shouldBe 3
        counters(d, overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.state.stack.size shouldBe 0
    }

    test("two quest counters cannot pay; other counter kinds and opposing quests do not count") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        d.addComponent(overseer, CountersComponent().withAdded(CounterType.QUEST, 2).withAdded(CounterType.CHARGE, 5))
        val enemy = d.putLandOnBattlefield(d.player2, "Plains")
        setQuest(d, enemy, 8)
        combat(d)
        d.pendingDecision shouldBe null
        counters(d, overseer) shouldBe 2
        counters(d, overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }

    test("partial, over-capacity and excess payments reject without spending counters") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        val land = d.putLandOnBattlefield(d.player1, "Plains")
        setQuest(d, overseer, 2); setQuest(d, land, 2)
        combat(d)
        d.submitCardSelection(d.player1, listOf(overseer)).error?.isNotEmpty() shouldBe true
        d.submitCardSelection(d.player1, List(3) { overseer }).error?.isNotEmpty() shouldBe true
        d.submitCardSelection(d.player1, listOf(overseer, overseer, land, land)).error?.isNotEmpty() shouldBe true
        counters(d, overseer) shouldBe 2
        counters(d, land) shouldBe 2
        d.submitCardSelection(d.player1, listOf(overseer, land, land)).error shouldBe null
        resolve(d)
        counters(d, overseer) shouldBe 1
        counters(d, land) shouldBe 0
        counters(d, overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }
})

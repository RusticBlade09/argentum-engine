package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.YesManPersonalSecuritron
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class YesManPersonalSecuritronScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + YesManPersonalSecuritron)
        initMirrorMatch(com.wingedsheep.sdk.model.Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val ability = YesManPersonalSecuritron.activatedAbilities.first().id
    test("giving Yes Man away draws for the activator on a separate stack object and adds a quest counter") {
        val d = setup()
        val yesMan = d.putPermanentOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        val before = d.getHandSize(d.player1)
        d.submit(ActivateAbility(d.player1, yesMan, ability, targets = listOf(ChosenTarget.Player(d.player2)))).error shouldBe null
        d.bothPass()
        d.state.projectedState.getController(yesMan) shouldBe d.player2
        d.getHandSize(d.player1) shouldBe before
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getHandSize(d.player1) shouldBe before + 2
        d.state.getEntity(yesMan)!!.get<CountersComponent>()!!.getCount(CounterType.QUEST) shouldBe 1
    }
    test("Yes Man leaving before the gift resolves prevents the draw payoff") {
        val d = setup()
        val yesMan = d.putPermanentOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        val before = d.getHandSize(d.player1)
        d.submit(ActivateAbility(d.player1, yesMan, ability, targets = listOf(ChosenTarget.Player(d.player2)))).error shouldBe null
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, bolt, listOf(yesMan)).error shouldBe null
        repeat(8) { if (d.state.stack.isNotEmpty()) d.bothPass() }
        (yesMan in d.getGraveyard(d.player1)) shouldBe true
        d.getHandSize(d.player1) shouldBe before
        d.getPermanents(d.player1).count { d.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 0
    }
    test("after an opponent controls it, dying uses last-known quest counters and gives tapped tokens to its owner") {
        val d = setup()
        val yesMan = d.putPermanentOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        d.addComponent(yesMan, CountersComponent(mapOf(CounterType.QUEST to 2)))
        d.submit(ActivateAbility(d.player1, yesMan, ability, targets = listOf(ChosenTarget.Player(d.player2)))).error shouldBe null
        d.bothPass()
        d.bothPass()
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, bolt, listOf(yesMan)).error shouldBe null
        repeat(8) { if (d.state.stack.isNotEmpty()) d.bothPass() }
        val tokens = d.getPermanents(d.player1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
        tokens.size shouldBe 3
        tokens.forEach { d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
        d.getPermanents(d.player2).count { d.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 0
    }
    test("activation is unavailable during an opponent's turn") {
        val d = setup()
        val yesMan = d.putPermanentOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.player2
        d.legalActions(d.player1).any {
            (it.action as? ActivateAbility)?.let { action -> action.sourceId == yesMan && action.abilityId == ability } == true
        } shouldBe false
    }
})

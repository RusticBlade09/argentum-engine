package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The target polarity of a fight ([com.wingedsheep.sdk.scripting.effects.FightEffect]).
 *
 * The creature fought is dealt damage: the victim's half reads harmful. The fighter's half names
 * nothing — its power is the weapon and the damage dealt back is the price, so which of our
 * creatures fights best is a board question the simulation answers, not a side of the table.
 */
class FightReadingAiTest : ScenarioTestBase() {

    private fun triggerSlots(name: String): List<TargetPolarity> {
        val trigger = cardRegistry.getCard(name)!!.script.triggeredAbilities.single()
        return TargetPolarityAnalyzer.forRequirements(trigger.effect, trigger.allTargetRequirements)
    }

    init {
        test("the creature fought reads as harmful") {
            withClue("Mind Meanderer: this creature fights up to one target creature an opponent controls") {
                triggerSlots("Mind Meanderer") shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("the fighter's slot stays unread") {
            withClue("Yoshimaru: another target creature you control fights up to one target creature") {
                triggerSlots("Yoshimaru, Scrappy Stray") shouldBe listOf(TargetPolarity.UNKNOWN, TargetPolarity.HARMFUL)
            }
        }
    }
}

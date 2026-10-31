package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A base-P/T change on a target ([com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect]).
 *
 * "Target creature has base power and toughness 0/0 until end of turn" (Multiply by Zero) is
 * removal: it kills anything not propped up by counters or a pump. Unread, the card had no intent
 * at all, so nothing that holds removal for a response or the end step saw it as removal. A base of
 * 1/1 or smaller is the curse side of the table; a larger base names nothing, and the slot's other
 * effects decide it.
 */
class SetBaseStatsReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a base 0/0 on a target creature is removal aimed at their side") {
            val card = cardRegistry.getCard("Multiply by Zero")!!
            withClue("its slot reads as harmful") {
                TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("and the card is removal") {
                intents.forName("Multiply by Zero")!!.tags shouldContain IntentTag.REMOVAL
            }
        }

        test("a larger base leaves the slot to the effects beside it") {
            // Taranika: untap target creature you control; it has base 4/4 and gains
            // indestructible — the untap and the keyword read the slot, the 4/4 does not veto them.
            val trigger = cardRegistry.getCard("Taranika, Akroan Veteran")!!.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(trigger.effect, trigger.allTargetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
        }
    }
}

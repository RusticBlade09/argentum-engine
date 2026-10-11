package com.wingedsheep.gameserver.deckbuilding

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class DeckbuildingChatReplyParsingTest : FunSpec({
    val json = Json { ignoreUnknownKeys = true; isLenient = true }

    test("a reasoning preamble full of mana costs doesn't hide the reply's JSON") {
        val raw = """
            Tweeze - {2}{R} Instant, Sear - {1}{R} Instant. Build BR.
            </think>

            {"message": "Go BR.", "actions": [{"type": "clear_highlights"}]}
        """.trimIndent()

        val obj = extractChatJsonObject(stripReasoning(raw), json).shouldNotBeNull()
        obj["message"]!!.jsonPrimitive.content shouldBe "Go BR."
        obj["actions"]!!.jsonArray.size shouldBe 1
    }

    test("an opening think tag is dropped with its block") {
        stripReasoning("<think>{W}{U} hmm</think>\n{\"message\": \"Hi\"}") shouldBe "{\"message\": \"Hi\"}"
    }

    test("a reply cut off mid-thought leaves nothing to show") {
        stripReasoning("<think>Looking at {B}{G} options") shouldBe ""
    }

    test("a reply without reasoning is untouched") {
        stripReasoning("  {\"message\": \"Hi\"}  ") shouldBe "{\"message\": \"Hi\"}"
    }
})

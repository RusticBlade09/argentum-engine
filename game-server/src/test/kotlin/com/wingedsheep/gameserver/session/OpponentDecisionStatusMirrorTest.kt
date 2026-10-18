package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.support.HiddenIdentityForkChecker
import com.wingedsheep.gameserver.protocol.ServerMessage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.full.primaryConstructor

/**
 * Interim guard for the hidden-identity fork checker's mirror of what the server sends a seat
 * whose opponent is deciding.
 *
 * `HiddenIdentityForkChecker` (rules-engine test fixtures) can't depend on game-server, so its
 * `OpponentDecisionView` mirrors [ServerMessage.OpponentDecisionStatus] by hand. This test fails
 * when either side gains or loses a field, so the mirror can't drift silently. It goes once the
 * enriched decision projection lives in rules-engine and both consume one implementation.
 */
class OpponentDecisionStatusMirrorTest : FunSpec({

    /** Each status field, and the mirror fields that stand for it. */
    val mirrored = mapOf(
        "playerId" to setOf("playerId"),
        "decisionType" to setOf("type"),
        // Derived from the decision's type, except an OrderObjectsDecision's own title.
        "displayText" to setOf("type", "orderingTitle"),
        "sourceName" to setOf("sourceName"),
        "sourceId" to setOf("sourceId"),
        "sourceImageUri" to setOf("sourceImageUri"),
    )

    fun fields(klass: kotlin.reflect.KClass<*>) = klass.primaryConstructor!!.parameters.mapNotNull { it.name }.toSet()

    test("every OpponentDecisionStatus field is mirrored") {
        fields(ServerMessage.OpponentDecisionStatus::class) shouldBe mirrored.keys
    }

    test("every mirror field stands for an OpponentDecisionStatus field") {
        fields(HiddenIdentityForkChecker.OpponentDecisionView::class) shouldBe mirrored.values.flatten().toSet()
    }
})

package com.wingedsheep.gameserver.stats

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant

class AiVsHumanAggregatorTest : FunSpec({

    // A Wednesday, so day / week / month buckets all differ.
    val now = Instant.parse("2026-10-14T12:00:00Z")
    fun daysAgo(d: Long) = now.minusSeconds(d * 86_400L)

    fun game(id: Long, at: Instant, outcome: AiMatchOutcome, mode: String = "CASUAL", version: String? = "abc") =
        AiMatchGame(id, at, mode, null, version, outcome)

    test("win rate is over decisive games only, with a Wilson interval around it") {
        val r = AiRecord.of(listOf(AiMatchOutcome.AI, AiMatchOutcome.HUMAN, AiMatchOutcome.HUMAN, AiMatchOutcome.DRAW))
        r shouldBe AiRecord(games = 4, aiWins = 1, humanWins = 2, draws = 1)
        r.aiWinRate!! shouldBe (1.0 / 3 plusOrMinus 1e-9)
        r.aiWinRateLow!! shouldBe (0.0615 plusOrMinus 1e-3)
        r.aiWinRateHigh!! shouldBe (0.7923 plusOrMinus 1e-3)
    }

    test("an all-draw record has no win rate") {
        AiRecord.of(listOf(AiMatchOutcome.DRAW)).aiWinRate.shouldBeNull()
        AiRecord.EMPTY.aiWinRateLow.shouldBeNull()
    }

    test("a perfect record still has a non-degenerate interval") {
        val r = AiRecord.of(List(3) { AiMatchOutcome.AI })
        r.aiWinRateHigh shouldBe 1.0
        (r.aiWinRateLow!! < 0.5) shouldBe true
    }

    test("the range and the window before it are split, and the series is gap-filled") {
        val games = listOf(
            game(1, daysAgo(12), AiMatchOutcome.HUMAN), // previous window
            game(2, daysAgo(5), AiMatchOutcome.AI),
            game(3, daysAgo(5), AiMatchOutcome.HUMAN),
            game(4, daysAgo(1), AiMatchOutcome.AI),
        )
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = 7, bucket = AiBucket.DAY, now = now)

        report.overall shouldBe AiRecord(3, 2, 1, 0)
        report.previous shouldBe AiRecord(1, 0, 1, 0)
        report.series.map { it.start }.first() shouldBe "2026-10-07"
        report.series.map { it.start }.last() shouldBe "2026-10-14"
        report.series.size shouldBe 8
        report.series.single { it.start == "2026-10-09" }.record shouldBe AiRecord(2, 1, 1, 0)
        report.series.single { it.start == "2026-10-10" }.record shouldBe AiRecord.EMPTY
    }

    test("all time has no previous window and starts the series at the first game") {
        val games = listOf(game(1, daysAgo(20), AiMatchOutcome.AI), game(2, daysAgo(2), AiMatchOutcome.HUMAN))
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = null, bucket = AiBucket.WEEK, now = now)
        report.previous.shouldBeNull()
        report.series.first().start shouldBe "2026-09-21" // the Monday on or before 2026-09-24
        report.series.last().start shouldBe "2026-10-12"
    }

    test("builds are newest first and games without a build are left out of them") {
        val games = listOf(
            game(1, daysAgo(6), AiMatchOutcome.HUMAN, version = "old"),
            game(2, daysAgo(3), AiMatchOutcome.AI, version = "new"),
            game(3, daysAgo(2), AiMatchOutcome.AI, version = null),
        )
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = 7, bucket = AiBucket.DAY, now = now)
        report.byBuild.map { it.version } shouldContainExactly listOf("new", "old")
        report.overall.games shouldBe 3
    }

    test("modes are ordered by games played") {
        val games = listOf(
            game(1, daysAgo(1), AiMatchOutcome.AI, mode = "TOURNAMENT"),
            game(2, daysAgo(1), AiMatchOutcome.AI, mode = "QUICK_GAME"),
            game(3, daysAgo(1), AiMatchOutcome.HUMAN, mode = "QUICK_GAME"),
        )
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = 7, bucket = AiBucket.DAY, now = now)
        report.byMode.map { it.gameMode } shouldContainExactly listOf("QUICK_GAME", "TOURNAMENT")
    }

    test("a human's record counts a drawn game as a draw and a lost pod as a loss") {
        val games = listOf(
            game(1, daysAgo(1), AiMatchOutcome.HUMAN),
            game(2, daysAgo(1), AiMatchOutcome.AI),
            game(3, daysAgo(1), AiMatchOutcome.DRAW),
            game(4, daysAgo(30), AiMatchOutcome.AI), // outside the range
        )
        val seats = listOf(
            AiMatchHumanSeat(1, "user:a", "Alice", isGuest = false, won = true),
            AiMatchHumanSeat(2, "user:a", "Alice", isGuest = false, won = false),
            AiMatchHumanSeat(3, "user:a", "Alice", isGuest = false, won = false),
            AiMatchHumanSeat(4, "user:a", "Alice", isGuest = false, won = false),
            AiMatchHumanSeat(1, "guest:Bob", "Bob", isGuest = true, won = false),
        )
        val report = AiVsHumanAggregator.aggregate(games, seats, days = 7, bucket = AiBucket.DAY, now = now)
        report.topHumans shouldContainExactly listOf(
            AiHumanRecord("Alice", isGuest = false, games = 3, wins = 1, losses = 1, draws = 1),
            AiHumanRecord("Bob", isGuest = true, games = 1, wins = 0, losses = 1, draws = 0),
        )
    }

    test("recent games keep only the latest, oldest first") {
        val games = (1L..50L).map { game(it, daysAgo(51 - it).plusSeconds(0), AiMatchOutcome.AI) }
        val report = AiVsHumanAggregator.aggregate(games, emptyList(), days = null, bucket = AiBucket.MONTH, now = now)
        report.recent.size shouldBe AiVsHumanAggregator.RECENT_GAMES
        report.recent.first().endedAt shouldBe daysAgo(40).toString()
        report.recent.last().endedAt shouldBe daysAgo(1).toString()
    }
})

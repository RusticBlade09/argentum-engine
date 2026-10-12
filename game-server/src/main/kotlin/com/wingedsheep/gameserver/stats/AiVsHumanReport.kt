package com.wingedsheep.gameserver.stats

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import kotlin.math.sqrt

/**
 * The admin "AI vs humans" report: how the built-in AI fares against human players, over time, per
 * deployed build and per game mode — the number to watch while improving the AI.
 *
 * The population is every recorded game with at least one AI seat *and* one human seat. Each game has
 * one [AiMatchOutcome]: the AI won (only AI seats won), the humans won (only human seats won), or a
 * draw — no winner, or an AI and a human that won together (a mixed Two-Headed Giant team). Win rates
 * are over *decisive* games only, so draws neither help nor hurt the AI.
 */
enum class AiMatchOutcome { AI, HUMAN, DRAW }

/** One AI-vs-human game, as read from the match-history tables. */
data class AiMatchGame(
    val matchId: Long,
    val endedAt: Instant,
    val gameMode: String?,
    val format: String?,
    /** The deploy that played it (`game_replays.engine_version`); null when no replay was stored. */
    val engineVersion: String?,
    val outcome: AiMatchOutcome,
)

/** A human seat in an AI-vs-human game. [playerKey] is the account id, or the guest's name. */
data class AiMatchHumanSeat(
    val matchId: Long,
    val playerKey: String,
    val displayName: String,
    val isGuest: Boolean,
    val won: Boolean,
)

/** A win/loss/draw tally from the AI's side, with its decisive win rate and a 95% Wilson interval. */
data class AiRecord(val games: Long, val aiWins: Long, val humanWins: Long, val draws: Long) {
    private val decisive: Long get() = aiWins + humanWins

    /** AI wins over decisive games; null when there were none. */
    val aiWinRate: Double? get() = if (decisive == 0L) null else aiWins.toDouble() / decisive

    /** Lower bound of the 95% Wilson score interval around [aiWinRate]. */
    val aiWinRateLow: Double? get() = wilson(-1)

    /** Upper bound of the 95% Wilson score interval around [aiWinRate]. */
    val aiWinRateHigh: Double? get() = wilson(+1)

    // Wilson rather than the normal approximation: the per-week samples here are small, and the
    // normal interval collapses to zero width at 0% / 100% — exactly where a sparse week lands.
    private fun wilson(sign: Int): Double? {
        val n = decisive.toDouble()
        if (n == 0.0) return null
        val p = aiWins / n
        val z = 1.96
        val z2 = z * z
        val centre = p + z2 / (2 * n)
        val margin = z * sqrt(p * (1 - p) / n + z2 / (4 * n * n))
        return ((centre + sign * margin) / (1 + z2 / n)).coerceIn(0.0, 1.0)
    }

    companion object {
        val EMPTY = AiRecord(0, 0, 0, 0)

        fun of(outcomes: Iterable<AiMatchOutcome>): AiRecord {
            var ai = 0L
            var human = 0L
            var draw = 0L
            for (o in outcomes) when (o) {
                AiMatchOutcome.AI -> ai++
                AiMatchOutcome.HUMAN -> human++
                AiMatchOutcome.DRAW -> draw++
            }
            return AiRecord(ai + human + draw, ai, human, draw)
        }
    }
}

/** One time bucket of the series; [start] is the bucket's first UTC day (`YYYY-MM-DD`). */
data class AiPeriodRecord(val start: String, val record: AiRecord)

data class AiModeRecord(val gameMode: String?, val format: String?, val record: AiRecord)

/** A deployed build's record. [firstSeen] / [lastSeen] bound the games it played in the range. */
data class AiBuildRecord(val version: String, val firstSeen: String, val lastSeen: String, val record: AiRecord)

data class AiRecentGame(val endedAt: String, val gameMode: String?, val format: String?, val outcome: AiMatchOutcome)

/** A human's record against the AI, from the human's side. */
data class AiHumanRecord(
    val name: String,
    // Pinned: Jackson would otherwise emit `guest` for an `is`-prefixed Boolean.
    @get:JsonProperty("isGuest") val isGuest: Boolean,
    val games: Long,
    val wins: Long,
    val losses: Long,
    val draws: Long,
)

data class AiVsHumanReport(
    /** Range length in days; null for all time. */
    val days: Int?,
    val bucket: AiBucket,
    val overall: AiRecord,
    /** The equally long window just before the range — null for all time. */
    val previous: AiRecord?,
    /** Oldest first, with empty buckets filled so the time axis is continuous. */
    val series: List<AiPeriodRecord>,
    /** Most games first. */
    val byMode: List<AiModeRecord>,
    /** Newest build first, at most [AiVsHumanAggregator.MAX_BUILDS]; games with no known build are left out. */
    val byBuild: List<AiBuildRecord>,
    /** The latest games, oldest first, at most [AiVsHumanAggregator.RECENT_GAMES]. */
    val recent: List<AiRecentGame>,
    /** The humans who have played the AI most, at most [AiVsHumanAggregator.MAX_HUMANS]. */
    val topHumans: List<AiHumanRecord>,
)

enum class AiBucket {
    DAY, WEEK, MONTH;

    /** The first UTC day of the bucket holding [day]. Weeks start on Monday. */
    fun startOf(day: LocalDate): LocalDate = when (this) {
        DAY -> day
        WEEK -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        MONTH -> day.withDayOfMonth(1)
    }

    fun next(start: LocalDate): LocalDate = when (this) {
        DAY -> start.plusDays(1)
        WEEK -> start.plusWeeks(1)
        MONTH -> start.plusMonths(1)
    }
}

/** Folds AI-vs-human games into an [AiVsHumanReport]. Pure, so it's tested without a database. */
object AiVsHumanAggregator {
    const val MAX_BUILDS = 10
    const val RECENT_GAMES = 40
    const val MAX_HUMANS = 10

    /**
     * @param games every AI-vs-human game since `now - 2 × days` (the range plus the window before
     *   it, for [AiVsHumanReport.previous]), or all of them when [days] is null.
     * @param humanSeats the human seats of those games.
     */
    fun aggregate(
        games: List<AiMatchGame>,
        humanSeats: List<AiMatchHumanSeat>,
        days: Int?,
        bucket: AiBucket,
        now: Instant,
    ): AiVsHumanReport {
        val rangeStart = days?.let { now.minusSeconds(it * 86_400L) }
        val inRange = games.filter { rangeStart == null || !it.endedAt.isBefore(rangeStart) }.sortedBy { it.endedAt }
        val previous = rangeStart?.let { start ->
            val prevStart = start.minusSeconds(days * 86_400L)
            AiRecord.of(games.filter { !it.endedAt.isBefore(prevStart) && it.endedAt.isBefore(start) }.map { it.outcome })
        }
        val inRangeIds = inRange.mapTo(HashSet()) { it.matchId }
        val outcomeById = inRange.associate { it.matchId to it.outcome }

        return AiVsHumanReport(
            days = days,
            bucket = bucket,
            overall = AiRecord.of(inRange.map { it.outcome }),
            previous = previous,
            series = series(inRange, bucket, rangeStart, now),
            byMode = inRange.groupBy { it.gameMode to it.format }
                .map { (key, g) -> AiModeRecord(key.first, key.second, AiRecord.of(g.map { it.outcome })) }
                .sortedWith(compareByDescending<AiModeRecord> { it.record.games }.thenBy { it.gameMode }),
            byBuild = inRange.filter { !it.engineVersion.isNullOrBlank() }
                .groupBy { it.engineVersion!! }
                .map { (version, g) ->
                    AiBuildRecord(
                        version = version,
                        firstSeen = g.first().endedAt.toString(),
                        lastSeen = g.last().endedAt.toString(),
                        record = AiRecord.of(g.map { it.outcome }),
                    )
                }
                .sortedByDescending { it.firstSeen }
                .take(MAX_BUILDS),
            recent = inRange.takeLast(RECENT_GAMES).map {
                AiRecentGame(it.endedAt.toString(), it.gameMode, it.format, it.outcome)
            },
            topHumans = humanSeats.filter { it.matchId in inRangeIds }
                .groupBy { it.playerKey }
                .map { (_, seats) ->
                    // A drawn game is a draw for every human in it; otherwise a seat that didn't win
                    // lost — in a pod, to the AI or to another human, either way not a win against it.
                    val draws = seats.count { outcomeById[it.matchId] == AiMatchOutcome.DRAW && !it.won }.toLong()
                    val wins = seats.count { it.won }.toLong()
                    AiHumanRecord(
                        name = seats.last().displayName,
                        isGuest = seats.last().isGuest,
                        games = seats.size.toLong(),
                        wins = wins,
                        losses = seats.size - wins - draws,
                        draws = draws,
                    )
                }
                .sortedWith(compareByDescending<AiHumanRecord> { it.games }.thenBy { it.name.lowercase() })
                .take(MAX_HUMANS),
        )
    }

    private fun series(games: List<AiMatchGame>, bucket: AiBucket, rangeStart: Instant?, now: Instant): List<AiPeriodRecord> {
        val firstDay = (rangeStart ?: games.firstOrNull()?.endedAt ?: return emptyList()).utcDay()
        val byBucket = games.groupBy { bucket.startOf(it.endedAt.utcDay()) }
        val last = bucket.startOf(now.utcDay())
        val out = ArrayList<AiPeriodRecord>()
        var cursor = bucket.startOf(firstDay)
        while (!cursor.isAfter(last)) {
            val outcomes = byBucket[cursor].orEmpty().map { it.outcome }
            out += AiPeriodRecord(cursor.toString(), if (outcomes.isEmpty()) AiRecord.EMPTY else AiRecord.of(outcomes))
            cursor = bucket.next(cursor)
        }
        return out
    }

    private fun Instant.utcDay(): LocalDate = atOffset(ZoneOffset.UTC).toLocalDate()
}

package com.wingedsheep.gameserver.ai

import com.wingedsheep.ai.engine.deck.GeneratedDeck
import com.wingedsheep.gameserver.controller.DecksController
import com.wingedsheep.gameserver.deck.DeckValidator
import com.wingedsheep.gameserver.protocol.DeckRequestConverter
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.Deck
import org.springframework.stereotype.Component
import kotlin.random.Random

/**
 * Picks a *different* built-in starter deck from the same set(s) as the human's deck, for an AI
 * seat on Auto.
 *
 * The starters come in families built against each other — Bloomburrow's six two-colour tribal
 * decks, the Standard lists — so a sibling is the fairest and most characterful opponent there is:
 * a tuned 60-card list facing a tuned 60-card list, both playing the set's mechanics. A deck
 * generated from the same pool is the fallback, not the first choice, because an auto-build from a
 * whole set is rarely as focused as a list someone curated.
 *
 * A candidate qualifies when it is built from the human's sets ([MIN_COVERAGE] of its non-basics
 * printed there), isn't the human's own deck in disguise ([MAX_OVERLAP]), has the shape the lobby
 * plays (a commander exactly when the game uses commanders), and passes the lobby's format.
 *
 * A tuned constructed deck spread over many sets has no set identity to match, so the resolver
 * matches it on power instead, by [formatTier]: the narrowest constructed format it is legal in.
 */
@Component
class PremadeOpponentPicker(
    private val setInference: DeckSetInference,
    private val deckValidator: DeckValidator,
) {
    /**
     * A sibling starter for [opponentDeck], or null when none qualifies.
     *
     * @param opponentDeck the human's submitted list (commander included, as the lobby holds it).
     * @param opponentSets the sets [DeckSetInference.setsOf] read off it; empty never matches.
     */
    fun pick(
        opponentDeck: Map<String, Int>,
        opponentSets: List<String>,
        format: DeckFormat?,
        commanderRules: Boolean,
        random: Random = Random.Default,
    ): GeneratedDeck? {
        if (opponentSets.isEmpty()) return null
        val opponentNames = setInference.nonBasicNames(opponentDeck)
        return DecksController.EXAMPLE_DECKS
            .filter { (it.commander != null) == commanderRules }
            .filter { setInference.coverage(it.cards, opponentSets) >= MIN_COVERAGE }
            .filter { overlap(setInference.nonBasicNames(it.cards), opponentNames) <= MAX_OVERLAP }
            .filter { format == null || isLegal(it, format) }
            .randomOrNull(random)
            ?.let { starter ->
                val commanders = listOfNotNull(starter.commander)
                GeneratedDeck(DeckRequestConverter.stripCommanders(starter.cards, commanders), starter.commander)
            }
    }

    /**
     * The narrowest constructed format [deck] is legal in — or null when it isn't tuned (most of its
     * non-basic copies outside playsets: a pile, not a list), or is legal in none of them.
     *
     * Pauper comes first because commons-only is a power ceiling of its own; past it the formats
     * nest, each card pool a superset of the one before. A lobby restriction [within] limits the
     * answer to tiers whose decks it admits, so the AI's deck stays legal there: Pauper only under
     * Pauper, Standard up to Modern under Modern, and null under a format outside the ladder
     * (Premodern, the singleton shapes).
     */
    fun formatTier(deck: Map<String, Int>, within: DeckFormat? = null): DeckFormat? {
        val tiers = when (within) {
            null -> listOf(DeckFormat.PAUPER) + NESTED_TIERS
            DeckFormat.PAUPER -> listOf(DeckFormat.PAUPER)
            in NESTED_TIERS -> NESTED_TIERS.subList(0, NESTED_TIERS.indexOf(within) + 1)
            else -> return null
        }
        if (!isTuned(deck)) return null
        return tiers.firstOrNull { deckValidator.validate(deck, it).valid }
    }

    /** At least [MIN_PLAYSET_SHARE] of the non-basic copies come three or more to a name. */
    private fun isTuned(deck: Map<String, Int>): Boolean {
        val nonBasics = setInference.nonBasicNames(deck)
        val copies = deck.filterKeys { it.substringBefore('#') in nonBasics }
            .entries
            .groupBy({ it.key.substringBefore('#') }, { it.value })
            .mapValues { (_, counts) -> counts.sum() }
            .values
        val total = copies.sum()
        return total > 0 && copies.filter { it >= PLAYSET_MIN }.sum() >= total * MIN_PLAYSET_SHARE
    }

    private fun isLegal(starter: DecksController.ExampleDeckDTO, format: DeckFormat): Boolean =
        if (starter.commander != null) {
            val library = DeckRequestConverter.stripCommanders(starter.cards, listOf(starter.commander))
            deckValidator.validate(
                Deck(cards = library.flatMap { (name, n) -> List(n) { name } }, commander = starter.commander),
                format,
            ).valid
        } else {
            deckValidator.validate(starter.cards, format).valid
        }

    /** Jaccard similarity of two name sets. */
    private fun overlap(a: Set<String>, b: Set<String>): Double {
        val union = (a + b).size
        return if (union == 0) 1.0 else (a intersect b).size.toDouble() / union
    }

    private companion object {
        const val MIN_COVERAGE = 0.9
        /** Past this, the "sibling" is the human's own starter (or a light edit of it). */
        const val MAX_OVERLAP = 0.5

        /** The formats whose card pools nest, weakest first; see [formatTier]. */
        val NESTED_TIERS = listOf(
            DeckFormat.STANDARD,
            DeckFormat.PIONEER,
            DeckFormat.MODERN,
            DeckFormat.LEGACY,
            DeckFormat.VINTAGE,
        )

        /** Three copies counts as a playset: lists run 3-ofs of cards they want but can't afford four of. */
        const val PLAYSET_MIN = 3
        const val MIN_PLAYSET_SHARE = 0.6
    }
}

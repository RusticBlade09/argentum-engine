/**
 * The state behind browsing the catalogue: the query (and what it parses to), the sort, the paged
 * slice on screen, and the reprint art an `s:` filter calls for.
 *
 * {@link CardBrowser} lays it out as one pane; the cube builder spreads the same state across the
 * deckbuilder's three columns (filters left, grid centre). Keeping it here means both browse cards
 * identically — same query language, same sort, same set-filter art.
 */
import { useEffect, useMemo, useState } from 'react'
import { isAdvancedQuery, parseQuery, type CardSummary } from '../cardFilter'
import { extractSetFilter } from '../query'
import { sortCards, type SortMode } from './cardSort'
import { useArtOverrides, useCardsWithSetArt, useSetPrintingOverride } from './useSetPrintingOverride'

const PAGE_SIZE = 120

export function useCardBrowse(catalog: readonly CardSummary[], initialQuery = '') {
  const [query, setQuery] = useState(initialQuery)
  const [sortMode, setSortMode] = useState<SortMode>('name')
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)

  const parseResult = useMemo(() => parseQuery(query, { withErrors: true }), [query])
  const advanced = useMemo(() => isAdvancedQuery(query), [query])
  const activeSetFilter = useMemo(() => extractSetFilter(parseResult.ast), [parseResult.ast])

  const predicate = parseResult.predicate
  const filtered = useMemo(
    () => sortCards(catalog.filter(predicate), sortMode),
    [catalog, predicate, sortMode],
  )

  useEffect(() => {
    setVisibleCount(PAGE_SIZE)
  }, [query, sortMode])

  // Only the visible page needs reprint art — asking for every match of a broad `s:` filter
  // would be a needlessly large request.
  const visibleNames = useMemo(
    () => filtered.slice(0, visibleCount).map((c) => c.name),
    [filtered, visibleCount],
  )
  const setOverrides = useSetPrintingOverride(activeSetFilter, visibleNames)
  const overrides = useArtOverrides(parseResult.ast, filtered, setOverrides)
  const withArt = useCardsWithSetArt(filtered, overrides)
  const displayed = useMemo(() => withArt.slice(0, visibleCount), [withArt, visibleCount])

  return {
    query,
    setQuery,
    sortMode,
    setSortMode,
    errors: parseResult.errors,
    advanced,
    filtered,
    displayed,
    /** Reprint art the active `s:` filter picked, by card name. */
    overrides,
    hasMore: displayed.length < filtered.length,
    showMore: () => setVisibleCount((c) => c + PAGE_SIZE),
  }
}

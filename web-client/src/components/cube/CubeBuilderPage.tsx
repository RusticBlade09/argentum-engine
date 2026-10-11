/**
 * The builder in cube mode — `/cubes/new` and `/cubes/edit/:cubeId`.
 *
 * Decks and cubes are built with the same tool: the deckbuilder's chrome, its filter rail and its
 * card grid, with the {@link BuilderModeSwitch} in the title flipping between them. What changes in
 * cube mode is the right rail and the rules behind it — no sideboard, format or copy cap; instead how
 * the cube becomes packs ({@link CubePackControls}), a rarity on every card ({@link CubeCardList}), and
 * the two cube-sized ways in: a whole set at its own rarities ({@link SetPane}) and a pasted list
 * ({@link PastePane}).
 *
 * The editing rules live in {@link useCubeDraft}, shared with the lobby's compact {@link CubeEditor}
 * dialog, so the two surfaces can't drift.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useMatch, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { listBasicLandSets } from '@/api/cubeSets'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { BuilderModeSwitch } from '@/components/deckbuilder/BuilderModeSwitch'
import { CardGrid, FilterSection, SearchBar, useCardBrowse, useCardCatalog } from '@/components/deckbuilder/browser'
import { PageShell, pageStyles as p } from '@/components/ui/PageShell'
import { type UnifiedCube, useUnifiedCubes } from '@/store/useUnifiedCubes'
import { CubeCardList } from './CubeCardList'
import { CubePackControls } from './CubePackControls'
import { cubeToText, isBoosterStyle } from './cubeModel'
import { CubeStats } from './CubeStats'
import { PastePane, SetPane } from './CubeSources'
import { ShareCubeDialog } from './ShareCubeDialog'
import { useCardHover } from './useCardHover'
import { type BasicLandSetOption, useCubeDraft } from './useCubeDraft'
import db from '@/components/deckbuilder/deckbuilder.module.css'
import editor from './CubeEditor.module.css'
import styles from './CubeBuilder.module.css'

/** Narrow screens show one column at a time; these tabs pick it. */
type Pane = 'cards' | 'filters' | 'cube'

/** A source dialog over the builder: a whole set, or a pasted list. */
type SourceDialog = 'set' | 'paste' | null

export function CubeBuilderPage() {
  const { cubeId } = useParams<{ cubeId?: string }>()
  const isNew = useMatch('/cubes/new') !== null
  const navigate = useNavigate()
  const library = useUnifiedCubes()
  const [basicLandSets, setBasicLandSets] = useState<BasicLandSetOption[]>([])
  /** A cube saved here that the library hook hasn't re-fetched yet. */
  const [savedHere, setSavedHere] = useState<UnifiedCube | null>(null)

  useEffect(() => {
    let cancelled = false
    void listBasicLandSets().then((sets) => {
      if (!cancelled) setBasicLandSets(sets)
    })
    return () => {
      cancelled = true
    }
  }, [])

  // A fresh working copy whenever the route opens a different cube — except for the redirect this
  // page makes itself after saving a new cube, which must keep the copy being edited.
  const routeKey = isNew ? 'new' : cubeId ?? 'new'
  const adoptedRef = useRef<string | null>(null)
  const [generation, setGeneration] = useState(0)
  const [openedKey, setOpenedKey] = useState(routeKey)
  if (routeKey !== openedKey) {
    setOpenedKey(routeKey)
    if (routeKey === adoptedRef.current) adoptedRef.current = null
    else setGeneration((g) => g + 1)
  }

  const cube = isNew || cubeId === undefined
    ? null
    : library.cubes.find((c) => c.id === cubeId) ?? (savedHere?.id === cubeId ? savedHere : null)

  if (!isNew && !cube) {
    return (
      <PageShell title={<BuilderModeSwitch mode="cube" />}>
        {library.ready ? (
          <div className={p.panel}>
            <p className={p.empty}>This cube isn’t in your library — it may have been deleted, or saved on another device.</p>
            <button type="button" className={p.button} onClick={() => navigate('/cubes')}>
              Back to your cubes
            </button>
          </div>
        ) : (
          <p className={p.empty}>Loading cube…</p>
        )}
      </PageShell>
    )
  }

  return (
    <CubeBuilder
      key={generation}
      cube={cube}
      availableSets={basicLandSets}
      onSaved={(saved) => {
        setSavedHere(saved)
        if (saved.id !== cubeId) {
          adoptedRef.current = saved.id
          navigate(`/cubes/edit/${encodeURIComponent(saved.id)}`, { replace: true })
        }
      }}
    />
  )
}

function CubeBuilder({
  cube,
  availableSets,
  onSaved,
}: {
  cube: UnifiedCube | null
  availableSets: readonly BasicLandSetOption[]
  onSaved: (cube: UnifiedCube) => void
}) {
  const navigate = useNavigate()
  const [search] = useSearchParams()
  const { catalog, setInfos, loading, error } = useCardCatalog()
  const byName = useMemo(() => {
    const map = new Map<string, CardSummary>()
    for (const card of catalog) map.set(card.name.toLowerCase(), card)
    return map
  }, [catalog])
  const lookup = useCallback((name: string) => byName.get(name.toLowerCase()), [byName])
  const draft = useCubeDraft({ cube, availableSets, lookup, catalogReady: !loading && !error, onSaved })
  const { entries, totalCards, unresolved, dirty, saving } = draft

  // Basics aren't cube cards — every cube's deckbuilding hands them out from its land-art set.
  const browsable = useMemo(() => catalog.filter((c) => !c.basicLand), [catalog])
  const browse = useCardBrowse(browsable)

  const from = search.get('from')
  const [dialog, setDialog] = useState<SourceDialog>(from === 'paste' ? 'paste' : from === 'set' ? 'set' : null)
  const [pane, setPane] = useState<Pane>('cards')
  const [toolsOpen, setToolsOpen] = useState(false)
  const [shareOpen, setShareOpen] = useState(false)
  /** Where an exit with unsaved changes was headed, while the confirm bar asks about it. */
  const [pendingExit, setPendingExit] = useState<(() => void) | null>(null)
  const { onHover, preview } = useCardHover()

  const guard = (proceed: () => void) => (dirty ? setPendingExit(() => proceed) : proceed())
  const openTool = (action: () => void) => () => {
    setToolsOpen(false)
    action()
  }

  return (
    <PageShell title={<BuilderModeSwitch mode="cube" guard={guard} />} fit width="full" plain>
      <div className={db.toolbar}>
        <div className={db.paneTabs} role="tablist" aria-label="Cube builder panes">
          {(['cards', 'filters', 'cube'] as const).map((p) => (
            <button
              key={p}
              type="button"
              role="tab"
              className={db.paneTab}
              aria-selected={pane === p}
              onClick={() => setPane(p)}
            >
              {p === 'cards' ? 'Cards' : p === 'filters' ? 'Filters' : (
                <>Cube <span className={db.paneTabCount}>{totalCards}</span></>
              )}
            </button>
          ))}
        </div>
        <div className={db.toolbarSpacer} />
        {toolsOpen && <div className={db.toolsBackdrop} onClick={() => setToolsOpen(false)} aria-hidden />}
        <div className={db.tools} data-open={toolsOpen}>
          <button type="button" className={db.toolButton} onClick={openTool(() => guard(() => navigate('/cubes')))}>
            My cubes
          </button>
          <button type="button" className={db.toolButton} onClick={openTool(() => setDialog('set'))}>
            Add a set
          </button>
          <button type="button" className={db.toolButton} onClick={openTool(() => setDialog('paste'))}>
            Import
          </button>
          <button
            type="button"
            className={db.toolButton}
            disabled={totalCards === 0}
            onClick={openTool(() => void draft.copyList())}
            title="Copy the cube as a text list"
          >
            Copy list
          </button>
          <button type="button" className={db.toolButton} disabled={totalCards === 0} onClick={openTool(() => setShareOpen(true))}>
            Share
          </button>
          <button type="button" className={db.toolButton} onClick={openTool(() => guard(() => navigate('/cubes/new')))}>
            New cube
          </button>
        </div>
        <button
          type="button"
          className={db.moreButton}
          onClick={() => setToolsOpen((v) => !v)}
          aria-expanded={toolsOpen}
          aria-haspopup="menu"
        >
          More
          <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden focusable="false">
            <path d="M2.5 4.5L6 8l3.5-3.5" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
        <span className={styles.saveState} data-dirty={dirty && !draft.notice} aria-live="polite">
          {draft.saveState}
        </span>
        <button
          type="button"
          className={`${db.newDeckButton} ${styles.saveButton}`}
          disabled={saving || totalCards === 0 || (!dirty && draft.saved !== null)}
          onClick={() => void draft.save()}
        >
          {saving ? 'Saving…' : 'Save'}
        </button>
      </div>

      {pendingExit && (
        <div className={`${editor.confirmBar} ${styles.banner}`} role="alert">
          <span>This cube has unsaved changes.</span>
          <button
            type="button"
            className={editor.primaryButton}
            disabled={saving || totalCards === 0}
            onClick={async () => {
              if (await draft.save()) {
                pendingExit()
                setPendingExit(null)
              }
            }}
          >
            Save and continue
          </button>
          <button
            type="button"
            className={editor.secondaryButton}
            onClick={() => {
              pendingExit()
              setPendingExit(null)
            }}
          >
            Discard changes
          </button>
          <button type="button" className={editor.linkButton} onClick={() => setPendingExit(null)}>
            Keep editing
          </button>
        </div>
      )}
      {draft.saveError && <div className={`${editor.warning} ${styles.banner}`}>{draft.saveError}</div>}

      <div className={`${db.workspace} ${styles.workspace}`} data-pane={pane}>
        <aside className={`${db.left} ${styles.paneLeft}`}>
          <section className={styles.startCard} aria-label="Add a whole set">
            <span className={styles.startTitle}>Start from a set</span>
            <span className={styles.startText}>
              Add a whole set at its own rarities — then extend it with cards from anywhere.
            </span>
            <button type="button" className={editor.primaryButton} onClick={() => setDialog('set')}>
              Add a set…
            </button>
          </section>
          <FilterSection
            query={browse.query}
            onQueryChange={browse.setQuery}
            catalog={browsable}
            setInfos={setInfos}
            advanced={browse.advanced}
          />
        </aside>

        <main className={`${db.center} ${styles.paneCenter}`}>
          <SearchBar
            query={browse.query}
            onQueryChange={browse.setQuery}
            sortMode={browse.sortMode}
            onSortChange={browse.setSortMode}
            errors={browse.errors}
            resultLabel={loading ? 'Loading…' : `Showing ${browse.displayed.length} of ${browse.filtered.length}`}
          />
          {error ? (
            <p className={styles.notice}>Couldn’t load the card catalogue: {error}</p>
          ) : (
            <CardGrid
              cards={browse.displayed}
              deckCards={draft.counts}
              onAdd={(card) => draft.addOne(card.name)}
              onRemove={draft.removeOne}
              hasMore={browse.hasMore}
              onShowMore={browse.showMore}
            />
          )}
        </main>

        <aside className={`${db.right} ${styles.rail}`}>
          <div className={styles.identity}>
            <input
              className={editor.titleInput}
              value={draft.name}
              onChange={(e) => draft.setName(e.target.value)}
              placeholder="Name your cube"
              aria-label="Cube name"
            />
            <input
              className={editor.descriptionInput}
              value={draft.description}
              onChange={(e) => draft.setDescription(e.target.value)}
              placeholder="What it plays like, who it's for"
              aria-label="Cube description"
              maxLength={280}
            />
          </div>

          <div className={styles.railSection}>
            <CubePackControls draft={draft} layout="stacked" />
          </div>

          {unresolved.length > 0 && (
            <div className={`${editor.warning} ${styles.railWarning}`}>
              <span>
                <strong>{unresolved.length}</strong> card{unresolved.length === 1 ? ' isn’t' : 's aren’t'} implemented
                yet, so this cube can’t be played.
              </span>
              <button type="button" className={editor.secondaryButton} onClick={draft.dropUnresolved}>
                Drop {unresolved.length === 1 ? 'it' : 'them'}
              </button>
            </div>
          )}

          <div className={styles.listHeader}>
            <span className={styles.listTitle}>
              {totalCards} card{totalCards === 1 ? '' : 's'}
              {entries.length !== totalCards ? ` · ${entries.length} different` : ''}
            </span>
            {entries.length > 0 && (
              <button type="button" className={editor.linkButton} onClick={() => draft.setEntries([])}>
                Clear all
              </button>
            )}
          </div>
          <CubeCardList
            entries={entries}
            lookup={lookup}
            catalogReady={!loading}
            rarityMatters={isBoosterStyle(draft.boosterStyle)}
            onHover={onHover}
            onCount={draft.setCount}
            onRarity={draft.setRarity}
            emptyHint={
              <>
                <p className={editor.emptyTitle}>This cube is empty</p>
                <p className={editor.hint}>
                  Click cards in the grid to add them, or start from a whole set with <strong>Add a set</strong>.
                  A shuffled cube of 360 cards seats 8 players at 3 packs of 15.
                </p>
              </>
            }
          />
        </aside>
      </div>

      {/* The cube's balance numbers. On a phone they belong to the Cube pane, not under the grid. */}
      <div className={styles.footer} data-mobile-hidden={pane !== 'cube'}>
        <CubeStats entries={entries} lookup={lookup} boosterStyle={draft.boosterStyle} packSize={draft.packSize} />
      </div>

      {dialog && (
        <>
          <div className={styles.dialogBackdrop} onClick={() => setDialog(null)} aria-hidden />
          <div
            className={styles.dialog}
            role="dialog"
            aria-label={dialog === 'set' ? 'Add a set' : 'Import a list'}
          >
            <div className={styles.dialogHeader}>
              <span className={styles.dialogTitle}>{dialog === 'set' ? 'Add a set' : 'Import a list'}</span>
              <button type="button" className={editor.secondaryButton} onClick={() => setDialog(null)}>
                Done
              </button>
            </div>
            <div className={styles.dialogBody}>
              {dialog === 'set' ? (
                <SetPane inCube={draft.inCube} onAdd={draft.addFromSet} />
              ) : (
                <PastePane
                  catalog={catalog}
                  cubeIsEmpty={entries.length === 0}
                  onImport={(list, replace) => {
                    draft.importList(list, replace)
                    setDialog(null)
                  }}
                  onSuggestName={draft.setName}
                />
              )}
            </div>
          </div>
        </>
      )}

      {shareOpen && (
        <ShareCubeDialog
          saved={draft.saved}
          dirty={dirty}
          isLoggedIn={draft.isLoggedIn}
          text={cubeToText(draft.shared)}
          onSave={draft.save}
          share={draft.shareSaved}
          unshare={draft.unshareSaved}
          onClose={() => setShareOpen(false)}
        />
      )}

      {preview}
    </PageShell>
  )
}

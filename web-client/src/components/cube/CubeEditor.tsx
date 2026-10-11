/**
 * CubeEditor — build and curate a cube (a hand-picked card pool used as a limited pack source).
 *
 * One editor, two frames: a full page on `/cubes` (`variant="page"`) and a dialog over the lobby
 * (`variant="dialog"`), where it can also hand the cube straight to the table. Everything between the
 * frames is shared, so the two never drift.
 *
 * A cube is a list of card *names* with counts — and, for the booster styles, a rarity per card — so
 * this editor never resolves printings and never decides what a pack contains: it edits a list and
 * hands it back. The lobby sends it to the server (`UpdateLobbySettings.cubeCards`), which resolves
 * it against the card registry and remains the authority on packs, capacity and legality.
 *
 * Layout: name and actions on top; how the cube becomes packs (the {@link BOOSTER_STYLES} choice,
 * with the pack drawn slot by slot) beneath; then the two working columns — where cards come from
 * ({@link SearchPane}, {@link SetPane}, {@link PastePane}) and what the cube holds
 * ({@link CubeCardList}) — and the balance numbers ({@link CubeStats}) along the bottom.
 *
 * Unimplemented cards are the one cube-specific rule: a cube with unresolvable names is *unplayable*
 * (the server rejects it wholesale), so they're surfaced in red with a one-click "drop them" action and
 * "Use this cube" stays disabled until the list is clean. Saving a still-dirty cube is allowed — sets
 * ship continuously, and a cube that names a card landing next month is a legitimate thing to keep.
 */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import type { CubeBoosterStyle, CubeRarity, SharedCube, SharedCubeEntry } from '@/api/account'
import { DEFAULT_CUBE_PACK_SIZE, cubeCardCount } from '@/store/cubeLibrary'
import { type UnifiedCube, cubeToShared, useUnifiedCubes } from '@/store/useUnifiedCubes'
import { CubeCardList } from './CubeCardList'
import { BOOSTER_STYLES, boosterStyleInfo, cubeToText, isBoosterStyle, mergeEntries, packHealth, rarityTallies } from './cubeModel'
import { CubeStats, PackStrip } from './CubeStats'
import { PastePane, SOURCE_TAB_LABEL, SearchPane, SetPane, type SourceTab } from './CubeSources'
import { ShareCubeDialog } from './ShareCubeDialog'
import { useCardHover } from './useCardHover'
import { useCardCatalog } from './useCardCatalog'
import styles from './CubeEditor.module.css'

/** A set the cube can take its basic-land art from. */
export interface BasicLandSetOption {
  readonly code: string
  readonly name: string
}

/** The name a new cube starts with — replaced by a set's name when the first thing added is a set. */
const DEFAULT_NAME = 'My Cube'

const SOURCE_TABS: readonly SourceTab[] = ['SEARCH', 'SET', 'PASTE']

interface CubeEditorProps {
  /** Cube to edit, or null to start a new one. */
  readonly cube: UnifiedCube | null
  /** Sets offered as the basic-land art source (a cube has no basics of its own). */
  readonly availableSets: readonly BasicLandSetOption[]
  /** Full page (`/cubes`) or a dialog over the lobby. */
  readonly variant?: 'page' | 'dialog'
  /** Which way of adding cards the left column opens on. Default: Add a set when empty, else Search. */
  readonly initialTab?: SourceTab | undefined
  readonly onClose: () => void
  /** Told about every successful save — the page uses it to move a new cube onto its own URL. */
  readonly onSaved?: (cube: UnifiedCube) => void
  /**
   * Called when the host commits the cube to the lobby. Only reachable once every name resolves —
   * an unresolved cube can't start a game.
   */
  readonly onUse?: (cube: SharedCube) => void
}

export function CubeEditor({ cube, availableSets, variant = 'dialog', initialTab, onClose, onSaved, onUse }: CubeEditorProps) {
  const { cubes, saveCube, share, unshare, isLoggedIn } = useUnifiedCubes()
  const catalog = useCardCatalog()

  const [saved, setSaved] = useState<UnifiedCube | null>(cube)
  const [name, setName] = useState(cube?.name ?? DEFAULT_NAME)
  const [description, setDescription] = useState(cube?.description ?? '')
  const [entries, setEntries] = useState<SharedCubeEntry[]>(() => cube?.cards.map((e) => ({ ...e })) ?? [])
  const [packSize, setPackSize] = useState(cube?.packSize ?? DEFAULT_CUBE_PACK_SIZE)
  const [boosterStyle, setBoosterStyle] = useState<CubeBoosterStyle>(cube?.boosterStyle ?? 'SHUFFLED')
  const [basicLandSetCode, setBasicLandSetCode] = useState(
    cube?.basicLandSetCode ?? availableSets[0]?.code ?? '',
  )
  const [tab, setTab] = useState<SourceTab>(initialTab ?? (entries.length === 0 ? 'SET' : 'SEARCH'))
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [shareOpen, setShareOpen] = useState(false)
  const [confirmingLeave, setConfirmingLeave] = useState(false)

  const shared: SharedCube = useMemo(
    () =>
      cubeToShared({
        name: name.trim() || 'Untitled cube',
        description: description.trim(),
        cards: entries,
        basicLandSetCode,
        packSize,
        boosterStyle,
      }),
    [name, description, entries, basicLandSetCode, packSize, boosterStyle],
  )

  /** What was last saved (or opened), to tell unsaved edits apart from a clean cube. */
  const [baseline, setBaseline] = useState(() => JSON.stringify(shared))
  const dirty = JSON.stringify(shared) !== baseline

  // The page renders before the server's set list has arrived; adopt the first set once it does —
  // into the baseline too, since that is a default arriving, not an edit.
  useEffect(() => {
    if (basicLandSetCode !== '' || !availableSets[0]) return
    const code = availableSets[0].code
    setBasicLandSetCode(code)
    setBaseline((prev) => JSON.stringify({ ...JSON.parse(prev), basicLandSetCode: code }))
  }, [availableSets, basicLandSetCode])

  // A tab close would drop unsaved work without asking; the browser's own prompt is the only one
  // that can run there. In-app exits ask through the confirm bar below instead.
  useEffect(() => {
    if (!dirty) return
    const warn = (e: BeforeUnloadEvent) => e.preventDefault()
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [dirty])

  const { onHover: handleHover, preview } = useCardHover()

  const lookup = useCallback((cardName: string) => catalog.byName.get(cardName.toLowerCase()), [catalog.byName])
  const totalCards = cubeCardCount(entries)
  const inCube = useMemo(() => new Set(entries.map((e) => e.name.toLowerCase())), [entries])
  /** Names the catalog doesn't know: the cube is unplayable until these are gone. */
  const unresolved = useMemo(
    () => (catalog.ready ? entries.filter((e) => !catalog.byName.has(e.name.toLowerCase())) : []),
    [entries, catalog],
  )
  const health = useMemo(
    () => packHealth(boosterStyle, rarityTallies(entries, (n) => lookup(n)?.rarity).distinct),
    [boosterStyle, entries, lookup],
  )
  const shortSlots = useMemo(
    () => new Set<string>(health?.lines.filter((l) => !l.ok).map((l) => l.slot) ?? []),
    [health],
  )

  const flash = (message: string) => {
    setNotice(message)
    window.setTimeout(() => setNotice((current) => (current === message ? null : current)), 2400)
  }

  function setCount(cardName: string, count: number) {
    setEntries((prev) =>
      count <= 0 ? prev.filter((e) => e.name !== cardName) : prev.map((e) => (e.name === cardName ? { ...e, count } : e)),
    )
  }

  function setRarity(cardName: string, rarity: CubeRarity | null) {
    setEntries((prev) =>
      prev.map((e) => {
        if (e.name !== cardName) return e
        return rarity ? { ...e, rarity } : { name: e.name, count: e.count }
      }),
    )
  }

  function addOne(cardName: string) {
    setEntries((prev) => mergeEntries(prev, [{ name: cardName, count: 1 }]).entries)
  }

  function addFromSet(additions: SharedCubeEntry[], set: { code: string; name: string }) {
    const wasEmpty = entries.length === 0
    const { entries: merged, added } = mergeEntries(entries, additions)
    setEntries(merged)
    // The first set into an empty cube is what the cube *is* — offer its name and its basics.
    if (wasEmpty) {
      if (name.trim() === '' || name === DEFAULT_NAME) setName(`${set.name} Cube`)
      if (availableSets.some((s) => s.code === set.code)) setBasicLandSetCode(set.code)
    }
    flash(`Added ${added} card${added === 1 ? '' : 's'} from ${set.name}`)
  }

  function importList(list: SharedCubeEntry[], replace: boolean) {
    if (replace) {
      setEntries(list)
      flash(`Imported ${cubeCardCount(list)} cards`)
    } else {
      const { entries: merged, added } = mergeEntries(entries, list)
      setEntries(merged)
      flash(`Added ${added} card${added === 1 ? '' : 's'}`)
    }
  }

  function dropUnresolved() {
    const misses = new Set(unresolved.map((e) => e.name))
    setEntries((prev) => prev.filter((e) => !misses.has(e.name)))
  }

  async function handleSave(): Promise<UnifiedCube | null> {
    setSaveError(null)
    setSaving(true)
    try {
      // Route back to the copy this editor opened, or to a same-named cube in the library.
      const target = saved ?? cubes.find((c) => c.name.toLowerCase() === shared.name.toLowerCase())
      const result = await saveCube(shared, target)
      setSaved(result)
      setBaseline(JSON.stringify(shared))
      flash(result.online ? 'Saved to your account' : 'Saved on this device')
      onSaved?.(result)
      return result
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Failed to save cube')
      return null
    } finally {
      setSaving(false)
    }
  }

  async function copyList() {
    try {
      await navigator.clipboard.writeText(cubeToText(shared))
      flash('Cube list copied')
    } catch {
      setSaveError('Your browser blocked the clipboard.')
    }
  }

  const requestClose = () => (dirty ? setConfirmingLeave(true) : onClose())

  const canUse = totalCards > 0 && unresolved.length === 0 && basicLandSetCode !== ''
  const styleInfo = boosterStyleInfo(boosterStyle)
  const landOptions = availableSets.some((s) => s.code === basicLandSetCode) || basicLandSetCode === ''
    ? availableSets
    : [{ code: basicLandSetCode, name: basicLandSetCode }, ...availableSets]
  const saveState = notice
    ?? (dirty ? 'Unsaved changes' : saved ? (saved.online ? 'Saved to your account' : 'Saved on this device') : '')

  const editor = (
    <div
      className={variant === 'page' ? styles.page : styles.dialog}
      role={variant === 'dialog' ? 'dialog' : undefined}
      aria-label="Cube editor"
    >
      <header className={styles.header}>
        <div className={styles.titleBlock}>
          <input
            className={styles.titleInput}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Name your cube"
            aria-label="Cube name"
          />
          <input
            className={styles.descriptionInput}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Add a description — what it plays like, who it's for"
            aria-label="Cube description"
            maxLength={280}
          />
        </div>
        <div className={styles.headerActions}>
          <span className={styles.saveState} data-dirty={dirty && !notice} aria-live="polite">
            {saveState}
          </span>
          <button type="button" className={styles.secondaryButton} disabled={totalCards === 0} onClick={() => void copyList()}>
            Copy list
          </button>
          <button type="button" className={styles.secondaryButton} disabled={totalCards === 0} onClick={() => setShareOpen(true)}>
            Share
          </button>
          <button
            type="button"
            className={onUse ? styles.secondaryButton : styles.primaryButton}
            disabled={saving || totalCards === 0 || (!dirty && saved !== null)}
            onClick={() => void handleSave()}
          >
            {saving ? 'Saving…' : 'Save'}
          </button>
          {onUse && (
            <button
              type="button"
              className={styles.primaryButton}
              disabled={!canUse || saving}
              title={
                canUse
                  ? undefined
                  : unresolved.length > 0
                    ? `${unresolved.length} cards in this cube aren't implemented yet`
                    : 'Add cards to the cube first'
              }
              onClick={async () => {
                if (!canUse) return
                if (!dirty && saved) onUse(shared)
                else if (await handleSave()) onUse(shared)
              }}
            >
              Use this cube
            </button>
          )}
          <button type="button" className={styles.secondaryButton} onClick={requestClose}>
            {variant === 'page' ? 'Done' : 'Close'}
          </button>
        </div>
      </header>

      {confirmingLeave && (
        <div className={styles.confirmBar} role="alert">
          <span>This cube has unsaved changes.</span>
          <button
            type="button"
            className={styles.primaryButton}
            disabled={saving || totalCards === 0}
            onClick={async () => {
              if (await handleSave()) onClose()
            }}
          >
            Save and close
          </button>
          <button type="button" className={styles.secondaryButton} onClick={onClose}>
            Discard changes
          </button>
          <button type="button" className={styles.linkButton} onClick={() => setConfirmingLeave(false)}>
            Keep editing
          </button>
        </div>
      )}

      <div className={styles.packBar}>
        <div className={styles.field}>
          <span className={styles.fieldLabel} id="cube-pack-style">Packs</span>
          <div className={styles.segmented} role="radiogroup" aria-labelledby="cube-pack-style">
            {BOOSTER_STYLES.map((option) => (
              <button
                key={option.style}
                type="button"
                role="radio"
                aria-checked={boosterStyle === option.style}
                className={styles.segment}
                onClick={() => setBoosterStyle(option.style)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
        <div className={styles.packExplain}>
          {isBoosterStyle(boosterStyle) && <PackStrip style={boosterStyle} short={shortSlots} />}
          <span className={styles.packBlurb}>{styleInfo.blurb}</span>
        </div>
        {!isBoosterStyle(boosterStyle) && (
          <label className={styles.field}>
            <span className={styles.fieldLabel}>Pack size</span>
            <input
              className={`${styles.input} ${styles.numberInput}`}
              type="number"
              min={1}
              max={30}
              value={packSize}
              onChange={(e) => setPackSize(Math.max(1, Math.min(30, Number(e.target.value) || 1)))}
            />
          </label>
        )}
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Basic land art</span>
          <select className={styles.select} value={basicLandSetCode} onChange={(e) => setBasicLandSetCode(e.target.value)}>
            {landOptions.map((set) => (
              <option key={set.code} value={set.code}>
                {set.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      {unresolved.length > 0 && (
        <div className={styles.warning}>
          <span>
            <strong>{unresolved.length}</strong> of {entries.length} cards aren&apos;t implemented yet, so this cube
            can&apos;t be played: {unresolved.slice(0, 6).map((e) => e.name).join(', ')}
            {unresolved.length > 6 ? `, +${unresolved.length - 6} more` : ''}
          </span>
          <button type="button" className={styles.secondaryButton} onClick={dropUnresolved}>
            Drop the {unresolved.length} unimplemented card{unresolved.length === 1 ? '' : 's'}
          </button>
        </div>
      )}

      {saveError && <div className={styles.warning}>{saveError}</div>}

      <div className={styles.body}>
        <section className={styles.column} aria-label="Add cards">
          <div className={styles.sourceTabs} role="tablist" aria-label="Add cards by">
            {SOURCE_TABS.map((t) => (
              <button
                key={t}
                type="button"
                role="tab"
                aria-selected={tab === t}
                className={styles.sourceTab}
                onClick={() => setTab(t)}
              >
                {SOURCE_TAB_LABEL[t]}
              </button>
            ))}
          </div>
          {tab === 'SEARCH' && (
            <SearchPane
              catalog={catalog.cards}
              catalogReady={catalog.ready}
              inCube={inCube}
              onAdd={addOne}
              onHover={handleHover}
            />
          )}
          {tab === 'SET' && <SetPane inCube={inCube} onAdd={addFromSet} />}
          {tab === 'PASTE' && (
            <PastePane catalog={catalog.cards} cubeIsEmpty={entries.length === 0} onImport={importList} onSuggestName={setName} />
          )}
        </section>

        <section className={styles.column} aria-label="Cards in the cube">
          <div className={styles.columnHeader}>
            <span className={styles.columnHeaderTitle}>
              {totalCards} card{totalCards === 1 ? '' : 's'}
              {entries.length !== totalCards ? ` (${entries.length} different)` : ''}
            </span>
            {entries.length > 0 && (
              <button type="button" className={styles.linkButton} onClick={() => setEntries([])}>
                Clear all
              </button>
            )}
          </div>
          <CubeCardList
            entries={entries}
            lookup={lookup}
            catalogReady={catalog.ready}
            rarityMatters={isBoosterStyle(boosterStyle)}
            onHover={handleHover}
            onCount={setCount}
            onRarity={setRarity}
            emptyHint={
              <>
                <p className={styles.emptyTitle}>This cube is empty</p>
                <p className={styles.hint}>
                  Start from a set with <strong>Add a set</strong> — a whole set at its own rarities, ready to open
                  as boosters — or paste a list you already have. A shuffled cube of 360 cards seats 8 players at 3
                  packs of 15.
                </p>
              </>
            }
          />
        </section>
      </div>

      <CubeStats entries={entries} lookup={lookup} boosterStyle={boosterStyle} packSize={packSize} />

      {shareOpen && (
        <ShareCubeDialog
          saved={saved}
          dirty={dirty}
          isLoggedIn={isLoggedIn}
          text={cubeToText(shared)}
          onSave={handleSave}
          share={async (c) => {
            const code = await share(c)
            setSaved((prev) => (prev ? { ...prev, shareCode: code } : prev))
            return code
          }}
          unshare={async (c) => {
            await unshare(c)
            setSaved((prev) => (prev ? { ...prev, shareCode: null } : prev))
          }}
          onClose={() => setShareOpen(false)}
        />
      )}
    </div>
  )

  if (variant === 'page') {
    return (
      <>
        {editor}
        {preview}
      </>
    )
  }

  // Portalled to <body>: the lobby panel this opens from sits inside a `backdrop-filter` ancestor,
  // which is a containing block for `position: fixed` — a full-screen dialog rendered in place would
  // size itself to the lobby's scroll height instead of the viewport.
  return createPortal(
    <>
      <div className={styles.backdrop} onClick={requestClose} />
      {editor}
      {preview}
    </>,
    document.body,
  )
}

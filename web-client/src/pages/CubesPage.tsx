/**
 * Cubes — the user's cube library, and the full-page cube editor.
 *
 * - `/cubes` — every saved cube (account and this device, merged by `useUnifiedCubes`) as a tile:
 *   name, description, colour balance, pack style, where it's saved and whether it's shared, with
 *   Play, Edit, Share, Duplicate and Delete. An empty library teaches the two ways to start.
 * - `/cubes/new` — the editor on a blank cube (`?from=paste` opens it on the Paste tab).
 * - `/cubes/edit/:cubeId` — the editor on a saved cube.
 *
 * The lobby keeps its own way in (the Card source → Cube panel opens the same editor as a dialog);
 * this page is for curating without a lobby open, and it is where share links and "Play this cube"
 * start from.
 */
import { useEffect, useMemo, useState } from 'react'
import { useMatch, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { listBasicLandSets } from '@/api/cubeSets'
import { CubeEditor, type BasicLandSetOption } from '@/components/cube/CubeEditor'
import { CubePlayButton } from '@/components/cube/CubePlayButton'
import { colorAndCurve } from '@/components/cube/CubeStats'
import { boosterStyleInfo, cubeToText, isBoosterStyle, uniqueCubeName } from '@/components/cube/cubeModel'
import { ShareCubeDialog } from '@/components/cube/ShareCubeDialog'
import { useCardCatalog, type CardCatalog } from '@/components/cube/useCardCatalog'
import { PageShell, pageStyles as p } from '@/components/ui/PageShell'
import { cubeCardCount } from '@/store/cubeLibrary'
import { type UnifiedCube, cubeToShared, useUnifiedCubes } from '@/store/useUnifiedCubes'
import styles from '@/components/cube/CubesPage.module.css'

export function CubesPage() {
  const { cubeId } = useParams<{ cubeId?: string }>()
  const [search] = useSearchParams()
  const navigate = useNavigate()
  const library = useUnifiedCubes()
  const [basicLandSets, setBasicLandSets] = useState<BasicLandSetOption[]>([])
  /** A cube saved by this page's editor, which the library hook here hasn't re-fetched yet. */
  const [savedHere, setSavedHere] = useState<UnifiedCube | null>(null)

  const isNew = useMatch('/cubes/new') !== null
  const editing = isNew || cubeId !== undefined

  useEffect(() => {
    if (!editing) return
    let cancelled = false
    void listBasicLandSets().then((sets) => {
      if (!cancelled) setBasicLandSets(sets)
    })
    return () => {
      cancelled = true
    }
  }, [editing])

  if (editing) {
    const cube = cubeId === undefined
      ? null
      : library.cubes.find((c) => c.id === cubeId) ?? (savedHere?.id === cubeId ? savedHere : null)
    const waiting = cubeId !== undefined && !cube && !library.ready
    return (
      <PageShell title="Cubes" fit width="full">
        {waiting ? (
          <p className={p.empty}>Loading cube…</p>
        ) : cubeId !== undefined && !cube ? (
          <div className={p.panel}>
            <p className={p.empty}>This cube isn’t in your library — it may have been deleted, or saved on another device.</p>
            <button type="button" className={p.button} onClick={() => navigate('/cubes')}>
              Back to your cubes
            </button>
          </div>
        ) : (
          // Keyed so saving a new cube (which moves it onto its own URL) keeps the editor's state.
          <CubeEditor
            key="cube-editor"
            variant="page"
            cube={cube}
            availableSets={basicLandSets}
            initialTab={search.get('from') === 'paste' ? 'PASTE' : undefined}
            onClose={() => navigate('/cubes')}
            onSaved={(saved) => {
              setSavedHere(saved)
              if (saved.id !== cubeId) navigate(`/cubes/edit/${encodeURIComponent(saved.id)}`, { replace: true })
            }}
          />
        )}
      </PageShell>
    )
  }

  return <CubeLibrary library={library} />
}

function CubeLibrary({ library }: { library: ReturnType<typeof useUnifiedCubes> }) {
  const navigate = useNavigate()
  const catalog = useCardCatalog()
  const { cubes, ready } = library
  const [sharing, setSharing] = useState<UnifiedCube | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function duplicate(cube: UnifiedCube) {
    setError(null)
    try {
      const name = uniqueCubeName(`${cube.name} copy`, cubes.map((c) => c.name))
      await library.saveCube({ ...cubeToShared(cube), name })
      library.reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not duplicate the cube')
    }
  }

  async function remove(cube: UnifiedCube) {
    setError(null)
    try {
      await library.removeCube(cube)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete the cube')
    }
  }

  return (
    <PageShell
      title="Cubes"
      width="wide"
      actions={
        cubes.length > 0 ? (
          <button type="button" className={p.buttonPrimary} onClick={() => navigate('/cubes/new')}>
            New cube
          </button>
        ) : undefined
      }
    >
      <header className={styles.intro}>
        <h1 className={p.h1}>Your cubes</h1>
        <p className={p.lede}>
          A cube is your own card pool to draft or open as sealed — a classic shuffled cube, or a whole set
          opened as real boosters, extended with whatever cards you like.
        </p>
      </header>

      {error && <p className={styles.error} role="alert">{error}</p>}

      {!ready && cubes.length === 0 ? (
        <p className={p.empty}>Loading your cubes…</p>
      ) : cubes.length === 0 ? (
        <EmptyLibrary />
      ) : (
        <ul className={styles.grid}>
          {cubes.map((cube) => (
            <CubeTile
              key={cube.id}
              cube={cube}
              catalog={catalog}
              onEdit={() => navigate(`/cubes/edit/${encodeURIComponent(cube.id)}`)}
              onShare={() => setSharing(cube)}
              onDuplicate={() => void duplicate(cube)}
              onDelete={() => void remove(cube)}
            />
          ))}
        </ul>
      )}

      {!library.isLoggedIn && cubes.length > 0 && (
        <p className={styles.footnote}>
          These cubes are saved in this browser. Sign in to keep them in your account and share them by link.
        </p>
      )}

      {sharing && (
        <ShareCubeDialog
          saved={sharing}
          dirty={false}
          isLoggedIn={library.isLoggedIn}
          text={cubeToText(sharing)}
          // A device-only cube moves into the account on its first share; `saveCube` routes it there.
          onSave={() => library.saveCube(cubeToShared(sharing))}
          share={library.share}
          unshare={library.unshare}
          onClose={() => setSharing(null)}
        />
      )}
    </PageShell>
  )
}

function EmptyLibrary() {
  const navigate = useNavigate()
  return (
    <section className={styles.starts} aria-label="Start a cube">
      <button type="button" className={styles.start} onClick={() => navigate('/cubes/new')}>
        <span className={styles.startTitle}>Start from a set</span>
        <span className={styles.startBody}>
          Take a whole set at its own rarities — Bloomburrow, say, plus the new cards from Bloomburrow
          Commander — and draft it as Play Boosters, mythics one pack in eight.
        </span>
        <span className={styles.startCta}>Choose a set</span>
      </button>
      <button type="button" className={styles.start} onClick={() => navigate('/cubes/new?from=paste')}>
        <span className={styles.startTitle}>Paste a list</span>
        <span className={styles.startBody}>
          Bring a cube you already have — from CubeCobra, Moxfield or a text file. Cards that aren’t implemented
          yet are flagged before you play.
        </span>
        <span className={styles.startCta}>Paste a list</span>
      </button>
    </section>
  )
}

const COLOR_BAR = [
  { key: 'W', color: '#efe6c8', label: 'White' },
  { key: 'U', color: '#4f8fd6', label: 'Blue' },
  { key: 'B', color: '#5d5566', label: 'Black' },
  { key: 'R', color: '#d65a3f', label: 'Red' },
  { key: 'G', color: '#4f9a5c', label: 'Green' },
  { key: 'M', color: '#d6b54f', label: 'Multicolour' },
  { key: 'C', color: '#9aa3b2', label: 'Colourless' },
] as const

/** The cube's colour balance as one bar, segment widths by share of cards. */
function ColorBar({ cube, catalog }: { cube: UnifiedCube; catalog: CardCatalog }) {
  const { colors } = useMemo(
    () => colorAndCurve(cube.cards, (name) => catalog.byName.get(name.toLowerCase())),
    [cube.cards, catalog.byName],
  )
  const total = COLOR_BAR.reduce((sum, c) => sum + (colors[c.key] ?? 0), 0)
  if (!catalog.ready || total === 0) return <span className={styles.colorBar} aria-hidden />
  const summary = COLOR_BAR.filter((c) => (colors[c.key] ?? 0) > 0)
    .map((c) => `${c.label} ${Math.round(((colors[c.key] ?? 0) / total) * 100)}%`)
    .join(', ')
  return (
    <span className={styles.colorBar} role="img" aria-label={`Colour balance: ${summary}`} title={summary}>
      {COLOR_BAR.map((c) => {
        const share = (colors[c.key] ?? 0) / total
        return share > 0 ? <span key={c.key} style={{ flexGrow: share, background: c.color }} /> : null
      })}
    </span>
  )
}

function CubeTile({
  cube,
  catalog,
  onEdit,
  onShare,
  onDuplicate,
  onDelete,
}: {
  cube: UnifiedCube
  catalog: CardCatalog
  onEdit: () => void
  onShare: () => void
  onDuplicate: () => void
  onDelete: () => void
}) {
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const total = cubeCardCount(cube.cards)
  const unresolved = catalog.ready
    ? cube.cards.filter((e) => !catalog.byName.has(e.name.toLowerCase())).length
    : 0
  const blockedReason = total === 0
    ? 'This cube is empty'
    : unresolved > 0
      ? `${unresolved} card${unresolved === 1 ? '' : 's'} in this cube aren’t implemented yet — edit it to drop them`
      : null

  return (
    <li className={styles.tile}>
      <button type="button" className={styles.tileMain} onClick={onEdit} aria-label={`Edit ${cube.name}`}>
        <span className={styles.tileName}>{cube.name}</span>
        {cube.description && <span className={styles.tileDescription}>{cube.description}</span>}
      </button>
      <ColorBar cube={cube} catalog={catalog} />
      <div className={styles.tileFacts}>
        <span>{total} cards</span>
        <span className={styles.badge} data-kind={isBoosterStyle(cube.boosterStyle) ? 'booster' : 'shuffled'}>
          {boosterStyleInfo(cube.boosterStyle).label}
        </span>
        <span className={styles.badge} data-kind={cube.online ? 'online' : 'local'}>
          {cube.online ? 'In your account' : 'This device'}
        </span>
        {cube.shareCode && <span className={styles.badge} data-kind="shared">Shared</span>}
        {unresolved > 0 && <span className={styles.badge} data-kind="warning">{unresolved} not implemented</span>}
      </div>
      {confirmingDelete ? (
        <div className={styles.tileConfirm} role="alert">
          <span>
            Delete “{cube.name}”?{cube.shareCode ? ' Its share link stops working.' : ''}
          </span>
          <button type="button" className={styles.dangerButton} onClick={onDelete}>
            Delete
          </button>
          <button type="button" className={styles.textButton} onClick={() => setConfirmingDelete(false)}>
            Cancel
          </button>
        </div>
      ) : (
        <div className={styles.tileActions}>
          <CubePlayButton cube={cubeToShared(cube)} blockedReason={blockedReason} />
          <button type="button" className={styles.textButton} onClick={onEdit}>Edit</button>
          <button type="button" className={styles.textButton} onClick={onShare} disabled={total === 0}>Share</button>
          <button type="button" className={styles.textButton} onClick={onDuplicate}>Duplicate</button>
          <button type="button" className={`${styles.textButton} ${styles.tileDelete}`} onClick={() => setConfirmingDelete(true)}>
            Delete
          </button>
        </div>
      )}
    </li>
  )
}

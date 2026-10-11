/**
 * `/cubes/shared/:code` — a cube someone shared by link.
 *
 * Read-only, and always the owner's latest saved version. What a friend wants from it is to look,
 * then either keep a copy (into their own library — localStorage for a guest, so no account is
 * needed) or play it straight away. "Copy list" covers everything else.
 */
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { type SharedCubeView, getSharedCube } from '@/api/account'
import { CubeCardList } from '@/components/cube/CubeCardList'
import { CubePlayButton } from '@/components/cube/CubePlayButton'
import { ColorSpread, PackStrip, RaritySpread, colorAndCurve } from '@/components/cube/CubeStats'
import { boosterStyleInfo, cubeToText, isBoosterStyle, rarityTallies, uniqueCubeName } from '@/components/cube/cubeModel'
import { useCardCatalog } from '@/components/cube/useCardCatalog'
import { useCardHover } from '@/components/cube/useCardHover'
import { PageShell, pageStyles as p } from '@/components/ui/PageShell'
import { cubeCardCount } from '@/store/cubeLibrary'
import { type UnifiedCube, cubeToShared, useUnifiedCubes } from '@/store/useUnifiedCubes'
import styles from '@/components/cube/CubesPage.module.css'

type Load =
  | { state: 'loading' }
  | { state: 'missing' }
  | { state: 'error'; message: string }
  | { state: 'ready'; view: SharedCubeView }

export function SharedCubePage() {
  const { code = '' } = useParams<{ code: string }>()
  const [load, setLoad] = useState<Load>({ state: 'loading' })

  useEffect(() => {
    let cancelled = false
    setLoad({ state: 'loading' })
    getSharedCube(code)
      .then((view) => {
        if (!cancelled) setLoad(view ? { state: 'ready', view } : { state: 'missing' })
      })
      .catch((err: unknown) => {
        if (!cancelled) setLoad({ state: 'error', message: err instanceof Error ? err.message : 'Could not load the cube' })
      })
    return () => {
      cancelled = true
    }
  }, [code])

  return (
    <PageShell title="Shared cube" width="wide">
      {load.state === 'loading' && <p className={p.empty}>Loading cube…</p>}
      {(load.state === 'missing' || load.state === 'error') && <NotFound message={load.state === 'error' ? load.message : null} />}
      {load.state === 'ready' && <SharedCube view={load.view} />}
    </PageShell>
  )
}

function NotFound({ message }: { message: string | null }) {
  const navigate = useNavigate()
  return (
    <div className={p.panel}>
      <h1 className={p.h1}>This cube link doesn’t open</h1>
      <p className={p.lede}>
        {message ?? 'Its owner may have stopped sharing it or deleted the cube. Ask them for a new link.'}
      </p>
      <p>
        <button type="button" className={p.button} onClick={() => navigate('/cubes')}>
          Go to your cubes
        </button>
      </p>
    </div>
  )
}

function SharedCube({ view }: { view: SharedCubeView }) {
  const navigate = useNavigate()
  const catalog = useCardCatalog()
  const library = useUnifiedCubes()
  const { onHover, preview } = useCardHover()
  const [copied, setCopied] = useState<UnifiedCube | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const cube = view.cube
  const lookup = useMemo(
    () => (name: string) => catalog.byName.get(name.toLowerCase()),
    [catalog.byName],
  )
  const { colors } = useMemo(() => colorAndCurve(cube.cards, lookup), [cube.cards, lookup])
  const tally = useMemo(() => rarityTallies(cube.cards, (n) => lookup(n)?.rarity).physical, [cube.cards, lookup])
  const total = cubeCardCount(cube.cards)
  const unresolved = catalog.ready ? cube.cards.filter((e) => !lookup(e.name)).length : 0
  const style = cube.boosterStyle ?? 'SHUFFLED'

  async function saveCopy() {
    setBusy(true)
    setNotice(null)
    try {
      const name = uniqueCubeName(cube.name, library.cubes.map((c) => c.name))
      const saved = await library.saveCube({ ...cubeToShared(cube), name })
      setCopied(saved)
    } catch (err) {
      setNotice(err instanceof Error ? err.message : 'Could not save a copy')
    } finally {
      setBusy(false)
    }
  }

  async function copyList() {
    try {
      await navigator.clipboard.writeText(cubeToText(cube))
      setNotice('Cube list copied')
    } catch {
      setNotice('Your browser blocked the clipboard.')
    }
  }

  return (
    <>
      <section className={p.panel}>
        <div className={styles.sharedHero}>
          <div className={styles.sharedTitle}>
            <h1 className={p.h1}>{cube.name}</h1>
            <span className={styles.sharedByline}>
              {view.ownerName ? `Shared by ${view.ownerName}` : 'A shared cube'} · {total} cards ·{' '}
              {boosterStyleInfo(style).label}
            </span>
            {cube.description && <p className={styles.sharedDescription}>{cube.description}</p>}
          </div>
          <div className={styles.sharedActions}>
            <CubePlayButton
              cube={cube}
              blockedReason={unresolved > 0 ? `${unresolved} cards in this cube aren’t implemented yet` : null}
            />
            {copied ? (
              <button
                type="button"
                className={p.button}
                onClick={() => navigate(`/cubes/edit/${encodeURIComponent(copied.id)}`)}
              >
                Open your copy
              </button>
            ) : (
              <button type="button" className={p.button} disabled={busy || !library.ready} onClick={() => void saveCopy()}>
                {busy ? 'Saving…' : 'Save a copy'}
              </button>
            )}
            <button type="button" className={p.buttonGhost} onClick={() => void copyList()}>
              Copy list
            </button>
          </div>
        </div>
        {copied && (
          <p className={styles.notice} role="status">
            Saved as “{copied.name}” {copied.online ? 'in your account' : 'on this device'}. Your copy won’t change when
            the original does.
          </p>
        )}
        {notice && <p className={styles.notice} role="status">{notice}</p>}
        <div className={styles.sharedStats}>
          <span className={styles.statGroup}>
            <span className={styles.statLabel}>Colours</span>
            <ColorSpread colors={colors} />
          </span>
          <span className={styles.statGroup}>
            <span className={styles.statLabel}>Rarity</span>
            <RaritySpread tally={tally} />
          </span>
          {isBoosterStyle(style) && (
            <span className={styles.statGroup}>
              <span className={styles.statLabel}>Each pack</span>
              <PackStrip style={style} />
            </span>
          )}
        </div>
      </section>

      <section className={`${p.panel} ${styles.listPanel}`} aria-label="Cards in the cube">
        <CubeCardList
          entries={cube.cards}
          lookup={lookup}
          catalogReady={catalog.ready}
          rarityMatters={isBoosterStyle(style)}
          onHover={onHover}
        />
      </section>
      {preview}
    </>
  )
}

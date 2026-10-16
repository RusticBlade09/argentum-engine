import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, FlipOntoBattlefieldDecision } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { FlipTableStage, type CardMark, type StagePointerKind } from './FlipTableStage'
import { FlipOrb } from './FlipOrb'
import { heightLabel, makeFlipView, previewTouches, releaseQuality, scatterSigma, type Point } from './flipGeometry'
import decisionStyles from '../decisions/DecisionUI.module.css'
import styles from './Flip.module.css'

/** One full swing of the release meter, up and back down. */
const SWING_MS = 2000
/** How long the hand stays steady before it starts to shake. */
const STEADY_MS = 1100
const GAUGE_HEIGHT = 220

/** The meter's strength at [elapsed] ms into the swing: a smooth 0 → 1 → 0 ping-pong. */
function meterAt(elapsed: number): number {
  return (1 - Math.cos((2 * Math.PI * elapsed) / SWING_MS)) / 2
}

/** The hand's drift (mm) at [elapsed] ms: steady at first, then a growing tremble. */
function tremorAt(elapsed: number): Point {
  const t = elapsed / 1000
  const amp = 1.2 + Math.max(0, elapsed - STEADY_MS) * 0.012
  return { x: Math.min(26, amp) * Math.sin(5.1 * t + 0.3), y: Math.min(26, amp) * Math.cos(3.7 * t) }
}

/**
 * The Chaos Orb mini-game, thrower's side: aim over the engine's table, press and hold to swing,
 * let go with the meter in the gold. The table is the server's; the throw is the player's; where
 * it lands is rolled by the server when it receives the throw.
 */
export function FlipDecisionOverlay({ decision }: { decision: FlipOntoBattlefieldDecision }) {
  const submitFlipThrow = useGameStore((s) => s.submitFlipThrow)
  const gameState = useGameStore((s) => s.gameState)
  const viewerId = useGameStore((s) => s.playerId)
  const { table, release } = decision
  const view = useMemo(() => makeFlipView(table, false), [table])
  const sourceName = decision.context.sourceName ?? 'Chaos Orb'
  const orbImage = getCardImageUrl(sourceName, decision.flippedImageUri)

  const [aim, setAim] = useState<Point | null>(null)
  const [swinging, setSwinging] = useState(false)
  const [hidden, setHidden] = useState(false)
  const swingStart = useRef(0)
  const aimRef = useRef<Point | null>(null)
  const submitted = useRef(false)

  const ghostRef = useRef<HTMLDivElement>(null)
  const ringRef = useRef<HTMLDivElement>(null)
  const crossRef = useRef<HTMLDivElement>(null)
  const needleRef = useRef<HTMLDivElement>(null)
  const readoutRef = useRef<HTMLDivElement>(null)
  const orbRef = useRef<HTMLDivElement>(null)

  const playerName = useCallback(
    (id: EntityId) => gameState?.players.find((p) => p.playerId === id)?.name ?? 'Opponent',
    [gameState],
  )

  // Place the aim ghost, the scatter ring and the crosshair at [p] (table mm) for [strength].
  const placeAim = useCallback(
    (p: Point, strength: number, locked: boolean) => {
      const s = view.toStage(p)
      const sigma = scatterSigma(table, release, p, strength)
      const r = 2 * sigma + 31.5
      const quality = releaseQuality(release, strength)
      for (const el of [ghostRef.current, crossRef.current]) {
        if (el) { el.style.left = `${s.x}px`; el.style.top = `${s.y}px` }
      }
      ghostRef.current?.classList.toggle(styles.ghostLocked!, locked)
      const ring = ringRef.current
      if (ring) {
        ring.style.left = `${s.x - r}px`
        ring.style.top = `${s.y - r}px`
        ring.style.width = `${2 * r}px`
        ring.style.height = `${2 * r}px`
        ring.classList.toggle(styles.ringClean!, locked && quality >= 0.999)
        ring.classList.toggle(styles.ringSloppy!, locked && quality < 0.6)
      }
    },
    [view, table, release],
  )

  // While swinging: run the meter, the trembling hand and the breathing scatter ring per frame.
  useEffect(() => {
    if (!swinging) return
    let frame = 0
    const tick = () => {
      const elapsed = performance.now() - swingStart.current
      const v = meterAt(elapsed)
      const base = aimRef.current
      if (base) {
        const d = tremorAt(elapsed)
        placeAim({ x: base.x + d.x, y: base.y + d.y }, v, true)
      }
      if (needleRef.current) needleRef.current.style.top = `${(1 - v) * GAUGE_HEIGHT}px`
      if (readoutRef.current) readoutRef.current.textContent = heightLabel(v)
      if (orbRef.current) {
        orbRef.current.style.transform = `translateY(${-v * 26}px) rotateX(${v * 38}deg) rotateZ(-6deg) scale(${1 + v * 0.08})`
      }
      frame = requestAnimationFrame(tick)
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [swinging, placeAim])

  useEffect(() => {
    if (orbRef.current) orbRef.current.style.transform = 'rotateZ(-6deg)'
  }, [])

  // Idle aim: show the ring a clean release would give.
  useEffect(() => {
    if (!swinging && aim) placeAim(aim, (release.cleanFrom + release.cleanTo) / 2, false)
  }, [aim, swinging, placeAim, release])

  const startSwing = useCallback((p: Point) => {
    if (submitted.current) return
    aimRef.current = p
    setAim(p)
    swingStart.current = performance.now()
    setSwinging(true)
  }, [])

  const cancelSwing = useCallback(() => {
    setSwinging(false)
    if (orbRef.current) orbRef.current.style.transform = 'rotateZ(-6deg)'
  }, [])

  const releaseSwing = useCallback(() => {
    if (submitted.current || !aimRef.current) return
    const elapsed = performance.now() - swingStart.current
    const strength = meterAt(elapsed)
    const d = tremorAt(elapsed)
    submitted.current = true
    setSwinging(false)
    submitFlipThrow(decision.id, aimRef.current.x + d.x, aimRef.current.y + d.y, strength)
  }, [decision.id, submitFlipThrow])

  const onPointer = (kind: StagePointerKind, p: Point, e: ReactPointerEvent<HTMLDivElement>) => {
    if (submitted.current) return
    switch (kind) {
      case 'move':
        if (swinging) aimRef.current = p
        else setAim(p)
        break
      case 'down':
        if (e.button === 2) { cancelSwing(); break }
        if (e.button !== 0) break
        e.currentTarget.setPointerCapture(e.pointerId)
        startSwing(p)
        break
      case 'up':
        if (swinging) releaseSwing()
        break
      case 'cancel':
        cancelSwing()
        break
      case 'leave':
        if (!swinging) setAim(null)
        break
    }
  }

  // Keyboard: Space/Enter swings at the current aim (the table's middle by default), Esc stops.
  useEffect(() => {
    const down = (e: KeyboardEvent) => {
      if (e.repeat) return
      if ((e.key === ' ' || e.key === 'Enter') && !swinging && !hidden) {
        e.preventDefault()
        startSwing(aimRef.current ?? aim ?? { x: table.width / 2, y: table.bands[0]!.top + table.bands[0]!.height / 2 })
      } else if (e.key === 'Escape' && swinging) {
        e.preventDefault()
        cancelSwing()
      }
    }
    const up = (e: KeyboardEvent) => {
      if ((e.key === ' ' || e.key === 'Enter') && swinging) {
        e.preventDefault()
        releaseSwing()
      }
    }
    window.addEventListener('keydown', down)
    window.addEventListener('keyup', up)
    return () => {
      window.removeEventListener('keydown', down)
      window.removeEventListener('keyup', up)
    }
  }, [swinging, hidden, aim, table, startSwing, cancelSwing, releaseSwing])

  // What the ghost currently covers, for the highlight and the label under it.
  const covered = useMemo(
    () => (aim ? table.cards.filter((c) => previewTouches(aim, c)) : []),
    [aim, table],
  )
  const marks = useMemo(() => {
    const m = new Map<EntityId, CardMark>()
    for (const c of covered) m.set(c.entityId, !c.eligible ? 'immune' : c.controllerId === viewerId ? 'own' : 'target')
    return m
  }, [covered, viewerId])
  const chips = useMemo(() => {
    const m = new Map<EntityId, { text: string; tone: 'danger' | 'warn' | 'calm' }>()
    for (const c of covered) {
      if (!c.eligible) m.set(c.entityId, { text: c.isToken ? 'Token · safe' : 'Safe', tone: 'calm' })
      else if (c.controllerId === viewerId) m.set(c.entityId, { text: 'Yours', tone: 'warn' })
    }
    return m
  }, [covered, viewerId])

  const aimLabel = (() => {
    if (!aim || swinging) return null
    const hits = covered.filter((c) => c.eligible)
    if (hits.length === 0) return covered.length > 0 ? 'Only tokens here' : 'Bare felt'
    return hits.map((c) => (c.name ?? 'Face-down permanent') + (c.controllerId === viewerId ? ' (yours)' : '')).join(' · ')
  })()

  if (hidden) {
    return (
      <button className={decisionStyles.floatingReturnButton} onClick={() => setHidden(false)}>
        Return to the {sourceName} flip
      </button>
    )
  }

  const hand = view.toStage({ x: table.handX, y: table.handY })
  // The gauge stands just off the table's right-hand edge, level with the hand.
  const gaugeX = view.toStage({ x: table.width, y: 0 }).x + 26
  const soft = release.tooSoftBelow
  const stageAim = aim ? view.toStage(aim) : null

  return (
    <div className={styles.overlay} role="dialog" aria-label={`Flip ${sourceName}`}>
      <div className={styles.header}>
        <div className={styles.headerArt} style={{ backgroundImage: `url("${orbImage}")` }} />
        <div className={styles.headerText}>
          <div className={styles.title}>Flip {sourceName}</div>
          <div className={styles.subtitle}>
            If it turns over completely, it destroys every nontoken permanent it touches — yours too. Then it's destroyed.
          </div>
        </div>
        <div className={styles.headerSpacer} />
        <button className={styles.ghostButton} onClick={() => setHidden(true)} disabled={swinging}>
          View battlefield
        </button>
      </div>

      <FlipTableStage
        table={table}
        view={view}
        viewerId={viewerId}
        playerName={playerName}
        marks={marks}
        chips={chips}
        className={styles.aiming}
        onPointer={onPointer}
      >
        <div ref={ringRef} className={styles.ring} style={{ display: aim ? undefined : 'none' }} />
        <div ref={ghostRef} className={styles.ghost} style={{ display: aim ? undefined : 'none' }} />
        <div ref={crossRef} className={styles.crosshair} style={{ display: aim ? undefined : 'none' }} />
        {stageAim && aimLabel && (
          <div className={styles.aimLabel} style={{ left: stageAim.x, top: stageAim.y }}>
            {aimLabel}
          </div>
        )}

        <div className={styles.orbAnchor} style={{ left: hand.x, top: hand.y, perspective: 700 }}>
          <FlipOrb ref={orbRef} imageUrl={orbImage} className={styles.orbInHand} faceClassName={styles.orbGlow} />
        </div>

        <div className={styles.gauge} style={{ left: gaugeX, top: hand.y + 44 - GAUGE_HEIGHT, height: GAUGE_HEIGHT }}>
          <div className={styles.gaugeTrack}>
            <div className={`${styles.gaugeZone} ${styles.zoneSoft}`} style={{ top: (1 - soft) * GAUGE_HEIGHT, bottom: 5 }} />
            <div
              className={`${styles.gaugeZone} ${styles.zoneClean}`}
              style={{ top: (1 - release.cleanTo) * GAUGE_HEIGHT, height: (release.cleanTo - release.cleanFrom) * GAUGE_HEIGHT }}
            />
          </div>
          <div ref={needleRef} className={styles.gaugeNeedle} style={{ top: GAUGE_HEIGHT, opacity: swinging ? 1 : 0.35 }} />
          <div className={styles.gaugeTick} style={{ top: GAUGE_HEIGHT - 4 }}>1 ft</div>
          <div className={styles.gaugeTick} style={{ top: (1 - (release.cleanFrom + release.cleanTo) / 2) * GAUGE_HEIGHT, color: '#ffe9a8' }}>
            clean
          </div>
          <div className={styles.gaugeTick} style={{ top: 6 }}>3 ft</div>
          <div ref={readoutRef} className={styles.gaugeReadout}>{swinging ? '' : 'drop height'}</div>
        </div>
      </FlipTableStage>

      <div className={styles.footer}>
        <div className={styles.steps} aria-live="polite">
          <span className={`${styles.step} ${!swinging && !aim ? styles.stepActive : ''}`}>
            <span className={styles.stepNum}>1</span>Aim over the table
          </span>
          <span className={`${styles.step} ${!swinging && aim ? styles.stepActive : ''}`}>
            <span className={styles.stepNum}>2</span>Press and hold to swing
          </span>
          <span className={`${styles.step} ${swinging ? styles.stepActive : ''}`}>
            <span className={styles.stepNum}>3</span>Let go in the gold
          </span>
        </div>
        <span className={styles.subtitle}>
          {swinging ? 'Drag to adjust · hold too long and your hand starts to shake · Esc to stop' : 'Space works too · the ring shows where it can land'}
        </span>
      </div>
    </div>
  )
}

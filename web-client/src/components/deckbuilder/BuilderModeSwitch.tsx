/**
 * Decks | Cubes — the builder's two modes, in the page title where "Deckbuilder" used to sit.
 *
 * Decks and cubes are built with the same card browser, so they are one tool with two modes rather
 * than two tools; this switch is what says so. Each mode remembers where it was for the rest of the
 * session, so switching away and back returns to the same deck or cube instead of a blank one.
 */
import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import styles from './BuilderModeSwitch.module.css'

export type BuilderMode = 'deck' | 'cube'

const LAST_PATH_KEY: Record<BuilderMode, string> = {
  deck: 'argentum.builder.lastDeckPath',
  cube: 'argentum.builder.lastCubePath',
}

const DEFAULT_PATH: Record<BuilderMode, string> = {
  deck: '/deckbuilder',
  cube: '/cubes/new',
}

function readLastPath(mode: BuilderMode): string {
  try {
    return window.sessionStorage.getItem(LAST_PATH_KEY[mode]) ?? DEFAULT_PATH[mode]
  } catch {
    return DEFAULT_PATH[mode]
  }
}

export function BuilderModeSwitch({
  mode,
  guard,
}: {
  mode: BuilderMode
  /**
   * Asked before leaving for the other mode; call `proceed` to go. The cube builder uses it to offer
   * saving unsaved changes first. Without one, the switch navigates straight away.
   */
  guard?: (proceed: () => void) => void
}) {
  const navigate = useNavigate()
  const location = useLocation()

  // Remember this mode's current place, so coming back lands on it.
  useEffect(() => {
    try {
      window.sessionStorage.setItem(LAST_PATH_KEY[mode], location.pathname + location.search)
    } catch {
      // Storage blocked: switching back simply opens the mode fresh.
    }
  }, [mode, location.pathname, location.search])

  function go(target: BuilderMode) {
    if (target === mode) return
    const proceed = () => navigate(readLastPath(target))
    if (guard) guard(proceed)
    else proceed()
  }

  return (
    <div className={styles.switch} role="tablist" aria-label="Build">
      {(['deck', 'cube'] as const).map((m) => (
        <button
          key={m}
          type="button"
          role="tab"
          aria-selected={mode === m}
          className={styles.option}
          onClick={() => go(m)}
        >
          {m === 'deck' ? 'Decks' : 'Cubes'}
        </button>
      ))}
    </div>
  )
}

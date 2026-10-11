import { useCallback, useEffect, useRef, useState } from 'react'
import { useGameStore, type DeckBuildingState } from '@/store/gameStore.ts'
import { sameDeck } from '@/store/slices/assistDeck'
import { SparkleIcon } from './AssistControls'
import styles from './chat.module.css'

/**
 * The deck advisor: a chat with the deckbuilding LLM on the server, docked over the deck rail and
 * opened from the header's AI group. It stays mounted while closed so the conversation survives
 * closing it; it is lost on reload, by design.
 *
 * The LLM may answer with actions that:
 *  - highlight cards in the pool
 *  - clear highlights
 *  - replace the current deck (main + basic lands) — undoable, like Auto-build
 */

type ChatRole = 'user' | 'assistant' | 'system'

type ActionNoteKind = 'highlight' | 'clear' | 'deck'

interface ActionNote {
  readonly kind: ActionNoteKind
  readonly text: string
}

interface ChatMessage {
  readonly role: ChatRole
  readonly content: string
  /** What this assistant turn did to the builder. */
  readonly actionNotes?: readonly ActionNote[]
  /** A failed turn: shown as an error with Retry, never sent back to the LLM (role 'system'). */
  readonly retryText?: string
}

type ServerAction =
  | { readonly type: 'highlight_cards'; readonly cardNames: readonly string[] }
  | { readonly type: 'clear_highlights' }
  | {
      readonly type: 'set_deck'
      readonly mainDeck: ReadonlyArray<{ readonly name: string; readonly count: number }>
      readonly lands: ReadonlyArray<{ readonly name: string; readonly count: number }>
    }

interface ServerResponse {
  readonly assistantMessage: string
  readonly actions: readonly ServerAction[]
  readonly error?: string | null
}

export function DeckbuilderChatPanel({
  state,
  open,
  onClose,
  mobile,
}: {
  state: DeckBuildingState
  open: boolean
  onClose: () => void
  mobile: boolean
}) {
  const applyAssistDeck = useGameStore((s) => s.applyAssistDeck)
  const undoAssistDeck = useGameStore((s) => s.undoAssistDeck)
  const assistUndo = useGameStore((s) => s.assistUndo)
  const setLlmHighlights = useGameStore((s) => s.setLlmHighlights)

  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [welcome, setWelcome] = useState(pickWelcome)
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [messages, sending])

  useEffect(() => {
    if (open) inputRef.current?.focus()
  }, [open])

  // Grow the input with its text, up to a few lines.
  useEffect(() => {
    const el = inputRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`
  }, [input])

  const applyActions = useCallback((actions: readonly ServerAction[]): ActionNote[] => {
    const notes: ActionNote[] = []
    let highlightTouched = false
    for (const action of actions) {
      switch (action.type) {
        case 'highlight_cards': {
          setLlmHighlights(action.cardNames)
          highlightTouched = true
          notes.push(
            action.cardNames.length === 0
              ? { kind: 'clear', text: 'Cleared highlights' }
              : { kind: 'highlight', text: `Highlighted ${plural(action.cardNames.length, 'card')} in your pool` },
          )
          break
        }
        case 'clear_highlights': {
          setLlmHighlights(null)
          highlightTouched = true
          notes.push({ kind: 'clear', text: 'Cleared highlights' })
          break
        }
        case 'set_deck': {
          // Flatten main deck into the (deck = string[]) shape the store uses.
          const flat: string[] = []
          let mainTotal = 0
          for (const entry of action.mainDeck) {
            for (let i = 0; i < entry.count; i++) flat.push(entry.name)
            mainTotal += entry.count
          }
          const landCounts: Record<string, number> = {}
          let landTotal = 0
          for (const entry of action.lands) {
            landCounts[entry.name] = (landCounts[entry.name] ?? 0) + entry.count
            landTotal += entry.count
          }
          applyAssistDeck(flat, landCounts, 'advisor')
          notes.push({ kind: 'deck', text: `Replaced your deck: ${mainTotal} spells + ${plural(landTotal, 'basic land')}` })
          break
        }
      }
    }
    if (!highlightTouched && actions.some((a) => a.type === 'set_deck')) {
      // Building a new deck implicitly invalidates an old highlight set.
      setLlmHighlights(null)
    }
    return notes
  }, [applyAssistDeck, setLlmHighlights])

  const send = useCallback(async (raw: string, history: readonly ChatMessage[] = messages) => {
    const text = raw.trim()
    if (!text || sending) return

    const nextMessages: ChatMessage[] = [...history, { role: 'user', content: text }]
    setMessages(nextMessages)
    setInput('')
    setSending(true)

    const fail = (content: string) =>
      setMessages((prev) => [...prev, { role: 'system', content, retryText: text }])

    try {
      const response = await fetch('/api/deckbuilding/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        // A question whose turn failed never got an answer; leave it out of what the model sees.
        body: JSON.stringify(buildRequest(
          state,
          nextMessages.filter((m, i) => !(m.role === 'user' && nextMessages[i + 1]?.retryText != null)),
        )),
      })
      if (!response.ok) {
        fail(await describeFailure(response))
        return
      }
      const data = (await response.json()) as ServerResponse
      const notes = applyActions(data.actions ?? [])
      setMessages((prev) => [
        ...prev,
        notes.length > 0
          ? { role: 'assistant', content: data.assistantMessage, actionNotes: notes }
          : { role: 'assistant', content: data.assistantMessage },
      ])
    } catch {
      fail('Couldn’t reach the advisor — check your connection and try again.')
    } finally {
      setSending(false)
    }
  }, [applyActions, messages, sending, state])

  // A retried turn replaces the failed question and its error rather than stacking another copy.
  const retry = (failedIndex: number, text: string) => {
    const question = lastIndexWhere(messages.slice(0, failedIndex), (m) => m.role === 'user')
    void send(text, messages.slice(0, question >= 0 ? question : failedIndex))
  }

  const newChat = () => {
    setMessages([])
    setWelcome(pickWelcome())
    setLlmHighlights(null)
    inputRef.current?.focus()
  }

  const onKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      void send(input)
    }
  }

  // Only the newest deck rewrite can be undone, and only while the deck is still what it left.
  const lastDeckChange = lastIndexWhere(messages, (m) => m.actionNotes?.some((n) => n.kind === 'deck') ?? false)
  const canUndoDeck =
    assistUndo?.source === 'advisor' &&
    sameDeck({ deck: state.deck, landCounts: state.landCounts }, assistUndo.applied)
  const hasHighlights = (state.llmHighlightedCards?.length ?? 0) > 0

  return (
    <section
      id="deck-advisor-panel"
      className={styles.panel}
      data-mobile={mobile}
      hidden={!open}
      aria-label="Deck advisor"
      onKeyDown={(e) => {
        if (e.key === 'Escape') onClose()
      }}
    >
      <header className={styles.head}>
        <span className={styles.avatar} aria-hidden><SparkleIcon size={14} /></span>
        <div className={styles.headText}>
          <h3 className={styles.title}>Deck advisor</h3>
          <span className={styles.subtitle}>Sees your pool and your deck</span>
        </div>
        {messages.length > 0 && (
          <button type="button" className={styles.headButton} onClick={newChat} disabled={sending} title="Start over">
            New chat
          </button>
        )}
        <button type="button" className={styles.iconButton} onClick={onClose} aria-label="Close the deck advisor" title="Close (Esc)">
          ×
        </button>
      </header>

      <div ref={scrollRef} className={styles.log} aria-live="polite">
        <div className={styles.welcome}>
          <p className={styles.welcomeIntro}>{welcome.intro}</p>
          <p className={styles.welcomeHow}>
            Ask about your pool, or have me highlight cards or build the deck — lands included.
            Anything I change to your deck can be undone.
          </p>
          {messages.length === 0 && (
            <div className={styles.suggestions}>
              {welcome.suggestions.map((s) => (
                <button key={s} type="button" className={styles.suggestion} onClick={() => void send(s)} disabled={sending}>
                  {s}
                </button>
              ))}
            </div>
          )}
        </div>

        {messages.map((m, i) => (
          <MessageBubble
            key={i}
            message={m}
            onRetry={m.retryText != null && !sending ? () => retry(i, m.retryText!) : undefined}
            onUndoDeck={i === lastDeckChange && canUndoDeck ? undoAssistDeck : undefined}
            onClearHighlights={hasHighlights ? () => setLlmHighlights(null) : undefined}
          />
        ))}
        {sending && <ThinkingBubble />}
      </div>

      <form
        className={styles.composer}
        onSubmit={(e) => {
          e.preventDefault()
          void send(input)
        }}
      >
        <textarea
          ref={inputRef}
          className={styles.input}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={onKeyDown}
          rows={1}
          placeholder="Ask the advisor…"
          aria-label="Message the deck advisor"
          disabled={sending}
        />
        <button
          type="submit"
          className={styles.send}
          disabled={sending || input.trim().length === 0}
          aria-label="Send"
          title="Send (Enter)"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8"
            strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M8 13V3M3.5 7.5L8 3l4.5 4.5" />
          </svg>
        </button>
      </form>
      {!mobile && <div className={styles.hint}>Enter to send · Shift+Enter for a new line · Esc to close</div>}
    </section>
  )
}

function MessageBubble({
  message,
  onRetry,
  onUndoDeck,
  onClearHighlights,
}: {
  message: ChatMessage
  onRetry: (() => void) | undefined
  onUndoDeck: (() => void) | undefined
  onClearHighlights: (() => void) | undefined
}) {
  if (message.retryText != null) {
    return (
      <div className={styles.error} role="alert">
        <span>{message.content}</span>
        {onRetry && <button type="button" className={styles.noteAction} onClick={onRetry}>Retry</button>}
      </div>
    )
  }
  const isUser = message.role === 'user'
  return (
    <div className={styles.row} data-role={isUser ? 'user' : 'assistant'}>
      <div className={styles.bubble} data-role={isUser ? 'user' : 'assistant'}>
        {renderInline(message.content)}
      </div>
      {message.actionNotes?.map((note, i) => (
        <div key={i} className={styles.note} data-kind={note.kind}>
          <span aria-hidden>{note.kind === 'deck' ? '⟳' : note.kind === 'highlight' ? '◎' : '○'}</span>
          <span>{note.text}</span>
          {note.kind === 'deck' && onUndoDeck && (
            <button type="button" className={styles.noteAction} onClick={onUndoDeck}>Undo</button>
          )}
          {note.kind === 'highlight' && onClearHighlights && (
            <button type="button" className={styles.noteAction} onClick={onClearHighlights}>Clear</button>
          )}
        </div>
      ))}
    </div>
  )
}

function ThinkingBubble() {
  // A whole deck can take the model the best part of a minute; say so rather than look stuck.
  const [slow, setSlow] = useState(false)
  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), 6000)
    return () => clearTimeout(timer)
  }, [])
  return (
    <div className={styles.row} data-role="assistant">
      <div className={`${styles.bubble} ${styles.thinking}`} data-role="assistant" aria-label="The advisor is thinking">
        <span /><span /><span />
      </div>
      {slow && <span className={styles.slow}>Still thinking — building a deck can take up to a minute.</span>}
    </div>
  )
}

/** Replies are plain text; honour the odd **bold** the model slips in rather than printing asterisks. */
function renderInline(text: string): React.ReactNode {
  const parts = text.split(/(\*\*[^*]+\*\*)/g)
  return parts.map((part, i) =>
    part.startsWith('**') && part.endsWith('**') && part.length > 4
      ? <strong key={i}>{part.slice(2, -2)}</strong>
      : part,
  )
}

/** The server answers a 503 with a JSON body explaining itself; show its sentence, not the JSON. */
async function describeFailure(response: Response): Promise<string> {
  const body = await response.text().catch(() => '')
  try {
    const parsed = JSON.parse(body) as Partial<ServerResponse>
    if (parsed.error === 'llm_not_configured') return 'The deck advisor isn’t set up on this server.'
    if (parsed.assistantMessage) return parsed.assistantMessage
  } catch {
    // Not JSON — fall through.
  }
  return response.status >= 500
    ? 'The advisor ran into a problem. Try again in a moment.'
    : `The advisor couldn’t answer that (error ${response.status}).`
}

function lastIndexWhere<T>(items: readonly T[], test: (item: T) => boolean): number {
  for (let i = items.length - 1; i >= 0; i--) if (test(items[i]!)) return i
  return -1
}

function plural(n: number, word: string): string {
  return `${n} ${word}${n === 1 ? '' : 's'}`
}

function buildRequest(state: DeckBuildingState, messages: readonly ChatMessage[]): unknown {
  // Aggregate the pool so each card appears once with its total + currently-available count.
  const counts: Record<string, number> = {}
  for (const card of state.cardPool) {
    counts[card.name] = (counts[card.name] ?? 0) + 1
  }
  const inDeck: Record<string, number> = {}
  for (const name of state.deck) {
    inDeck[name] = (inDeck[name] ?? 0) + 1
  }

  const seen = new Set<string>()
  const pool = state.cardPool
    .filter((card) => {
      if (seen.has(card.name)) return false
      seen.add(card.name)
      return true
    })
    .map((card) => ({
      name: card.name,
      manaCost: card.manaCost,
      typeLine: card.typeLine,
      rarity: card.rarity,
      oracleText: card.oracleText ?? null,
      power: card.power ?? null,
      toughness: card.toughness ?? null,
      isDoubleFaced: card.isDoubleFaced ?? false,
      backFaceName: card.backFaceName ?? null,
      backFaceTypeLine: card.backFaceTypeLine ?? null,
      backFaceOracleText: card.backFaceOracleText ?? null,
      totalCount: counts[card.name] ?? 1,
      availableCount: Math.max(0, (counts[card.name] ?? 1) - (inDeck[card.name] ?? 0)),
    }))

  return {
    messages: messages
      .filter((m) => m.role === 'user' || m.role === 'assistant')
      .map((m) => ({ role: m.role, content: m.content })),
    pool,
    basicLands: state.basicLands.map((c) => ({ name: c.name })),
    deck: state.deck,
    landCounts: state.landCounts,
    setCodes: state.setCodes,
  }
}

const BUILD_EXAMPLES: readonly string[] = [
  'Build the best two-color deck you can',
  'Build an aggressive deck from this pool',
  'Build me a control deck with lots of removal',
  'Add lands to my deck in the right colors',
]

const HIGHLIGHT_EXAMPLES: readonly string[] = [
  'Highlight all the removal',
  'Show me the bombs in my pool',
  'Highlight every creature with flying',
  'Highlight my two-drops',
  'Highlight the mana fixing',
]

const QUESTION_EXAMPLES: readonly string[] = [
  'Which color pair is strongest here?',
  'What archetypes does my pool support?',
  'Is my mana curve okay?',
  'How does my current deck look?',
  'Should I splash a third color?',
]

function pickRandom<T>(arr: readonly T[]): T {
  return arr[Math.floor(Math.random() * arr.length)] ?? arr[0]!
}

const WELCOME_INTROS: readonly string[] = [
  "I've already looked at your pool. Some of it is great, some of it is a 2/1 for three — we'll work with what we have.",
  "Welcome to the deckbuilding table. No, you can't sneak that fifth color past me.",
  'A fresh pool — the mana hums with possibility, the rares hum a bit louder.',
  "Forty cards stand between you and a 0-3 record. Let's make sure they're the right forty.",
  'Before you build the army, we should probably count the lands.',
  "A new pool, a new puzzle. I make no promises about the rares, but I'll help you make the best of them.",
  'Two colors, maybe three. A handful of bombs, a pile of removal, and hopefully not a curve of all sevens.',
]

/** One opener per conversation, plus a build, a highlight and a question to start from. */
function pickWelcome(): { intro: string; suggestions: readonly string[] } {
  return {
    intro: pickRandom(WELCOME_INTROS),
    suggestions: [pickRandom(QUESTION_EXAMPLES), pickRandom(HIGHLIGHT_EXAMPLES), pickRandom(BUILD_EXAMPLES)],
  }
}

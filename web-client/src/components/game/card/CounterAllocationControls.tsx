import { useGameStore } from '@/store/gameStore'
import type { EntityId } from '@/types'

/** Allocate repeated selections on the permanent, using only the server's capacity. */
export function CounterAllocationControls({ cardId, cardName }: { cardId: EntityId; cardName: string }) {
  const capacity = useGameStore((s) => s.decisionSelectionState?.selectionLimits?.[cardId] ?? 0)
  const allocated = useGameStore((s) => s.decisionSelectionState?.selectedOptions.filter((id) => id === cardId).length ?? 0)
  const totalAtMax = useGameStore((s) => s.decisionSelectionState != null &&
    s.decisionSelectionState.selectedOptions.length >= s.decisionSelectionState.maxSelections)
  if (capacity <= 0) return null
  const buttonStyle = { width: 28, height: 28, borderRadius: 4, border: 'none', fontSize: 18 }
  return (
    <div
      data-testid={`counter-allocation-${cardId}`}
      style={{ position: 'absolute', top: 4, left: 0, right: 0, zIndex: 25,
        display: 'flex', justifyContent: 'center', gap: 4, alignItems: 'center',
        background: 'rgba(0, 0, 0, 0.85)', padding: 3, borderRadius: 5 }}
      onClick={(e) => e.stopPropagation()}
    >
      <button
        type="button" aria-label={`Remove one allocation from ${cardName}`}
        style={buttonStyle} disabled={allocated === 0}
        onClick={() => useGameStore.getState().decrementDecisionSelection(cardId)}
      >−</button>
      <span style={{ color: '#eab308', minWidth: 36, textAlign: 'center' }}>{allocated} / {capacity}</span>
      <button
        type="button" aria-label={`Allocate one counter from ${cardName}`}
        style={buttonStyle} disabled={allocated >= capacity || totalAtMax}
        onClick={() => useGameStore.getState().toggleDecisionSelection(cardId)}
      >+</button>
    </div>
  )
}

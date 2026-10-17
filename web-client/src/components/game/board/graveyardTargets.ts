import type { ClientCard, EntityId } from '@/types'
import { ZoneType } from '@/types'

/** A graveyard card that one or more stack objects currently target. */
export interface GraveyardTarget {
  readonly card: ClientCard
  /** Names of the stack objects targeting it, top of the stack first, de-duplicated. */
  readonly sourceNames: readonly string[]
}

/**
 * The cards in one graveyard that objects on the stack target, in the order they were first
 * targeted (walking the stack top-down). Graveyard cards are tiny on the board and usually buried
 * under the top card, so the board pulls these out into a readable callout.
 */
export function collectGraveyardTargets(
  stackCards: readonly ClientCard[],
  graveyardCards: readonly ClientCard[],
): GraveyardTarget[] {
  if (graveyardCards.length === 0) return []
  const byId = new Map(graveyardCards.map((c) => [c.id, c]))
  const order: EntityId[] = []
  const sources = new Map<EntityId, string[]>()
  // The stack selector returns bottom-first; the top object is the one about to resolve.
  for (let i = stackCards.length - 1; i >= 0; i--) {
    const stackCard = stackCards[i]
    if (!stackCard?.targets) continue
    for (const target of stackCard.targets) {
      if (target.type !== 'Card' || !byId.has(target.cardId)) continue
      let names = sources.get(target.cardId)
      if (!names) {
        names = []
        sources.set(target.cardId, names)
        order.push(target.cardId)
      }
      if (!names.includes(stackCard.name)) names.push(stackCard.name)
    }
  }
  return order.map((id) => ({ card: byId.get(id)!, sourceNames: sources.get(id) ?? [] }))
}

/**
 * Readable names for a stack object's card targets that sit in a public zone (graveyard, exile),
 * e.g. "Grizzly Bears (your graveyard)". Permanents and players already get an unambiguous arrow;
 * a card in a pile doesn't, so the stack's text box spells these out. Cards in hidden zones are
 * skipped — the client can't (and mustn't) name them.
 */
export function zoneCardTargetLabels(
  stackCard: ClientCard,
  cards: Readonly<Record<EntityId, ClientCard>>,
  viewerId: EntityId | null,
  playerName: (playerId: EntityId) => string,
): string[] {
  const labels: string[] = []
  for (const target of stackCard.targets ?? []) {
    if (target.type !== 'Card') continue
    const card = cards[target.cardId]
    const zone = card?.zone
    if (!card || !zone) continue
    const zoneName = zone.zoneType === ZoneType.GRAVEYARD ? 'graveyard'
      : zone.zoneType === ZoneType.EXILE ? 'exile'
      : null
    if (!zoneName) continue
    const owner = zone.ownerId === viewerId ? 'your' : `${playerName(zone.ownerId)}'s`
    labels.push(zoneName === 'exile' ? `${card.name} (in exile)` : `${card.name} (${owner} ${zoneName})`)
  }
  return labels
}

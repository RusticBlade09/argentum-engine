import type { EntityId } from '@/types'

/** Intersect the original pool with the server-approved group containing every current pick. */
export function compatibleSelectionPool(
  pool: readonly EntityId[],
  groups: readonly (readonly EntityId[])[] | null | undefined,
  selected: readonly EntityId[],
): readonly EntityId[] {
  if (!groups || selected.length === 0) return pool
  const group = groups.find((ids) => selected.every((id) => ids.includes(id)))
  return pool.filter((id) => group?.includes(id))
}

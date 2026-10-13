import { describe, it, expect } from 'vitest'
import type { EntityId } from '@/types'
import { compatibleSelectionPool } from './selectionGroups'
const ids = (s: string[]) => s as EntityId[]
describe('server-approved selection groups', () => {
  const pool = ids(['a', 'b', 'c'])
  const groups = [ids(['a', 'b']), ids(['c'])]
  it('narrows and restores the highlighted pool without reading characteristics', () => {
    expect(compatibleSelectionPool(pool, groups, ids(['a']))).toEqual(ids(['a', 'b']))
    expect(compatibleSelectionPool(pool, groups, ids(['c']))).toEqual(ids(['c']))
    expect(compatibleSelectionPool(pool, groups, [])).toEqual(pool)
    expect(compatibleSelectionPool(pool, groups, ids(['a', 'c']))).toEqual([])
  })
})

import { describe, expect, it } from 'vitest'
import { canonicalNameIn, designatedCommanders, parseArenaDeckList } from './parseArenaDeck'

describe('importing a Commander section', () => {
  it('designates both commanders of a partner pair', () => {
    const parsed = parseArenaDeckList([
      'Commander',
      '1 Kodama of the East Tree (C16) 40',
      '1 Ishai, Ojutai Dragonspeaker',
      '',
      'Deck',
      '1 Sol Ring',
    ].join('\n'))
    expect(parsed.errors).toEqual([])
    expect(parsed.entries.map((e) => e.name)).toEqual(['Sol Ring'])
    expect(designatedCommanders(parsed.commander)).toEqual([
      'Kodama of the East Tree',
      'Ishai, Ojutai Dragonspeaker',
    ])
  })

  it('designates at most two distinct commanders; the rest stay deck cards', () => {
    const parsed = parseArenaDeckList(['Commander', '1 A', '1 A', '1 B', '1 C'].join('\n'))
    expect(designatedCommanders(parsed.commander)).toEqual(['A', 'B'])
  })

  it('uses the catalogue casing for a name it knows', () => {
    const parsed = parseArenaDeckList(['Commander', '1 kodama of the east tree', '1 Unknown Legend'].join('\n'))
    const canonical = canonicalNameIn([{ name: 'Kodama of the East Tree' }])
    expect(designatedCommanders(parsed.commander, canonical)).toEqual(['Kodama of the East Tree', 'Unknown Legend'])
  })

  it('designates nothing without a Commander section', () => {
    expect(designatedCommanders(parseArenaDeckList('4 Lightning Bolt').commander)).toEqual([])
  })
})

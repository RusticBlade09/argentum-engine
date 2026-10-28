import contextlib
import importlib.machinery
import importlib.util
import io
import json
import re
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root / 'scripts'))
loader = importlib.machinery.SourceFileLoader('cp', str(root / 'scripts/check-card-printing.py'))
spec = importlib.util.spec_from_loader('cp', loader)
cp = importlib.util.module_from_spec(spec)
sys.modules['cp'] = cp
loader.exec_module(cp)
cards = root / 'mtg-sets/2026/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/trk/cards'
names = json.loads((root / '.trk-sweep/names.json').read_text(encoding='utf-8-sig'))
new_cards = []
for path in cards.glob('*.kt'):
    text = path.read_text(encoding='utf-8-sig')
    match = re.search(r'\bcard\("([^"]+)"\)', text)
    if not match or match[1] not in names:
        continue
    name = match[1]
    new_cards.append(name)
    slug = re.sub('[^a-zA-Z0-9]', '', name)
    data = json.loads((root / f'.trk-sweep/{slug}.scryfall.json').read_text(encoding='utf-8-sig'))
    block = text.split('metadata {', 1)[1]
    def read_field(field, source):
        m = re.search(r'\b' + field + r'\s*=\s*("(?:[^"\\]|\\.)*")', source)
        return json.loads(m[1].replace('\\$', '$')) if m else None
    for field, key in [('collectorNumber', 'collector_number'), ('artist','artist'), ('flavorText','flavor_text')]:
        assert read_field(field, block) == data.get(key), (name, field)
    assert read_field('imageUri', block) == data['image_uris']['normal'], name
    assert read_field('typeLine', text) == data['type_line'], name
    assert read_field('oracleText', text) == data['oracle_text'], name
    assert read_field('manaCost', text) == (data.get('mana_cost') or None), name
    for stat in ['power','toughness']:
        if data.get(stat, '').isdigit():
            assert re.search(r'\b'+stat+r'\s*=\s*'+data[stat]+r'\b',text), (name,stat)
    assert 'Rarity.' + data['rarity'].upper() in block, name
    sys.argv = ['check-card-printing.py', name]
    output = io.StringIO()
    with contextlib.redirect_stdout(output):
        code = cp.main()
    if code:
        print(output.getvalue())
        raise SystemExit(code)
    print('Verified metadata and placement:', name, flush=True)
assert len(new_cards) == 56, len(new_cards)
print('All 56 canonical cards verified.')

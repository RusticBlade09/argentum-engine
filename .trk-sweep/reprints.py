import importlib.machinery
import importlib.util
import sys
from pathlib import Path
root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root / 'scripts'))
loader = importlib.machinery.SourceFileLoader('mr', str(root / 'scripts/missing-reprints.py'))
spec = importlib.util.spec_from_loader('mr', loader)
mr = importlib.util.module_from_spec(spec)
sys.modules['mr'] = mr
loader.exec_module(mr)
names = ['Blood Crypt', 'Breeding Pool', 'Godless Shrine', 'Hallowed Fountain', 'Overgrown Tomb', 'Reckless Impulse', 'Sacred Foundry', 'Steam Vents', 'Stomping Ground', 'Temple Garden', 'Watery Grave']
for name in names:
    mr.fetch_printings(name)
    print('Refreshed', name, flush=True)
(root / '.trk-sweep/reprint-names.txt').write_text('\n'.join(names), encoding='utf-8')

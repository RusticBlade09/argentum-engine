"""Where the per-set Scryfall caches live, shared by `card-status` (writer) and `gen-set-totals`.

Each catalogued set is cached as `~/.cache/scryfall/set-<code>.json`. The `set-` prefix keeps the
filename off Windows' reserved device names: a bare `con.json` (Conflux) is silently unwritable
there from Python, which used to make `gen-set-totals` drop Conflux from the published totals
(the same reason Conflux's definitions live in `definitions/conflux/` — see `set_dirs.py`).

Caches written under the older bare `<code>.json` name are still read, so an existing cache keeps
working until its set is next refreshed into the new name.
"""

from __future__ import annotations

from pathlib import Path

CACHE_ROOT = Path.home() / ".cache" / "scryfall"


def set_cache_path(code: str) -> Path:
    """The path a set's cache is written to."""
    return CACHE_ROOT / f"set-{code.lower()}.json"


def existing_set_cache(code: str) -> Path | None:
    """The set's cache file to read — the current name, else the legacy bare name — or None."""
    for path in (set_cache_path(code), CACHE_ROOT / f"{code.lower()}.json"):
        if path.is_file():
            return path
    return None

# starsector_mods

Private Starsector mods, centralized in one repo.

## Layout

- `reference/starfarer-api/` — local copy of the Starsector API (Java source + Javadoc) with a searchable `INDEX.md`. Used as the knowledge base for development (see `CLAUDE.md`).
- `tools/import_api.py` — (re)builds `reference/starfarer-api/` from the game install.

## Updating the API reference

The game ships the API source at `<Starsector install>/starsector-core/starfarer.api.zip`.
That's the same content as https://fractalsoftworks.com/starfarer.api/, matched to your installed version.

```bash
python3 tools/import_api.py "/path/to/Starsector/starsector-core/starfarer.api.zip" --version 0.98a-RC8
```

Replace the version with the one shown on the game's launcher. The import replaces the previous copy, so `git diff` shows what changed in the API between versions.

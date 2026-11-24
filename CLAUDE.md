# Starsector mods

This repo centralizes our Starsector mods.

## Starsector API reference — use the local copy first

The full `starfarer.api` (Java source + Javadoc) lives in `reference/starfarer-api/`.
**Look things up there instead of fetching https://fractalsoftworks.com/starfarer.api/.**

- `reference/starfarer-api/VERSION` — game version the copy came from.
- `reference/starfarer-api/INDEX.md` — every type, one per line: kind, fully-qualified name, one-line summary, path. Grep it first.
- `reference/starfarer-api/src/com/fs/starfarer/api/...` — the sources. Open these for method signatures, constants and Javadoc.

Typical lookups:

```bash
grep -i "hullmod" reference/starfarer-api/INDEX.md
grep -rn "getFleetData" reference/starfarer-api/src
```

If `reference/starfarer-api/` is missing, or the game version changed, re-import it from the game install
(see "Updating the API reference" in README.md) rather than going to the website.

## Writing mods

- One folder per mod in `mods/<mod_id>/`, laid out as it installs into `<Starsector>/mods/`
  (`mod_info.json`, `src/`, `jars/`, `data/`). Add each new mod to the table in README.md.
- Java packages go under `dreukrag.<modname>` (not `data.scripts`, which collides across mods).
- Build with `tools/build_mod.sh <mod_id>`, commit the built jar so the folder is drop-in.
- Prefer changing specs at load (`ModPlugin.onApplicationLoad`) over copying vanilla CSV rows:
  vanilla data files are not in this repo, and overrides break when the game updates them.
- The API sources here are the public API only; core game logic (e.g. how the refit screen enforces
  hullmod limits) is obfuscated and not available, so state assumptions and log what the mod changes.

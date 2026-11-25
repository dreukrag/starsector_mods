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

## Game version on this branch

This branch (`98.5`) targets **0.98.5a-RC6**; `main` targets 0.98a-RC8. Since 0.98.5a the API sources use Java 16
syntax, so `tools/build_mod.sh` compiles with `--release 16`. The behaviour below was verified on 0.98a-RC8;
nothing it relies on changed in the 0.98.5a API, but re-verify in-game before relying on it.

## Verified game behaviour

Facts confirmed in-game (they aren't visible in the public API):

- Logistics hullmod limit (0.98a-RC8): only hullmods tagged `Logistics` (`HullMods.TAG_UI_LOGISTICS`)
  count toward it. Removing the tag from the spec in `onApplicationLoad` exempts that hullmod.
  See `mods/solar_shielding_unrestricted`.
- Replacing a vanilla hullmod's behaviour (0.98a-RC8): subclass its effect class, then call
  `spec.setEffectClass(<subclass name>)` in `onApplicationLoad`. The game picks up the new class
  (`spec.getEffect()` returns it), so no `hull_mods.csv` override is needed.
  See `mods/high_res_sensors_boost`.
- Hullmod OP costs can be changed at load with `spec.setFrigateCost(...)` etc.
- Combat-wide effects: a hullmod's `advanceInCombat` can add an `EveryFrameCombatPlugin` to the engine
  once per battle (guard with `engine.getCustomData()`); `engine.maintainStatusForPlayerShip` shows its status.
- New hullmods: add a row to the mod's own `data/hullmods/hull_mods.csv` with the vanilla columns
  (header in `mods/maintenance_gantries/data/hullmods/hull_mods.csv`); vanilla sprites can be referenced by path.
  A `tier` and no `no_drop` tag puts it in the blueprint drop pool. See `mods/maintenance_gantries`.
- Fleet-wide per-ship stat changes: a `HullModFleetEffect` can put modifiers on each member's stats in
  `onFleetSync`; they show on the fleet screen and drive real costs. Re-check from `advanceInCampaign`
  (player fleet, throttled) in case stats are rebuilt without a sync.
- The refit screen previews ships from scratch (`stats.getFleetMember()` is null there), so modifiers
  applied from outside the ship's own hullmods/skills don't show in refit; the fleet screen shows them.


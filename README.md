# starsector_mods

Private Starsector mods, centralized in one repo.

## Layout

- `reference/starfarer-api/` — local copy of the Starsector API (Java source + Javadoc) with a searchable `INDEX.md`. Used as the knowledge base for development (see `CLAUDE.md`).
- `mods/<mod_id>/` — one folder per mod, laid out exactly as it goes into `<Starsector>/mods/`. Java code in `src/`, built jar in `jars/`.
- `tools/import_api.py` — (re)builds `reference/starfarer-api/` from the game install.
- `tools/build_mod.sh <mod_id>` — compiles a mod's `src/` into `jars/<mod_id>.jar`.

## Game versions

| Branch | Game version | Notes |
| --- | --- | --- |
| `main` | 0.98a-RC8 | Mods verified in-game. |
| `98_5` | 0.98.5a-RC6 | Same mods rebuilt against the 0.98.5a API (Java 16 bytecode). Not yet verified in-game. |

## Mods

| Mod | What it does |
| --- | --- |
| [solar_shielding_unrestricted](mods/solar_shielding_unrestricted) | Solar Shielding no longer counts toward the logistics hullmod limit. |
| [high_res_sensors_boost](mods/high_res_sensors_boost) | High Resolution Sensors give +400/800/1600/3200 sensor range, cost 5/10/15/25 OP, and boost fleet-wide missile speed and maneuverability in combat. |
| [maintenance_gantries](mods/maintenance_gantries) | New logistics hullmod: each carrier cuts every ship's maintenance and recovery cost by 1/3/7/15 supplies (max 50%). Blueprint drop, or granted by Makeshift Equipment. |

## Building mods

```bash
tools/build_mod.sh <mod_id>
```

Without the game installed, this compiles against `reference/starfarer-api/src` and downloads the game's
third-party libraries (LWJGL, org.json, log4j, XStream) from Maven Central into `.build/`.
With the game installed, set `STARSECTOR_CORE=<Starsector>/starsector-core` to compile against the real jars.
Jars target Java 16 (0.98.5a's own API code needs it); set `JAVA_RELEASE=<n>` to change that.

## Updating the API reference

The game ships the API source at `<Starsector install>/starsector-core/starfarer.api.zip`.
That's the same content as https://fractalsoftworks.com/starfarer.api/, matched to your installed version.

```bash
python3 tools/import_api.py "/path/to/Starsector/starsector-core/starfarer.api.zip" --version 0.98a-RC8
```

Replace the version with the one shown on the game's launcher. The import replaces the previous copy, so `git diff` shows what changed in the API between versions.

# Repair and Maintenance Gantries

Adds a new logistics hullmod, **Repair and Maintenance Gantries** (`drk_maintenance_gantries`), using the
vanilla Salvage Gantry icon. Vanilla Salvage Gantry is untouched.

| | Frigate | Destroyer | Cruiser | Capital |
| --- | --- | --- | --- | --- |
| Maintenance cut to every ship in the fleet (supplies/month) | 1 | 3 | 7 | 15 |
| Recovery cost cut to every ship in the fleet (supplies) | 1 | 3 | 7 | 15 |
| OP cost | 20 | 40 | 80 | 160 |

- Reductions from every carrier add up (no diminishing returns), and apply to every ship in the fleet, carriers included.
- No ship's maintenance goes below half its cost, rounded up: a 5/month destroyer stops at 3.
- **Recovery cost** (supplies to recover from deployment) is cut by the same amount, but applied after every
  other modifier: it never goes below half of the cost with all other modifiers, rounded up. A ship at 10
  brought down to 7 by other effects stops at 4; one brought down to 6 stops at 3.
- The refit screen shows costs before gantry reductions (it builds its own preview of the ship, without the
  fleet); the fleet screen shows the actual costs the game uses.
- Carriers only count when not mothballed and at 10% CR or more.
- Doubles the carrier's minimum crew.
- Logistics hullmod (counts toward the two-per-ship limit) and requires a dock to install.

**Makeshift Equipment synergy:** the gantries cut each ship's cost with a flat modifier, and the game applies
flat modifiers before multipliers. Makeshift Equipment's fleet-wide supply reduction (a multiplier, at most
-50% or 100 supplies/month) therefore applies to the already-reduced total. With a large fleet, the
100-supply cap then covers a bigger share of what's left.

## Getting it

- **Blueprint:** tier 2, eligible for hullmod blueprint drops like other unknown, droppable hullmods.
- **Makeshift Equipment:** taking the skill teaches it automatically (campaign message "learned Repair and
  Maintenance Gantries"). It stays known if the skill is later removed.

## How it works

- `data/hullmods/hull_mods.csv` defines the hullmod (same columns as vanilla 0.98a-RC8).
- `MaintenanceGantries` extends `BaseLogisticsHullMod` and implements `HullModFleetEffect`. On every fleet
  sync it sums the active carriers' reductions and puts a capped flat modifier on each member's supplies/month.
  The game can rebuild a ship's stats (e.g. on refit) without a fleet sync, which drops the modifier, so
  `advanceInCampaign` also checks the player fleet 4 times a second and restores any missing or outdated modifier
  (the tooltip mentions the bonus may take a moment to apply after refits).
- `MakeshiftGrantScript` (added on game load, not saved) checks for the skill and adds the hullmod to the player faction.

This mod adds content, so it isn't a utility mod: don't remove it from a save that has ships using the hullmod.

## Install

Copy this folder into `<Starsector>/mods/` and enable it in the launcher.

## Verify in game

1. With Makeshift Equipment, the "learned Repair and Maintenance Gantries" message appears; otherwise use the
   console (`addhullmod drk_maintenance_gantries` with Console Commands) or find a blueprint.
2. Install on 3 frigates: refit should show 20 OP each, and the tooltip a fleet reduction of 3 supplies/month.
3. In the **fleet screen** (not refit), a 5/month destroyer's maintenance should read 3, and recovery costs should
   drop by the fleet reduction down to half.

## Build

```bash
tools/build_mod.sh maintenance_gantries
```

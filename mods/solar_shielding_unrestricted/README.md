# Solar Shielding Unrestricted

Solar Shielding no longer counts as a logistics hullmod, so it doesn't take one of the ship's two logistics slots.

## How it works

At game load, `SolarShieldingUnrestrictedPlugin` removes the `Logistics` tag from the `solar_shielding`
hullmod spec. No vanilla files are overridden, so it's safe to add or remove mid-save.
Side effect: Solar Shielding no longer appears under the "Logistics" filter in the refit screen.

Status: verified working in-game on Starsector 0.98a-RC8.

## Install

Copy this folder (`solar_shielding_unrestricted/`) into `<Starsector>/mods/` and enable it in the launcher.

## Verify in game

1. Check `starsector-core/starsector.log` for `Removed 'Logistics' from Solar Shielding ...`.
2. In refit, give a ship two logistics hullmods, then add Solar Shielding; it should be allowed.

## Build

```bash
tools/build_mod.sh solar_shielding_unrestricted
```

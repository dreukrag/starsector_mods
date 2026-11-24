# High Resolution Sensors - Stat Boost

High Resolution Sensors' fleet sensor range bonus goes from +50/75/100/150 to **+400/800/1600/3200**
(frigate/destroyer/cruiser/capital), and its cost changes to **5/10/15/25 OP**. It's still a logistics hullmod, so it counts toward the two-mod limit.

Unchanged from vanilla: diminishing returns across multiple ships, the 10% CR minimum,
and the S-mod combat sight bonus (+1000/1500/2000/2500).

Fleet totals with the vanilla diminishing-returns formula:

| Ships with HRS | Fleet bonus |
| --- | --- |
| 1 frigate | 400 |
| 3 frigates | 880 |
| 6 destroyers | 2364 |
| 1 capital | 3200 |
| 4 cruisers | 4021 |
| 1 capital + 3 frigates | 4312 |
| 2 capitals, or 1 capital + 2 cruisers | 5621 |

Vanilla's diminishing returns are kept on purpose; the per-ship values are set high enough that
dedicated sensor ships stay worth bringing despite them.

## Combat: fleet-wide missile guidance

Each live ship carrying High Resolution Sensors gives **every ship and fighter on its side**
+5% missile top speed and +10% missile maneuverability (acceleration, turn rate and turn acceleration).
This stacks per ship up to 3 ships, for a maximum of +15% / +30%; more carriers add nothing. A ship stops counting once destroyed,
and the player sees the current bonus as a "High-resolution sensor guided missiles" status in combat.
It works the same for AI fleets, so enemies carrying the hullmod get it too.
The "burn through enemy ECM" part of the tooltip is flavour; it doesn't change ECM/ECCM values.

## How it works

`BoostedHighResSensors` extends vanilla's `HighResSensors` and replaces only the per-ship bonus and the tooltip
(vanilla keeps its values in a private map). At game load, `HrsBoostModPlugin` points the `hiressensors`
hullmod spec at the new class and sets the OP cost directly on the spec.
In combat, the first ship with the hullmod to be deployed adds `HrsMissileGuidancePlugin` to the battle,
which recounts each side every frame and sets the missile bonuses.
To change the numbers, edit `BONUS` in `BoostedHighResSensors.java` the `COST_*` values in
`HrsBoostModPlugin.java`, or `SPEED_PER_SHIP` / `MANEUVER_PER_SHIP` / `MAX_STACKS` in `HrsMissileGuidancePlugin.java`, then rebuild.

## Install

Copy this folder into `<Starsector>/mods/` and enable it in the launcher.

## Verify in game

1. `starsector-core/starsector.log` should contain `High Resolution Sensors now use dreukrag.hrsboost.BoostedHighResSensors`.
   A `WARN` line instead means the swap didn't take.
   It should also log `High Resolution Sensors cost set to 5/10/15/25 OP (was ...)`.
2. In refit, the hullmod should cost 5/10/15/25 OP and its tooltip should read 400/800/1600/3200.
3. The fleet's sensor range in the campaign should rise by the amounts above.
4. In combat with a ship carrying the hullmod deployed, a "High-resolution sensor guided missiles" status should show
   the bonus, and missiles should fly visibly faster with several carriers.

## Build

```bash
tools/build_mod.sh high_res_sensors_boost
```

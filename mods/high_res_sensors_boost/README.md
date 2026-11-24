# High Resolution Sensors - Stat Boost

High Resolution Sensors' fleet sensor range bonus goes from +50/75/100/150 to **+200/400/800/1600**
(frigate/destroyer/cruiser/capital). It's still a logistics hullmod, so it counts toward the two-mod limit.

Unchanged from vanilla: diminishing returns across multiple ships, the 10% CR minimum,
and the S-mod combat sight bonus (+1000/1500/2000/2500).

Fleet totals with the vanilla diminishing-returns formula:

| Ships with HRS | Fleet bonus |
| --- | --- |
| 1 frigate | 200 |
| 3 frigates | 440 |
| 4 cruisers | 2010 |
| 1 capital | 1600 |
| 2 capitals, or 1 capital + 2 cruisers | 2810 |

## How it works

`BoostedHighResSensors` extends vanilla's `HighResSensors` and replaces only the per-ship bonus and the tooltip
(vanilla keeps its values in a private map). At game load, `HrsBoostModPlugin` points the `hiressensors`
hullmod spec at the new class. To change the numbers, edit `BONUS` in `BoostedHighResSensors.java` and rebuild.

## Install

Copy this folder into `<Starsector>/mods/` and enable it in the launcher.

## Verify in game

1. `starsector-core/starsector.log` should contain `High Resolution Sensors now use dreukrag.hrsboost.BoostedHighResSensors`.
   A `WARN` line instead means the swap didn't take.
2. The hullmod's tooltip in refit should read 200/400/800/1600.
3. The fleet's sensor range in the campaign should rise by the amounts above.

## Build

```bash
tools/build_mod.sh high_res_sensors_boost
```

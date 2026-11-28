# Cass Economy Overhaul — design

Target game version: **0.98.5a** (API reference in `reference/starfarer-api/`).
Mod id `cass_economy_overhaul`, Java package `dreukrag.casseconomy`.

Three modules, each with its own on/off switch and numbers:

| Module | Package | What it does |
| --- | --- | --- |
| Colony management | `dreukrag.casseconomy.colony` | One major colony crisis at a time; reveal Remnant nexus |
| Economy | `dreukrag.casseconomy.economy` | More battle credits, pricier AI cores, ship broker contact |
| Military | `dreukrag.casseconomy.military` | AI fleets recover CR more slowly after fighting you |

## Dependencies

- **Required:** LazyLib (every mod you use needs it already).
- **Optional:** LunaLib 2.1.0 (in-game settings menu). Without it, values come from the mod's
  `data/config/settings.json`.
- **Optional:** Console Commands 4.0.9 (status and debug commands). Commands only load when it is enabled.

## Settings

Every value below is a setting. Where it lives:

1. In game, with LunaLib: Mod Settings menu (Shift+F2). Applies immediately.
2. Without LunaLib: `mods/cass_economy_overhaul/data/config/settings.json`, then restart.
3. With Console Commands: `cass_status`, `cass_set <key> <value>` (testing).

---

## 1. Colony management

### How vanilla decides when a crisis happens

`HostileActivityEventIntel` (the "Colony Crises" event):

- The progress bar runs 0–600 and fills from each faction's monthly points × the colony defense multiplier,
  capped by `ha_maxMonthlyProgress`.
- Once progress passes a random point between 425 and 500, one faction's crisis is picked. The odds are its base
  frequency × its share of the points. The same faction can't be picked twice in a row.
- At 600 the crisis fires, and progress resets to 0–400. The roll favours the low end, so about 133 on average.
- **There is no check across factions.** Persean League, Sindrian Diktat and Luddic Church only skip a new crisis while
  their *own* crisis is running. Hegemony and Tri-Tachyon don't even check their own. The bar keeps filling while a
  crisis plays out, so a second one is usually picked before the first ends.

Changes in 0.98.5: "Escalate crisis" now sets the bar to 595 (was 550). The Sindrian Diktat only adds points while
the new Askonia standoff storyline is in progress.

### Gating: one major crisis at a time

Major factions: Hegemony, Tri-Tachyon, Persean League, Sindrian Diktat, Luddic Church.
**Not** major (always allowed): pirates, Luddic Path, Remnant.

How we tell that a major crisis is running:

| Crisis | Check |
| --- | --- |
| Persean League blockade / punitive expedition | `PerseanLeagueBlockade.get()`, `PerseanLeaguePunitiveExpedition.get()` |
| Sindrian Diktat punitive expedition | `SindrianDiktatPunitiveExpedition.get()` |
| Luddic Church takeover | `KnightsOfLuddTakeoverExpedition.get()` |
| Tri-Tachyon mercenary attack | `TTMercenaryAttack.get()` |
| Hegemony AI inspection | Scan the intel list for `HegemonyInspectionIntel` that hasn't ended |

A background script watches the crisis stage. If it picked a major faction while another major crisis is running
(or during the cooldown), the pick is cancelled with `resetHA_EVENT()`, so the game picks again and pirates or the
Luddic Path can be chosen instead. No vanilla classes are replaced.

| Setting | Default | Meaning |
| --- | --- | --- |
| `colony_enabled` | true | Module on/off |
| `crisis_gateMajor` | true | Only one major crisis at a time |
| `crisis_cooldownDays` | 60 | Days after a major crisis ends before the next major one can be picked |
| `crisis_resetMin` | 0 | Lowest progress after a crisis fires (vanilla 0) |
| `crisis_resetMax` | 400 | Highest progress after a crisis fires (vanilla 400) |

### Remnant nexus reveal

The nexus is a Remnant station in the same system as your colony (`RemnantHostileActivityFactor.getRemnantNexus`).
When a nexus first starts adding crisis points, or launches an attack, the mod adds a "Remnant Nexus" intel entry
that points at the station on the map. This mirrors how the Pather base is revealed (`base.makeKnown()`).
If the station is still hard to find on sensors, we also make it easier to detect (to test in game).

| Setting | Default |
| --- | --- |
| `crisis_revealNexus` | true |

---

## 2. Economy

NPC factions have no credit balance in the API. Their fleet strength comes from their markets' fleet-size stats, not
from money. So none of these changes make NPCs richer.

Avoid raising **commodity** base prices: that makes supplies and fuel cost more, and battle loot is filled by value,
so you'd get fewer units.

### Battle credits

Vanilla (`FleetEncounterContext.generatePlayerLoot`): battle value = enemy FP lost × `salvageValuePerFP` ×
the player fleet's `Stats.BATTLE_SALVAGE_MULT_FLEET`. A random share of that (`salvageFractionCreditsMin`/`Max`)
becomes credits, and the rest becomes metals, supplies, fuel and heavy machinery.

We add to `BATTLE_SALVAGE_MULT_FLEET` on the player fleet. Only the player's loot reads it.

| Setting | Default | Meaning |
| --- | --- | --- |
| `economy_enabled` | true | Module on/off |
| `loot_battleMult` | 2.0 | Battle credits and commodity loot × this |

### AI cores

Vanilla (`rulecmd/salvage/AICores.java`): credits = the core's base price × the buying faction's `AICoreValueMult`.
Reputation = 15/5/2/1 per Omega/Alpha/Beta/Gamma × the faction's `AICoreRepMult`.

We multiply each core's commodity base price at load, so each faction's credit/rep trade-off stays the same.
Side effect: buying cores costs more too.

| Setting | Default |
| --- | --- |
| `aicore_gammaMult` | 1.5 |
| `aicore_betaMult` | 2.0 |
| `aicore_alphaMult` | 3.0 |
| `aicore_omegaMult` | 1.0 |

### Ship broker contact

A "ship broker" person on every market with a spaceport. Hidden pirate and Pather bases are skipped.
Added to the market's comm directory at game load, the same way vanilla adds contacts.

- Each broker holds **one request: a hull size**. The odds favour destroyers and cruisers, capitals less, frigates rare.
- Fulfil it: pick a ship of that size from your fleet (the game's ship picker) and get paid
  `member.getBaseBuyValue() × broker_payoutMult`. `getBaseBuyValue` counts mounted weapons and wings, before tariffs.
  A new request is rolled straight away.
- **1 story point: re-roll** the request. Always a different size, never a frigate.
- **1 story point: sell any ship**, at the same payout.
- Your flagship can't be sold. An AI core captain is returned to your cargo (as Nexerelin does).

For comparison, Nexerelin's "buy ship" mission (`nex_bShip`) pays `getBaseBuyValue() × buyShipBasePriceMult`, which is
1.6 in `exerelin_config.json`. Its requests go by tech base and other conditions and it appears occasionally.
The broker goes by size and is always there.

| Setting | Default |
| --- | --- |
| `broker_enabled` | true |
| `broker_payoutMult` | 1.6 |
| `broker_weightFrigate` / `Destroyer` / `Cruiser` / `Capital` | 5 / 40 / 40 / 15 |
| `broker_storyPointOptions` | true |

---

## 3. Military

Harrying a disengaging fleet costs each of its ships CR equal to its deployment cost
(`FleetEncounterContext.applyPursuitOption`). AI fleets then recover CR at their normal daily rate
(`getBaseCRRecoveryRatePercentPerDay`).

After any battle the player is in (`reportBattleFinished`), every non-player ship in the battle gets a buff that
multiplies its CR recovery rate. This uses the same mechanism as vanilla's `CRRecoveryBuff`. Only fleets you've fought
are affected.

| Setting | Default | Meaning |
| --- | --- | --- |
| `military_enabled` | true | Module on/off |
| `cr_aiRecoveryMult` | 0.33 | AI CR recovery rate × this |
| `cr_buffDays` | 30 | How long the slower recovery lasts |

---

## Open questions

1. ~~Extended mode?~~ Not played. The `ext_*` settings (salaries, combat bonus pay, tariffs, drops) only apply
   in Extended mode, so the mod ignores them.
2. Defaults above (cooldown 60 days, loot ×2, AI cores ×1.5/×2/×3, CR ×0.33 for 30 days) — OK as a starting point?

## Build order

1. Colony management (crisis gating, nexus reveal)
2. Military (CR recovery)
3. Economy (battle loot, AI cores, ship broker)

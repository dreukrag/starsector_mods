package dreukrag.hrsboost;

import java.util.Iterator;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;

/**
 * Fleet-wide missile guidance from High Resolution Sensors.
 *
 * Every frame, counts each side's live, non-fighter ships that carry the hullmod and gives every
 * ship (and fighter) on that side +SPEED_PER_SHIP% missile top speed and +MANEUVER_PER_SHIP%
 * missile maneuverability per counted ship, counting at most MAX_STACKS ships. Bonuses drop as those ships are destroyed, and are
 * removed when none are left. Applies to both sides, so enemy fleets with the hullmod get it too.
 *
 * Added to the combat engine by BoostedHighResSensors the first time a ship with the hullmod
 * is deployed in a battle.
 */
public class HrsMissileGuidancePlugin extends BaseEveryFrameCombatPlugin {

	public static final float SPEED_PER_SHIP = 5f;
	public static final float MANEUVER_PER_SHIP = 10f;
	/** Ships beyond this many per side don't add more bonus. */
	public static final int MAX_STACKS = 3;

	public static final String ENGINE_KEY = "dreukrag_hrs_missile_guidance";
	private static final String MOD_ID = "dreukrag_hrs_missile_guidance";
	private static final Object STATUS_KEY = new Object();

	/** Adds the plugin to the current battle once. */
	public static void ensureAdded(CombatEngineAPI engine) {
		if (engine == null || engine.getCustomData().containsKey(ENGINE_KEY)) return;
		engine.getCustomData().put(ENGINE_KEY, Boolean.TRUE);
		engine.addPlugin(new HrsMissileGuidancePlugin());
	}

	public void advance(float amount, List events) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null) return;

		List ships = engine.getShips();
		int playerCount = 0;
		int enemyCount = 0;
		Iterator it = ships.iterator();
		while (it.hasNext()) {
			ShipAPI s = (ShipAPI) it.next();
			if (!providesGuidance(s)) continue;
			if (s.getOwner() == 0) playerCount++;
			else if (s.getOwner() == 1) enemyCount++;
		}

		playerCount = Math.min(playerCount, MAX_STACKS);
		enemyCount = Math.min(enemyCount, MAX_STACKS);

		it = ships.iterator();
		while (it.hasNext()) {
			ShipAPI s = (ShipAPI) it.next();
			int count = s.getOwner() == 0 ? playerCount : s.getOwner() == 1 ? enemyCount : 0;
			apply(s.getMutableStats(), count);
		}

		if (playerCount > 0 && engine.getPlayerShip() != null) {
			HullModSpecAPI spec = Global.getSettings().getHullModSpec(HrsBoostModPlugin.HULLMOD_ID);
			String icon = spec != null ? spec.getSpriteName() : null;
			engine.maintainStatusForPlayerShip(STATUS_KEY, icon, "Sensor-guided missiles",
					"+" + Math.round(playerCount * SPEED_PER_SHIP) + "% speed, +"
							+ Math.round(playerCount * MANEUVER_PER_SHIP) + "% maneuverability", false);
		}
	}

	private static boolean providesGuidance(ShipAPI s) {
		if (s == null || s.isFighter() || s.isHulk() || !s.isAlive()) return false;
		return s.getVariant() != null && s.getVariant().hasHullMod(HrsBoostModPlugin.HULLMOD_ID);
	}

	static void apply(MutableShipStatsAPI stats, int count) {
		if (count <= 0) {
			stats.getMissileMaxSpeedBonus().unmodify(MOD_ID);
			stats.getMissileAccelerationBonus().unmodify(MOD_ID);
			stats.getMissileMaxTurnRateBonus().unmodify(MOD_ID);
			stats.getMissileTurnAccelerationBonus().unmodify(MOD_ID);
			return;
		}
		float speed = count * SPEED_PER_SHIP;
		float maneuver = count * MANEUVER_PER_SHIP;
		stats.getMissileMaxSpeedBonus().modifyPercent(MOD_ID, speed);
		stats.getMissileAccelerationBonus().modifyPercent(MOD_ID, maneuver);
		stats.getMissileMaxTurnRateBonus().modifyPercent(MOD_ID, maneuver);
		stats.getMissileTurnAccelerationBonus().modifyPercent(MOD_ID, maneuver);
	}
}

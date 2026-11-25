package dreukrag.maintgantry;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.hullmods.BaseLogisticsHullMod;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Repair and Maintenance Gantries.
 *
 * Every active ship carrying this hullmod (not mothballed, CR at or above MIN_CR) reduces the
 * monthly maintenance of every ship in its fleet by REDUCTION[carrier hull size]. Reductions from
 * several carriers add up, but no ship drops below half its maintenance, rounded up.
 *
 * The reduction is a flat modifier on supplies/month. The game applies flat modifiers before
 * multipliers, so Makeshift Equipment's fleet-wide multiplier acts on the already-reduced cost.
 *
 * Recovery cost (supplies to recover from deployment) is reduced by the same amount, but last:
 * the cap is half of the cost after all other modifiers, rounded up.
 *
 * Also doubles the carrier's minimum crew.
 */
public class MaintenanceGantries extends BaseLogisticsHullMod implements HullModFleetEffect {

	public static final String HULLMOD_ID = "drk_maintenance_gantries";
	public static final String MOD_ID = "drk_maintenance_gantries";
	public static final String MOD_DESC = "Repair and maintenance gantries";

	public static final float MIN_CR = 0.1f;
	public static final float MAX_REDUCTION_FRACTION = 0.5f;
	public static final float MIN_CREW_MULT = 2f;

	public static final Map REDUCTION = new HashMap();
	static {
		REDUCTION.put(HullSize.FRIGATE, Float.valueOf(1f));
		REDUCTION.put(HullSize.DESTROYER, Float.valueOf(3f));
		REDUCTION.put(HullSize.CRUISER, Float.valueOf(7f));
		REDUCTION.put(HullSize.CAPITAL_SHIP, Float.valueOf(15f));
	}

	public static float reductionFor(HullSize size) {
		Float v = (Float) REDUCTION.get(size);
		return v == null ? 0f : v.floatValue();
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMinCrewMod().modifyMult(id, MIN_CREW_MULT);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) reductionFor(HullSize.FRIGATE);
		if (index == 1) return "" + (int) reductionFor(HullSize.DESTROYER);
		if (index == 2) return "" + (int) reductionFor(HullSize.CRUISER);
		if (index == 3) return "" + (int) reductionFor(HullSize.CAPITAL_SHIP);
		if (index == 4) return "" + Math.round(MAX_REDUCTION_FRACTION * 100f) + "%";
		return null;
	}

	// ---- fleet effect ----

	/** How often the player fleet is re-checked, in real-time seconds. */
	public static final float RECHECK_INTERVAL = 0.25f;
	private static long lastRecheckNanos = 0L;

	/**
	 * Called every frame for every fleet. Only the player fleet is handled, at most every
	 * RECHECK_INTERVAL seconds: it re-applies the reduction when a ship's stats were rebuilt
	 * (e.g. after a refit) without a fleet sync, which would otherwise leave ships without the
	 * modifier until the next sync. Does nothing when every ship already has the right modifier.
	 */
	public void advanceInCampaign(CampaignFleetAPI fleet) {
		if (fleet == null || !fleet.isPlayerFleet()) return;
		long now = System.nanoTime();
		if (now - lastRecheckNanos < (long) (RECHECK_INTERVAL * 1e9f)) return;
		lastRecheckNanos = now;
		syncFleet(fleet, false);
	}

	public boolean withAdvanceInCampaign() {
		return true;
	}

	public boolean withOnFleetSync() {
		return true;
	}

	/** Total per-ship reduction the fleet's active carriers provide, in supplies/month. */
	public static float getFleetReduction(CampaignFleetAPI fleet) {
		if (fleet == null || fleet.getFleetData() == null) return 0f;
		float total = 0f;
		Iterator it = fleet.getFleetData().getMembersListCopy().iterator();
		while (it.hasNext()) {
			FleetMemberAPI m = (FleetMemberAPI) it.next();
			if (isActiveCarrier(m)) total += reductionFor(m.getHullSpec().getHullSize());
		}
		return total;
	}

	public static boolean isActiveCarrier(FleetMemberAPI m) {
		if (m == null || m.isMothballed() || m.getVariant() == null) return false;
		if (!m.getVariant().hasHullMod(HULLMOD_ID)) return false;
		return m.getRepairTracker() != null && m.getRepairTracker().getCR() >= MIN_CR;
	}

	/** Called for every fleet on every fleet sync; cheap when the fleet has no carriers. */
	public void onFleetSync(CampaignFleetAPI fleet) {
		syncFleet(fleet, true);
	}

	/**
	 * Puts the right reduction on every member. With force=false, members whose modifier is
	 * already correct are left alone, so calling this every frame doesn't churn the stats.
	 */
	public static void syncFleet(CampaignFleetAPI fleet, boolean force) {
		if (fleet == null || fleet.getFleetData() == null) return;
		float reduction = getFleetReduction(fleet);
		List members = fleet.getFleetData().getMembersListCopy();
		Iterator it = members.iterator();
		while (it.hasNext()) {
			FleetMemberAPI m = (FleetMemberAPI) it.next();
			if (m.getStats() == null) continue;
			setOurFlat(m.getStats().getSuppliesPerMonth(), maintenanceFlat(m.getStats().getSuppliesPerMonth(), reduction), force);
			setOurFlat(m.getStats().getSuppliesToRecover(), recoveryFlat(m.getStats().getSuppliesToRecover(), reduction), force);
		}
	}

	/** This mod's flat modifier on the stat (negative when reducing), 0 if none. */
	public static float ourFlat(MutableStat stat) {
		MutableStat.StatMod mod = stat.getFlatStatMod(MOD_ID);
		return mod == null ? 0f : mod.value;
	}

	/** The stat's value before multipliers, ignoring this mod. */
	private static float preMultWithoutUs(MutableStat stat) {
		float base = stat.getBaseValue();
		return base + base * stat.getPercentMod() / 100f + stat.getFlatMod() - ourFlat(stat);
	}

	private static float halfRoundedUp(float v) {
		return (float) Math.ceil(v * (1f - MAX_REDUCTION_FRACTION) - 0.001f);
	}

	/**
	 * Maintenance: reduce by the fleet reduction, but keep the cost before multipliers at or above
	 * half of what it is without this mod, rounded up. Multipliers (Makeshift Equipment) apply after.
	 * Returns the flat modifier to use (0 or negative).
	 */
	public static float maintenanceFlat(MutableStat stat, float reduction) {
		if (reduction <= 0f) return 0f;
		float before = preMultWithoutUs(stat);
		if (before <= 0f) return 0f;
		return -Math.max(0f, Math.min(reduction, before - halfRoundedUp(before)));
	}

	/**
	 * Recovery cost: applied after every other modifier. The final cost is reduced by the fleet
	 * reduction, but not below half of the cost with all other modifiers, rounded up.
	 * Because the game applies multipliers after flat modifiers, the flat value is scaled so the
	 * final cost lands exactly on target. Returns the flat modifier to use (0 or negative).
	 */
	public static float recoveryFlat(MutableStat stat, float reduction) {
		if (reduction <= 0f) return 0f;
		float mult = stat.getMult();
		if (mult <= 0f) return 0f;
		float others = preMultWithoutUs(stat) * mult;
		if (others <= 0f) return 0f;
		float target = Math.max(halfRoundedUp(others), others - reduction);
		if (target >= others) return 0f;
		return -(others - target) / mult;
	}

	/** Sets this mod's flat modifier; skips the change when it's already correct unless force. */
	private static void setOurFlat(MutableStat stat, float flat, boolean force) {
		if (!force && Math.abs(ourFlat(stat) - flat) <= 0.001f) return;
		stat.unmodifyFlat(MOD_ID);
		if (flat < 0f) stat.modifyFlat(MOD_ID, flat, MOD_DESC);
	}

	// ---- tooltip ----

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltip.addPara("The repair and maintenance systems need some time to readjust after refitting or ship " +
				"loadout changes; the maintenance bonus may take a moment to apply.", Misc.getGrayColor(), 10f);
		tooltip.addPara("The refit screen shows maintenance and recovery costs before gantry reductions; " +
				"the fleet screen shows the actual costs.", Misc.getGrayColor(), 3f);

		if (isForModSpec || ship == null) return;
		if (Global.getSettings().getCurrentState() == GameState.TITLE) return;
		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
		if (fleet == null) return;

		float opad = 10f;
		Color h = Misc.getHighlightColor();
		float total = getFleetReduction(fleet);
		tooltip.addPara("Your fleet's gantries currently reduce each ship's maintenance by up to %s supplies/month, " +
				"and its recovery cost by up to %s supplies.",
				opad, h, "" + Math.round(total), "" + Math.round(total));
	}
}

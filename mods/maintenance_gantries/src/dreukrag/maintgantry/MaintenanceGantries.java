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

	public void advanceInCampaign(CampaignFleetAPI fleet) {
	}

	public boolean withAdvanceInCampaign() {
		return false;
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
		if (fleet == null || fleet.getFleetData() == null) return;
		float reduction = getFleetReduction(fleet);
		List members = fleet.getFleetData().getMembersListCopy();
		Iterator it = members.iterator();
		while (it.hasNext()) {
			FleetMemberAPI m = (FleetMemberAPI) it.next();
			applyReduction(m.getStats().getSuppliesPerMonth(), reduction);
		}
	}

	/**
	 * Applies a flat reduction to a supplies/month stat, limited so the cost before multipliers
	 * stays at or above half of what it was, rounded up. Returns the reduction applied.
	 */
	public static float applyReduction(MutableStat stat, float reduction) {
		stat.unmodifyFlat(MOD_ID);
		if (reduction <= 0f) return 0f;
		float base = stat.getBaseValue();
		float before = base + base * stat.getPercentMod() / 100f + stat.getFlatMod();
		if (before <= 0f) return 0f;
		float floor = (float) Math.ceil(before * (1f - MAX_REDUCTION_FRACTION) - 0.001f);
		float applied = Math.min(reduction, before - floor);
		if (applied <= 0f) return 0f;
		stat.modifyFlat(MOD_ID, -applied, MOD_DESC);
		return applied;
	}

	// ---- tooltip ----

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		if (isForModSpec || ship == null) return;
		if (Global.getSettings().getCurrentState() == GameState.TITLE) return;
		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
		if (fleet == null) return;

		float opad = 10f;
		Color h = Misc.getHighlightColor();
		float total = getFleetReduction(fleet);
		tooltip.addPara("Your fleet's gantries currently reduce each ship's maintenance by up to %s supplies/month.",
				opad, h, "" + Math.round(total));
	}
}

package dreukrag.hrsboost;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.HighResSensors;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Vanilla High Resolution Sensors with bigger campaign sensor range bonuses.
 *
 * Extends the vanilla class, so it stays a logistics hullmod and keeps vanilla's fleet sync,
 * diminishing returns (HighResSensors.getAdjustedHRSModifier) and S-mod combat sight bonus.
 * Only the per-ship bonus and the tooltip that prints it are replaced, because vanilla
 * keeps its values in a private map.
 */
public class BoostedHighResSensors extends HighResSensors {

	public static final Map BONUS = new HashMap();
	static {
		BONUS.put(HullSize.FRIGATE, Float.valueOf(200f));
		BONUS.put(HullSize.DESTROYER, Float.valueOf(400f));
		BONUS.put(HullSize.CRUISER, Float.valueOf(800f));
		BONUS.put(HullSize.CAPITAL_SHIP, Float.valueOf(1600f));
	}

	// Same as vanilla: S-mod adds combat sight radius.
	private static final Map COMBAT_BONUS = new HashMap();
	static {
		COMBAT_BONUS.put(HullSize.FRIGATE, Float.valueOf(1000f));
		COMBAT_BONUS.put(HullSize.DESTROYER, Float.valueOf(1500f));
		COMBAT_BONUS.put(HullSize.CRUISER, Float.valueOf(2000f));
		COMBAT_BONUS.put(HullSize.CAPITAL_SHIP, Float.valueOf(2500f));
	}

	public static float bonusFor(HullSize size) {
		Float v = (Float) BONUS.get(size);
		return v == null ? 0f : v.floatValue();
	}

	private static String str(Map m, HullSize size) {
		return "" + ((Float) m.get(size)).intValue();
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.HRS_SENSOR_RANGE_MOD).modifyFlat(id, bonusFor(hullSize));
		if (isSMod(stats)) {
			Float mag = (Float) COMBAT_BONUS.get(hullSize);
			if (mag != null) stats.getSightRadiusMod().modifyFlat(id, mag.floatValue());
		}
	}

	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return str(COMBAT_BONUS, HullSize.FRIGATE);
		if (index == 1) return str(COMBAT_BONUS, HullSize.DESTROYER);
		if (index == 2) return str(COMBAT_BONUS, HullSize.CRUISER);
		if (index == 3) return str(COMBAT_BONUS, HullSize.CAPITAL_SHIP);
		return null;
	}

	// Copy of vanilla's tooltip, reading BONUS instead of vanilla's private values.
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		tooltip.addPara("A ship with high resolution sensors increases the fleet's sensor range by %s/%s/%s/%s," +
				" depending on hull size. " +
				"Each additional ship with high resolution sensors provides diminishing returns. " +
				"The higher the highest sensor range increase from a single ship in the fleet, the later diminishing returns kick in.",
				opad, h,
				str(BONUS, HullSize.FRIGATE),
				str(BONUS, HullSize.DESTROYER),
				str(BONUS, HullSize.CRUISER),
				str(BONUS, HullSize.CAPITAL_SHIP));

		if (isForModSpec || ship == null) return;
		if (Global.getSettings().getCurrentState() == GameState.TITLE) return;

		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
		float fleetMod = getAdjustedHRSModifier(fleet, null, 0f);
		float currShipMod = bonusFor(hullSize);
		float fleetModWithOneMore = getAdjustedHRSModifier(fleet, null, currShipMod);
		float fleetModWithoutThisShip = getAdjustedHRSModifier(fleet, ship.getFleetMemberId(), 0f);

		tooltip.addPara("The total sensor range increase for your fleet is %s.", opad, h,
				"" + Math.round(fleetMod));

		float cr = ship.getCurrentCR();
		Iterator it = fleet.getFleetData().getMembersListCopy().iterator();
		while (it.hasNext()) {
			FleetMemberAPI member = (FleetMemberAPI) it.next();
			if (member.getId().equals(ship.getFleetMemberId())) {
				cr = member.getRepairTracker().getCR();
			}
		}

		if (cr < MIN_CR) {
			String crText = "" + Math.round(MIN_CR * 100f) + "%";
			String withOne = "" + Math.round(fleetModWithOneMore);
			LabelAPI label = tooltip.addPara("This ship's combat readiness is below %s " +
					"and its high resolution sensors can not be utilized. Bringing this ship into readiness " +
					"would increase the fleetwide bonus to %s.", opad, h, crText, withOne);
			label.setHighlightColors(new Color[] {bad, h});
			label.setHighlight(new String[] {crText, withOne});
		} else if (fleetMod > currShipMod) {
			tooltip.addPara("Removing this ship would decrease it to %s. Adding another ship of the same type " +
					"would increase it to %s.", opad, h,
					"" + Math.round(fleetModWithoutThisShip),
					"" + Math.round(fleetModWithOneMore));
		} else {
			tooltip.addPara("Adding another ship of the same type would increase it to %s.", opad, h,
					"" + Math.round(fleetModWithOneMore));
		}
	}
}

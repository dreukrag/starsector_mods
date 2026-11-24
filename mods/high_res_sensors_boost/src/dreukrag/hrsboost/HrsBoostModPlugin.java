package dreukrag.hrsboost;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.loading.HullModSpecAPI;

/**
 * Sets the High Resolution Sensors ordnance point cost and points the hullmod at BoostedHighResSensors.
 */
public class HrsBoostModPlugin extends BaseModPlugin {

	public static final String HULLMOD_ID = "hiressensors";

	// Ordnance point cost by hull size.
	public static final int COST_FRIGATE = 5;
	public static final int COST_DESTROYER = 10;
	public static final int COST_CRUISER = 15;
	public static final int COST_CAPITAL = 25;

	private static final Logger log = Global.getLogger(HrsBoostModPlugin.class);

	public void onApplicationLoad() throws Exception {
		HullModSpecAPI spec = Global.getSettings().getHullModSpec(HULLMOD_ID);
		if (spec == null) {
			log.warn("Hullmod '" + HULLMOD_ID + "' not found; High Resolution Sensors left unchanged.");
			return;
		}
		String oldCosts = spec.getFrigateCost() + "/" + spec.getDestroyerCost() + "/"
				+ spec.getCruiserCost() + "/" + spec.getCapitalCost();
		spec.setFrigateCost(COST_FRIGATE);
		spec.setDestroyerCost(COST_DESTROYER);
		spec.setCruiserCost(COST_CRUISER);
		spec.setCapitalCost(COST_CAPITAL);
		log.info("High Resolution Sensors cost set to " + COST_FRIGATE + "/" + COST_DESTROYER + "/"
				+ COST_CRUISER + "/" + COST_CAPITAL + " OP (was " + oldCosts + ").");

		String before = spec.getEffectClass();
		spec.setEffectClass(BoostedHighResSensors.class.getName());

		HullModEffect effect = spec.getEffect();
		if (effect instanceof BoostedHighResSensors) {
			log.info("High Resolution Sensors now use " + BoostedHighResSensors.class.getName()
					+ " (was " + before + ").");
		} else {
			log.warn("Set effect class on '" + HULLMOD_ID + "', but its effect is still "
					+ (effect == null ? "null" : effect.getClass().getName())
					+ "; the boosted values may not apply.");
		}
	}
}

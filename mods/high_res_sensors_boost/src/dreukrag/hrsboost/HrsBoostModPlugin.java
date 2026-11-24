package dreukrag.hrsboost;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.loading.HullModSpecAPI;

/** Points the vanilla High Resolution Sensors hullmod at BoostedHighResSensors. */
public class HrsBoostModPlugin extends BaseModPlugin {

	public static final String HULLMOD_ID = "hiressensors";

	private static final Logger log = Global.getLogger(HrsBoostModPlugin.class);

	public void onApplicationLoad() throws Exception {
		HullModSpecAPI spec = Global.getSettings().getHullModSpec(HULLMOD_ID);
		if (spec == null) {
			log.warn("Hullmod '" + HULLMOD_ID + "' not found; High Resolution Sensors left unchanged.");
			return;
		}
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

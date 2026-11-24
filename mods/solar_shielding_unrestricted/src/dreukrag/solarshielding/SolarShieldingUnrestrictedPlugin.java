package dreukrag.solarshielding;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;

/**
 * Strips the "Logistics" tag from Solar Shielding so it no longer counts
 * toward the per-ship logistics hullmod limit.
 *
 * Written without generics or lambdas so it also compiles under the
 * game's built-in script compiler (Janino) if the jar is ever dropped.
 */
public class SolarShieldingUnrestrictedPlugin extends BaseModPlugin {

	private static final Logger log = Global.getLogger(SolarShieldingUnrestrictedPlugin.class);

	public void onApplicationLoad() throws Exception {
		HullModSpecAPI spec = Global.getSettings().getHullModSpec(HullMods.SOLAR_SHIELDING);
		if (spec == null) {
			log.warn("Hullmod '" + HullMods.SOLAR_SHIELDING + "' not found; nothing changed.");
			return;
		}
		int removed = stripLogistics(spec.getUITags(), "uiTags") + stripLogistics(spec.getTags(), "tags");
		if (removed == 0) {
			log.warn("Solar Shielding had no Logistics tag; nothing changed.");
		}
	}

	private int stripLogistics(Set tags, String which) {
		int removed = 0;
		Iterator it = tags.iterator();
		while (it.hasNext()) {
			String tag = (String) it.next();
			if (HullMods.TAG_UI_LOGISTICS.equalsIgnoreCase(tag)) {
				it.remove();
				removed++;
				log.info("Removed '" + tag + "' from Solar Shielding " + which + ".");
			}
		}
		return removed;
	}
}

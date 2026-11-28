package dreukrag.casseconomy;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;

/**
 * All tunable values. Defaults come from this mod's data/config/settings.json (merged into the game's
 * settings); when LunaLib is enabled its saved values override them. Reloaded whenever LunaLib settings change.
 */
public final class CassConfig {

	public static final String MOD_ID = "cass_economy_overhaul";
	public static final Logger log = Global.getLogger(CassConfig.class);

	private static final int VANILLA_RESET_MIN = 0;
	private static final int VANILLA_RESET_MAX = 400;

	// --- Colony management ---
	public static boolean colonyEnabled = true;
	public static boolean gateMajor = true;
	public static int cooldownDays = 60;
	public static int resetMin = VANILLA_RESET_MIN;
	public static int resetMax = VANILLA_RESET_MAX;
	public static boolean revealNexus = true;

	private CassConfig() {}

	public static void load() {
		colonyEnabled = bool("cass_colony_enabled", true);
		gateMajor = bool("cass_crisis_gateMajor", true);
		cooldownDays = Math.max(0, integer("cass_crisis_cooldownDays", 60));
		resetMin = clamp(integer("cass_crisis_resetMin", VANILLA_RESET_MIN), 0, 400);
		resetMax = clamp(integer("cass_crisis_resetMax", VANILLA_RESET_MAX), 0, 400);
		if (resetMin > resetMax) resetMin = resetMax;
		revealNexus = bool("cass_crisis_revealNexus", true);

		applyStatics();
		log.info("Cass Economy Overhaul settings: colony=" + colonyEnabled + " gateMajor=" + gateMajor
				+ " cooldownDays=" + cooldownDays + " reset=" + resetMin + "-" + resetMax
				+ " revealNexus=" + revealNexus + (lunaEnabled() ? " (LunaLib)" : " (settings.json)"));
	}

	/** Values the game reads from public static fields. Restored to vanilla when the module is off. */
	private static void applyStatics() {
		if (colonyEnabled) {
			HostileActivityEventIntel.RESET_MIN = resetMin;
			HostileActivityEventIntel.RESET_MAX = resetMax;
		} else {
			HostileActivityEventIntel.RESET_MIN = VANILLA_RESET_MIN;
			HostileActivityEventIntel.RESET_MAX = VANILLA_RESET_MAX;
		}
	}

	public static boolean lunaEnabled() {
		return Global.getSettings().getModManager().isModEnabled("lunalib");
	}

	private static boolean bool(String key, boolean def) {
		if (lunaEnabled()) {
			Boolean v = LunaBridge.getBoolean(key);
			if (v != null) return v;
		}
		try {
			return Global.getSettings().getBoolean(key);
		} catch (Throwable t) {
			return def;
		}
	}

	private static int integer(String key, int def) {
		if (lunaEnabled()) {
			Integer v = LunaBridge.getInt(key);
			if (v != null) return v;
		}
		SettingsAPI s = Global.getSettings();
		try {
			return Math.round(s.getFloat(key));
		} catch (Throwable t) {
			return def;
		}
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}

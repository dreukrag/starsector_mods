package dreukrag.casseconomy;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

/**
 * The only class that touches LunaLib. It is loaded only when LunaLib is enabled, so the mod runs without it.
 */
public final class LunaBridge {

	private LunaBridge() {}

	public static Boolean getBoolean(String key) {
		return LunaSettings.getBoolean(CassConfig.MOD_ID, key);
	}

	public static Integer getInt(String key) {
		return LunaSettings.getInt(CassConfig.MOD_ID, key);
	}

	public static Float getFloat(String key) {
		return LunaSettings.getFloat(CassConfig.MOD_ID, key);
	}

	public static void registerListener() {
		if (LunaSettings.hasSettingsListenerOfClass(Listener.class)) return;
		LunaSettings.addSettingsListener(new Listener());
	}

	public static class Listener implements LunaSettingsListener {
		@Override
		public void settingsChanged(String modID) {
			if (CassConfig.MOD_ID.equals(modID)) {
				CassConfig.load();
			}
		}
	}
}

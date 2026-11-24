package dreukrag.maintgantry;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class MaintenanceGantriesModPlugin extends BaseModPlugin {

	public void onGameLoad(boolean newGame) {
		// Transient: re-added on every load, never written into the save.
		Global.getSector().addTransientScript(new MakeshiftGrantScript());
	}
}

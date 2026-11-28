package dreukrag.casseconomy;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import dreukrag.casseconomy.colony.CrisisGateScript;
import dreukrag.casseconomy.colony.NexusRevealScript;

public class CassModPlugin extends BaseModPlugin {

	@Override
	public void onApplicationLoad() {
		CassConfig.load();
		if (CassConfig.lunaEnabled()) {
			LunaBridge.registerListener();
		}
	}

	@Override
	public void onGameLoad(boolean newGame) {
		CassConfig.load();
		SectorAPI sector = Global.getSector();
		// Transient: re-added every load, never written to the save.
		if (!sector.hasTransientScript(CrisisGateScript.class)) {
			sector.addTransientScript(new CrisisGateScript());
		}
		if (!sector.hasTransientScript(NexusRevealScript.class)) {
			sector.addTransientScript(new NexusRevealScript());
		}
	}
}

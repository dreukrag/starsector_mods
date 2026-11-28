package dreukrag.casseconomy.colony;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.RemnantHostileActivityFactor;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import dreukrag.casseconomy.CassConfig;

/**
 * Reveals a Remnant nexus in any system with player colonies (the nexus is what adds Remnant crisis progress):
 * adds a {@link RemnantNexusIntel} entry once, and makes the station detectable from anywhere in the system.
 */
public class NexusRevealScript implements EveryFrameScript {

	public static final Logger log = Global.getLogger(NexusRevealScript.class);
	public static final String KEY_REVEALED = "$cass_nexusRevealed";
	public static final String DETECT_MOD_ID = "cass_nexus_reveal";
	public static final float DETECT_RANGE_BONUS = 100000f;

	private final IntervalUtil interval = new IntervalUtil(0.5f, 1f);

	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}

	public void advance(float amount) {
		if (!CassConfig.colonyEnabled || !CassConfig.revealNexus) return;
		interval.advance(Misc.getDays(amount));
		if (!interval.intervalElapsed()) return;

		for (StarSystemAPI system : Misc.getSystemsWithPlayerColonies(false)) {
			CampaignFleetAPI nexus = RemnantHostileActivityFactor.getRemnantNexus(system);
			if (nexus == null) continue;
			nexus.getDetectedRangeMod().modifyFlat(DETECT_MOD_ID, DETECT_RANGE_BONUS, "Revealed");
			if (nexus.getMemoryWithoutUpdate().getBoolean(KEY_REVEALED)) continue;

			nexus.getMemoryWithoutUpdate().set(KEY_REVEALED, true);
			Global.getSector().getIntelManager().addIntel(new RemnantNexusIntel(nexus, system));
			log.info("Revealed Remnant nexus in " + system.getName());
		}
	}
}

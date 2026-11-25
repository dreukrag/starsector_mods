package dreukrag.maintgantry;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.Misc;

/**
 * Teaches the player Repair and Maintenance Gantries once they have the Makeshift Equipment skill.
 * Checks about once a second of real time while in the campaign. Once learned, the hullmod stays
 * known even if the skill is later removed, like any other learned blueprint.
 */
public class MakeshiftGrantScript implements EveryFrameScript {

	private static final Logger log = Global.getLogger(MakeshiftGrantScript.class);
	private static final float CHECK_INTERVAL = 1f;

	private float elapsed = CHECK_INTERVAL;
	private boolean done = false;

	public boolean isDone() {
		return done;
	}

	public boolean runWhilePaused() {
		return true;
	}

	public void advance(float amount) {
		elapsed += amount;
		if (elapsed < CHECK_INTERVAL) return;
		elapsed = 0f;

		SectorAPI sector = Global.getSector();
		if (sector == null || sector.getPlayerStats() == null) return;
		FactionAPI player = sector.getPlayerFaction();
		if (player.knowsHullMod(MaintenanceGantries.HULLMOD_ID)) {
			done = true;
			return;
		}
		if (sector.getPlayerStats().getSkillLevel(Skills.MAKESHIFT_EQUIPMENT) <= 0) return;

		player.addKnownHullMod(MaintenanceGantries.HULLMOD_ID);
		done = true;
		log.info("Makeshift Equipment: player learned " + MaintenanceGantries.HULLMOD_ID);
		if (sector.getCampaignUI() != null) {
			sector.getCampaignUI().addMessage("Makeshift Equipment: learned Repair and Maintenance Gantries",
					Misc.getHighlightColor());
		}
	}
}

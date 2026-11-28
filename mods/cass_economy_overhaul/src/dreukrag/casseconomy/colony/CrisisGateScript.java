package dreukrag.casseconomy.colony;

import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.HAERandomEventData;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.Stage;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import dreukrag.casseconomy.CassConfig;

/**
 * One major colony crisis at a time.
 *
 * Vanilla picks the next crisis when the Colony Crises bar reaches 500 (cancelled if it drops to 425), and fires it at
 * 600, with no check for crises already running. This script watches that pick: while a major crisis is running (or
 * during the cooldown after one ends) a major-faction pick is cleared and replaced by a pirate / Luddic Path / Remnant
 * pick, using vanilla's weighting. If none of those can strike, the bar is held just below the pick point.
 *
 * Added as a transient script, so nothing of this class goes into the save. State is two sector memory keys.
 */
public class CrisisGateScript implements EveryFrameScript {

	public static final Logger log = Global.getLogger(CrisisGateScript.class);

	/** True while a major crisis was running at the last check. */
	public static final String KEY_WAS_ACTIVE = "$cass_majorCrisisWasActive";
	/** Timestamp of when the last major crisis ended. */
	public static final String KEY_ENDED_AT = "$cass_majorCrisisEndedAt";

	private final IntervalUtil interval = new IntervalUtil(0.1f, 0.2f);

	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}

	public void advance(float amount) {
		if (!CassConfig.colonyEnabled) return;
		interval.advance(Misc.getDays(amount));
		if (!interval.intervalElapsed()) return;

		HostileActivityEventIntel intel = HostileActivityEventIntel.get();
		if (intel == null) return;

		String active = MajorCrises.getActive();
		trackCooldown(active);

		if (!CassConfig.gateMajor) return;
		String blockReason = active != null ? active : (inCooldown() ? "cooldown" : null);
		if (blockReason == null) return;

		EventStageData stage = intel.getDataFor(Stage.HA_EVENT);
		if (stage == null) return;
		if (!(stage.rollData instanceof HAERandomEventData)) return;

		HAERandomEventData data = (HAERandomEventData) stage.rollData;
		if (!MajorCrises.isMajor(data.factor)) return;

		// Clear the major pick without the "crisis averted" notice that resetRandomizedStage() would send.
		stage.rollData = null;
		BaseHostileActivityFactor replacement = rollNonMajor(intel, stage);
		if (stage.rollData != null) {
			log.info("Crisis gate: replaced " + name(data.factor) + " pick with " + name(replacement)
					+ " (blocked by " + blockReason + ")");
			return;
		}

		// Nothing else can strike: hold the bar below the pick point so vanilla doesn't pick again right away.
		int hold = Math.max(0, stage.progressToRollAt - 1);
		if (intel.getProgress() > hold) {
			intel.setProgress(hold);
		}
		log.info("Crisis gate: cleared " + name(data.factor) + " pick, holding bar at " + intel.getProgress()
				+ " (blocked by " + blockReason + ")");
	}

	/** Same weighting as HostileActivityEventIntel.rollRandomizedStage, limited to non-major factions. */
	private BaseHostileActivityFactor rollNonMajor(HostileActivityEventIntel intel, EventStageData stage) {
		float total = 0f;
		for (EventFactor factor : intel.getFactors()) {
			if (factor instanceof BaseHostileActivityFactor) {
				total += factor.getProgress(intel);
			}
		}
		if (total < 1f) total = 1f;

		Random random = intel.getRandom();
		WeightedRandomPicker<BaseHostileActivityFactor> picker = new WeightedRandomPicker<BaseHostileActivityFactor>(random);
		for (EventFactor factor : intel.getFactors()) {
			if (!(factor instanceof BaseHostileActivityFactor)) continue;
			BaseHostileActivityFactor curr = (BaseHostileActivityFactor) factor;
			if (MajorCrises.isMajor(curr)) continue;
			curr.setRandomizedStageSeed(random.nextLong());
			float f = curr.getEventFrequency(intel, stage);
			float w = factor.getProgress(intel) / total;
			if (w > 0) w = 0.1f + 0.9f * w;
			if (f * w > 0) picker.add(curr, f * w);
		}
		BaseHostileActivityFactor pick = picker.pick();
		if (pick != null) pick.rollEvent(intel, stage);
		return pick;
	}

	private void trackCooldown(String active) {
		MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
		if (active != null) {
			mem.set(KEY_WAS_ACTIVE, true);
		} else if (mem.getBoolean(KEY_WAS_ACTIVE)) {
			mem.unset(KEY_WAS_ACTIVE);
			mem.set(KEY_ENDED_AT, Global.getSector().getClock().getTimestamp());
			log.info("Crisis gate: major crisis ended, cooldown " + CassConfig.cooldownDays + " days");
		}
	}

	public static boolean inCooldown() {
		MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
		if (!mem.contains(KEY_ENDED_AT)) return false;
		return getCooldownDaysLeft() > 0;
	}

	public static float getCooldownDaysLeft() {
		MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
		if (!mem.contains(KEY_ENDED_AT)) return 0f;
		float elapsed = Global.getSector().getClock().getElapsedDaysSince(mem.getLong(KEY_ENDED_AT));
		return Math.max(0f, CassConfig.cooldownDays - elapsed);
	}

	private static String name(Object factor) {
		return factor == null ? "nothing" : factor.getClass().getSimpleName();
	}

	/** For status output: name of the crisis the bar has picked, or null. */
	public static String getPickedFactorName() {
		HostileActivityEventIntel intel = HostileActivityEventIntel.get();
		if (intel == null) return null;
		EventStageData stage = intel.getDataFor(Stage.HA_EVENT);
		if (stage == null || !(stage.rollData instanceof HAERandomEventData)) return null;
		return name(((HAERandomEventData) stage.rollData).factor);
	}
}

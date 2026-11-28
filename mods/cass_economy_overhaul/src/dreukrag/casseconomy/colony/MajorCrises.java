package dreukrag.casseconomy.colony;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.events.HegemonyHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.LuddicChurchHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.PerseanLeagueHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.SindrianDiktatHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.TriTachyonHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.group.KnightsOfLuddTakeoverExpedition;
import com.fs.starfarer.api.impl.campaign.intel.group.PerseanLeagueBlockade;
import com.fs.starfarer.api.impl.campaign.intel.group.PerseanLeaguePunitiveExpedition;
import com.fs.starfarer.api.impl.campaign.intel.group.SindrianDiktatPunitiveExpedition;
import com.fs.starfarer.api.impl.campaign.intel.group.TTMercenaryAttack;
import com.fs.starfarer.api.impl.campaign.intel.inspection.HegemonyInspectionIntel;

/**
 * Which colony-crisis factions count as "major", and whether one of their crises is running.
 * Pirates, the Luddic Path and the Remnant are not major.
 */
public final class MajorCrises {

	private MajorCrises() {}

	public static boolean isMajor(HostileActivityFactor factor) {
		return factor instanceof HegemonyHostileActivityFactor
				|| factor instanceof TriTachyonHostileActivityFactor
				|| factor instanceof PerseanLeagueHostileActivityFactor
				|| factor instanceof SindrianDiktatHostileActivityFactor
				|| factor instanceof LuddicChurchHostileActivityFactor;
	}

	/** Name of the running major crisis, or null if none is running. */
	public static String getActive() {
		if (PerseanLeagueBlockade.get() != null) return "Persean League blockade";
		if (PerseanLeaguePunitiveExpedition.get() != null) return "Persean League punitive expedition";
		if (SindrianDiktatPunitiveExpedition.get() != null) return "Sindrian Diktat punitive expedition";
		if (KnightsOfLuddTakeoverExpedition.get() != null) return "Knights of Ludd takeover";
		if (TTMercenaryAttack.get() != null) return "Tri-Tachyon mercenary attack";
		// The Hegemony inspection keeps no global reference, so look for one in the intel list.
		for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(HegemonyInspectionIntel.class)) {
			if (!intel.isEnding() && !intel.isEnded()) return "Hegemony AI inspection";
		}
		return null;
	}
}

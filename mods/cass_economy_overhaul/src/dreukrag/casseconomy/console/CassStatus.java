package dreukrag.casseconomy.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.Stage;

import dreukrag.casseconomy.CassConfig;
import dreukrag.casseconomy.colony.CrisisGateScript;
import dreukrag.casseconomy.colony.MajorCrises;

/** Console Commands: "cass_status". Only loaded when Console Commands is enabled. */
public class CassStatus implements BaseCommand {

	@Override
	public CommandResult runCommand(String args, CommandContext context) {
		if (!context.isInCampaign()) {
			Console.showMessage("Campaign only.");
			return CommandResult.WRONG_CONTEXT;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Cass Economy Overhaul (").append(CassConfig.lunaEnabled() ? "LunaLib" : "settings.json").append(")\n");
		sb.append("Colony module: ").append(CassConfig.colonyEnabled ? "on" : "off")
				.append(", one major crisis at a time: ").append(CassConfig.gateMajor ? "on" : "off")
				.append(", reveal nexus: ").append(CassConfig.revealNexus ? "on" : "off").append("\n");

		String active = MajorCrises.getActive();
		sb.append("Running major crisis: ").append(active != null ? active : "none").append("\n");
		sb.append("Cooldown left: ").append(Math.round(CrisisGateScript.getCooldownDaysLeft()))
				.append(" of ").append(CassConfig.cooldownDays).append(" days\n");

		HostileActivityEventIntel intel = HostileActivityEventIntel.get();
		if (intel != null) {
			EventStageData stage = intel.getDataFor(Stage.HA_EVENT);
			sb.append("Crisis bar: ").append(intel.getProgress()).append(" / ").append(intel.getMaxProgress())
					.append(", +").append(intel.getMonthlyProgress()).append(" per month");
			if (stage != null) sb.append(", pick at ").append(stage.progressToRollAt);
			sb.append("\n");
			String picked = CrisisGateScript.getPickedFactorName();
			sb.append("Picked crisis: ").append(picked != null ? picked : "none yet").append("\n");
		} else {
			sb.append("Colony crises event not started yet.\n");
		}
		sb.append("Bar reset after a crisis: ").append(HostileActivityEventIntel.RESET_MIN).append("-")
				.append(HostileActivityEventIntel.RESET_MAX);
		Console.showMessage(sb.toString());
		return CommandResult.SUCCESS;
	}
}

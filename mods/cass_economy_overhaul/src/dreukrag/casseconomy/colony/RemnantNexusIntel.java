package dreukrag.casseconomy.colony;

import java.awt.Color;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/** Intel entry pointing at a Remnant nexus in a system with player colonies. Ends when the nexus is gone. */
public class RemnantNexusIntel extends BaseIntelPlugin {

	protected CampaignFleetAPI nexus;
	protected StarSystemAPI system;

	public RemnantNexusIntel(CampaignFleetAPI nexus, StarSystemAPI system) {
		this.nexus = nexus;
		this.system = system;
	}

	public boolean isNexusGone() {
		return nexus == null || !nexus.isAlive() || nexus.getContainingLocation() != system;
	}

	@Override
	protected void advanceImpl(float amount) {
		if (!isEnding() && !isEnded() && isNexusGone()) {
			endAfterDelay();
			sendUpdateIfPlayerHasIntel(null, false);
		}
	}

	@Override
	protected String getName() {
		String name = "Remnant Nexus - " + system.getNameWithNoType();
		if (isEnding() || isEnded()) name += " - Destroyed";
		return name;
	}

	@Override
	public FactionAPI getFactionForUIColors() {
		return Global.getSector().getFaction(Factions.REMNANTS);
	}

	@Override
	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
		info.addPara("Located in the " + system.getNameWithLowercaseTypeShort(), initPad, tc,
				Misc.getHighlightColor(), system.getNameWithLowercaseTypeShort());
	}

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		info.addImage(getFactionForUIColors().getLogo(), width, 128, opad);
		if (isEnding() || isEnded()) {
			info.addPara("The Remnant nexus in the " + system.getNameWithLowercaseTypeShort()
					+ " is no longer a threat to your colonies.", opad);
			return;
		}
		info.addPara("A Remnant nexus in the " + system.getNameWithLowercaseTypeShort()
				+ " threatens your colonies there. Its fleets raid local shipping and it adds to colony crisis "
				+ "progress until it is destroyed.", opad, Misc.getNegativeHighlightColor(), "destroyed");
	}

	@Override
	public String getIcon() {
		return getFactionForUIColors().getCrest();
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_COLONIES);
		tags.add(getFactionForUIColors().getId());
		return tags;
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		if (!isNexusGone()) return nexus;
		return system.getCenter();
	}

	@Override
	public String getSmallDescriptionTitle() {
		return getName();
	}
}

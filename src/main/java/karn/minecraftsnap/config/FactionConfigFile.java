package karn.minecraftsnap.config;

import java.util.ArrayList;
import java.util.List;

public class FactionConfigFile {
	public String displayName = "";
	public List<String> summaryLines = new ArrayList<>();
	public CaptainSkillDisplayConfig captainSkill = new CaptainSkillDisplayConfig();
	public List<FactionUnitEntry> units = new ArrayList<>();

	public void normalize() {
		if (summaryLines == null) {
			summaryLines = new ArrayList<>();
		}
		if (captainSkill == null) {
			captainSkill = new CaptainSkillDisplayConfig();
		}
		captainSkill.normalize();
		if (units == null) {
			units = new ArrayList<>();
			return;
		}
		units.forEach(FactionUnitEntry::normalize);
	}
}

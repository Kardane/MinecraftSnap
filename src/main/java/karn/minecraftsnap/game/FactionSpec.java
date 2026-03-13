package karn.minecraftsnap.game;

import java.util.List;

public record FactionSpec(
	String displayName,
	List<String> summaryLines,
	String captainSkillName,
	List<String> captainSkillDescriptionLines
) {
}

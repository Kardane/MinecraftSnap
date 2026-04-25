package karn.minecraftsnap.config;

import java.util.ArrayList;
import java.util.List;

public class BiomeEntry {
	public String id = "";
	public String displayName = "";
	public String displayItemId = "";
	public String type = "";
	public Integer lane1Weight = 0;
	public Integer lane2Weight = 0;
	public Integer lane3Weight = 0;
	public List<String> descriptionLines = new ArrayList<>();
	public String minecraftBiomeId = "minecraft:plains";
	public String effectType = "noop";
	public String structureId = "";
	public List<String> revealMessages = new ArrayList<>();
	public String revealSoundId = "minecraft:block.note_block.pling";

	public void normalize() {
		if (descriptionLines == null) {
			descriptionLines = new ArrayList<>();
		}
		if (revealMessages == null) {
			revealMessages = new ArrayList<>();
		}
		if (displayItemId == null) {
			displayItemId = "";
		}
		type = normalizeType(type);
		lane1Weight = normalizeWeight(lane1Weight);
		lane2Weight = normalizeWeight(lane2Weight);
		lane3Weight = normalizeWeight(lane3Weight);
		if (minecraftBiomeId == null || minecraftBiomeId.isBlank()) {
			minecraftBiomeId = "minecraft:plains";
		}
		if (effectType == null || effectType.isBlank()) {
			effectType = "noop";
		}
		if (structureId == null) {
			structureId = "";
		}
		if (revealSoundId == null || revealSoundId.isBlank()) {
			revealSoundId = "minecraft:block.note_block.pling";
		}
	}

	private static String normalizeType(String type) {
		if (type == null || type.isBlank()) {
			return "neutral";
		}
		var normalized = type.toLowerCase(java.util.Locale.ROOT);
		return switch (normalized) {
			case "neutral", "special", "structure" -> normalized;
			default -> "neutral";
		};
	}

	private static int normalizeWeight(Integer weight) {
		return weight == null ? 0 : Math.max(0, weight);
	}
}

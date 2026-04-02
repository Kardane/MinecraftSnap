package karn.minecraftsnap.config;

import java.util.ArrayList;
import java.util.List;

public class BiomeEntry {
	public String id = "";
	public String displayName = "";
	public String displayItemId = "";
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
}

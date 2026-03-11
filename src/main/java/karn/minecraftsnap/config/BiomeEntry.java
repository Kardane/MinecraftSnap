package karn.minecraftsnap.config;

import java.util.ArrayList;
import java.util.List;

public class BiomeEntry {
	public String id = "";
	public String displayName = "";
	public List<String> descriptionLines = new ArrayList<>();
	public String minecraftBiomeId = "minecraft:plains";
	public String effectType = "noop";
	public String structureId = "";
	public int structureOffsetX;
	public int structureOffsetY;
	public int structureOffsetZ;
	public List<String> revealMessages = new ArrayList<>();
	public String revealSoundId = "minecraft:block.note_block.pling";
	public int pulseIntervalSeconds;
	public List<String> pulseMessages = new ArrayList<>();
	public String pulseSoundId = "minecraft:block.note_block.pling";

	public void normalize() {
		if (descriptionLines == null) {
			descriptionLines = new ArrayList<>();
		}
		if (revealMessages == null) {
			revealMessages = new ArrayList<>();
		}
		if (pulseMessages == null) {
			pulseMessages = new ArrayList<>();
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
		if (pulseSoundId == null || pulseSoundId.isBlank()) {
			pulseSoundId = "minecraft:block.note_block.pling";
		}
	}
}

package karn.minecraftsnap.config;

import java.util.ArrayList;
import java.util.List;

public class AdvanceOptionEntry {
	public String resultUnitId = "";
	public String displayName = "";
	public List<String> descriptionLines = new ArrayList<>();
	public List<String> biomes = new ArrayList<>();
	public List<String> weathers = new ArrayList<>();
	public int requiredTicks = 1;

	public void normalize() {
		if (resultUnitId == null) {
			resultUnitId = "";
		}
		if (displayName == null) {
			displayName = "";
		}
		if (descriptionLines == null) {
			descriptionLines = new ArrayList<>();
		}
		if (biomes == null) {
			biomes = new ArrayList<>();
		}
		if (weathers == null) {
			weathers = new ArrayList<>();
		}
		if (requiredTicks <= 0) {
			requiredTicks = 1;
		}
	}
}

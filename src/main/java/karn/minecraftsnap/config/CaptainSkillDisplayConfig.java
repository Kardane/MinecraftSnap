package karn.minecraftsnap.config;

import java.util.ArrayList;
import java.util.List;

public class CaptainSkillDisplayConfig {
	public String name = "";
	public List<String> descriptionLines = new ArrayList<>();

	public void normalize() {
		if (descriptionLines == null) {
			descriptionLines = new ArrayList<>();
		}
	}
}

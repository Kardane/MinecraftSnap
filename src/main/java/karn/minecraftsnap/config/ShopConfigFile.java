package karn.minecraftsnap.config;

import java.util.ArrayList;
import java.util.List;

public class ShopConfigFile {
	public List<ShopEntry> entries = new ArrayList<>();

	public void normalize() {
		if (entries == null) {
			entries = new ArrayList<>();
			return;
		}
		entries.forEach(ShopEntry::normalize);
	}
}

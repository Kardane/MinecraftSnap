package karn.minecraftsnap.config;

import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class UnitItemEntry {
	public String itemId = "";
	public String displayName = "";
	public List<String> loreLines = new ArrayList<>();

	public static UnitItemEntry create(String itemId) {
		var entry = new UnitItemEntry();
		entry.itemId = itemId;
		return entry;
	}

	public void normalize() {
		if (itemId == null) {
			itemId = "";
		}
		if (displayName == null) {
			displayName = "";
		}
		if (loreLines == null) {
			loreLines = new ArrayList<>();
		}
	}

	public Item resolve(Function<String, Item> itemResolver) {
		if (itemResolver == null) {
			return null;
		}
		return itemResolver.apply(itemId);
	}
}

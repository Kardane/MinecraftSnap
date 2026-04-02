package karn.minecraftsnap.config;

import java.util.ArrayList;
import java.util.List;

public class ShopEntry {
	public String id = "";
	public String type = "item";
	public int price = 1;
	public UnitItemEntry item = new UnitItemEntry();
	public String enchantmentId = "";
	public String target = "";
	public int maxLevel = 1;
	public List<Integer> prices = new ArrayList<>();

	public void normalize() {
		if (id == null) {
			id = "";
		}
		if (type == null || type.isBlank()) {
			type = "item";
		}
		type = type.toLowerCase(java.util.Locale.ROOT);
		if (price <= 0) {
			price = 1;
		}
		if (item == null) {
			item = new UnitItemEntry();
		}
		item.normalize();
		if (enchantmentId == null) {
			enchantmentId = "";
		}
		if (target == null) {
			target = "";
		}
		if (maxLevel <= 0) {
			maxLevel = 1;
		}
		if (prices == null) {
			prices = new ArrayList<>();
		}
		prices = prices.stream()
			.map(levelPrice -> levelPrice == null || levelPrice <= 0 ? 1 : levelPrice)
			.toList();
		if (prices.isEmpty()) {
			prices = List.of(price);
		}
		if ("enchant".equals(type)) {
			maxLevel = Math.max(maxLevel, prices.size());
		}
	}
}

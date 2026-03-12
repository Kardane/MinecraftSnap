package karn.minecraftsnap.config;

public class ShopEntry {
	public String id = "";
	public int price = 1;
	public UnitItemEntry item = new UnitItemEntry();

	public void normalize() {
		if (id == null) {
			id = "";
		}
		if (price <= 0) {
			price = 1;
		}
		if (item == null) {
			item = new UnitItemEntry();
		}
		item.normalize();
	}
}

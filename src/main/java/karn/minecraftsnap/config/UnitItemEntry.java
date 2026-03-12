package karn.minecraftsnap.config;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class UnitItemEntry {
	public String itemId = "";
	public int count = 1;
	public String displayName = "";
	public List<String> loreLines = new ArrayList<>();
	public String componentsNbt = "";
	public String stackNbt = "";

	public static UnitItemEntry create(String itemId) {
		var entry = new UnitItemEntry();
		entry.itemId = itemId;
		return entry;
	}

	public void normalize() {
		if (itemId == null) {
			itemId = "";
		}
		if (count <= 0) {
			count = 1;
		}
		if (displayName == null) {
			displayName = "";
		}
		if (loreLines == null) {
			loreLines = new ArrayList<>();
		}
		if (componentsNbt == null) {
			componentsNbt = "";
		}
		if (stackNbt == null) {
			stackNbt = "";
		}
	}

	public boolean isEmpty() {
		return stackNbt.isBlank() && (itemId.isBlank() || "minecraft:air".equals(itemId));
	}

	public Item resolve() {
		var resolvedItemId = itemId;
		if ((resolvedItemId == null || resolvedItemId.isBlank()) && stackNbt != null && !stackNbt.isBlank()) {
			try {
				resolvedItemId = StringNbtReader.readCompound(stackNbt).getString("id").orElse("");
			} catch (Exception ignored) {
				resolvedItemId = itemId;
			}
		}
		if (resolvedItemId == null || resolvedItemId.isBlank()) {
			return null;
		}
		try {
			var item = Registries.ITEM.get(Identifier.of(resolvedItemId));
			return item == Items.AIR ? null : item;
		} catch (Exception ignored) {
			return null;
		}
	}

	public Item resolve(Function<String, Item> itemResolver) {
		if (itemResolver != null) {
			var resolvedItemId = itemId;
			if ((resolvedItemId == null || resolvedItemId.isBlank()) && stackNbt != null && !stackNbt.isBlank()) {
				try {
					resolvedItemId = StringNbtReader.readCompound(stackNbt).getString("id").orElse("");
				} catch (Exception ignored) {
					resolvedItemId = itemId;
				}
			}
			return itemResolver.apply(resolvedItemId);
		}
		return resolve();
	}
}

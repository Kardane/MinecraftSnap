package karn.minecraftsnap.config;

public class EntitySpecEntry {
	public String entityId = "";
	public String entityNbt = "";

	public static EntitySpecEntry create(String entityId) {
		var entry = new EntitySpecEntry();
		entry.entityId = entityId;
		return entry;
	}

	public void normalize() {
		if (entityId == null) {
			entityId = "";
		}
		if (entityNbt == null) {
			entityNbt = "";
		}
	}

	public boolean isEmpty() {
		return entityId.isBlank();
	}
}

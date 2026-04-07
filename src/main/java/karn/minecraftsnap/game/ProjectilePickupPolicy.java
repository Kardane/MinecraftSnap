package karn.minecraftsnap.game;

public final class ProjectilePickupPolicy {
	private ProjectilePickupPolicy() {
	}

	public static boolean shouldDisallowGroundArrowPickup(boolean inGround) {
		return inGround;
	}
}

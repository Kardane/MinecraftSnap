package karn.minecraftsnap.ui;

import net.minecraft.text.Text;

public interface PlayerDisplayNameHolder {
	Text minecraftsnap$getStyledDisplayName();

	void minecraftsnap$setStyledDisplayName(Text displayName);

	Text minecraftsnap$getPlayerListDisplayName();

	void minecraftsnap$setPlayerListDisplayName(Text displayName);
}

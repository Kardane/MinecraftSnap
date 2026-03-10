package karn.minecraftsnap.game;

import java.util.UUID;

public record UnitInstanceState(
	UUID playerId,
	UUID captainId,
	TeamId teamId,
	String unitId
) {
}

package karn.minecraftsnap.ui;

import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PlayerDisplayNameService {
	public void sync(MinecraftServer server, MatchManager matchManager, karn.minecraftsnap.config.StatsRepository statsRepository, SystemConfig systemConfig) {
		sync(server, matchManager, statsRepository, systemConfig, false);
	}

	public void refreshAll(MinecraftServer server, MatchManager matchManager, karn.minecraftsnap.config.StatsRepository statsRepository, SystemConfig systemConfig) {
		sync(server, matchManager, statsRepository, systemConfig, true);
	}

	private void sync(MinecraftServer server, MatchManager matchManager, karn.minecraftsnap.config.StatsRepository statsRepository, SystemConfig systemConfig, boolean force) {
		if (server == null) {
			return;
		}

		for (var player : server.getPlayerManager().getPlayerList()) {
			var holder = player instanceof PlayerDisplayNameHolder access ? access : null;
			if (holder == null) {
				continue;
			}

			var state = matchManager.getPlayerState(player.getUuid());
			var ladder = statsRepository.getLadder(player.getUuid(), player.getName().getString());
			Text displayName = useStyledDisplayName(matchManager.getPhase())
				? buildDisplayName(player.getName().copy(), ladder, state.getRoleType(), state.getTeamId(), matchManager.getPhase(), systemConfig.display)
				: null;
			Text playerListName = buildDisplayName(player.getName().copy(), ladder, state.getRoleType(), state.getTeamId(), matchManager.getPhase(), systemConfig.display);

			boolean changed = !textEquals(holder.minecraftsnap$getStyledDisplayName(), displayName)
				|| !textEquals(holder.minecraftsnap$getPlayerListDisplayName(), playerListName);
			if (!changed && !force) {
				continue;
			}

			holder.minecraftsnap$setStyledDisplayName(displayName);
			holder.minecraftsnap$setPlayerListDisplayName(playerListName);
			broadcastPlayerListName(server, player);
		}
	}

	private void broadcastPlayerListName(MinecraftServer server, ServerPlayerEntity player) {
		var packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player);
		server.getPlayerManager().sendToAll(packet);
	}

	static Text buildDisplayName(ServerPlayerEntity player, int ladder, RoleType roleType) {
		return buildDisplayName(player.getName().copy(), ladder, roleType, MatchPhase.GAME_RUNNING, new SystemConfig.DisplayConfig());
	}

	static Text buildDisplayName(Text playerName, int ladder, RoleType roleType) {
		return buildDisplayName(playerName, ladder, roleType, MatchPhase.GAME_RUNNING, new SystemConfig.DisplayConfig());
	}

	static boolean useStyledDisplayName(MatchPhase phase) {
		return phase != MatchPhase.LOBBY;
	}

	static Text buildDisplayName(Text playerName, int ladder, RoleType roleType, MatchPhase phase) {
		return buildDisplayName(playerName, ladder, roleType, phase, new SystemConfig.DisplayConfig());
	}

	static Text buildDisplayName(Text playerName, int ladder, RoleType roleType, MatchPhase phase, SystemConfig.DisplayConfig displayConfig) {
		return buildDisplayName(playerName, ladder, roleType, null, phase, displayConfig);
	}

	static Text buildDisplayName(Text playerName, int ladder, RoleType roleType, TeamId teamId, MatchPhase phase, SystemConfig.DisplayConfig displayConfig) {
		return Text.empty()
			.append(Text.literal(displayConfig.ladderPrefixFormat.replace("{ladder}", Integer.toString(ladder))).formatted(Formatting.GRAY))
			.append(phase != MatchPhase.LOBBY && roleType == RoleType.CAPTAIN ? Text.literal(displayConfig.captainStar).formatted(Formatting.YELLOW) : Text.empty())
			.append(teamColor(phase, teamId) == null ? playerName.copy() : playerName.copy().formatted(teamColor(phase, teamId)));
	}

	private static Formatting teamColor(MatchPhase phase, TeamId teamId) {
		if (phase == MatchPhase.LOBBY || teamId == null) {
			return null;
		}
		return teamId == TeamId.RED ? Formatting.RED : Formatting.BLUE;
	}

	private boolean textEquals(Text left, Text right) {
		return java.util.Objects.equals(left, right);
	}
}

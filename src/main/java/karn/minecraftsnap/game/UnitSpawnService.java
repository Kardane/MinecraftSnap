package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

import java.util.List;
import java.util.UUID;

public class UnitSpawnService {
	private final CaptainManaService captainManaService;
	private final UnitRegistry unitRegistry;
	private final UnitLoadoutService unitLoadoutService;
	private final UnitAbilityService unitAbilityService;

	public UnitSpawnService() {
		this(new CaptainManaService(), null, new UnitLoadoutService(), new UnitAbilityService());
	}

	public UnitSpawnService(
		CaptainManaService captainManaService,
		UnitRegistry unitRegistry,
		UnitLoadoutService unitLoadoutService,
		UnitAbilityService unitAbilityService
	) {
		this.captainManaService = captainManaService;
		this.unitRegistry = unitRegistry;
		this.unitLoadoutService = unitLoadoutService;
		this.unitAbilityService = unitAbilityService;
	}

	public SpawnCandidate selectSpawnCandidate(String unitId, List<SpawnCandidate> candidates) {
		for (var candidate : candidates) {
			if (candidate.spectator() && unitId.equals(candidate.preferredUnitId())) {
				return candidate;
			}
		}
		for (var candidate : candidates) {
			if (candidate.spectator()) {
				return candidate;
			}
		}
		return null;
	}

	public SpawnResult spawnSelectedUnit(
		ServerPlayerEntity captain,
		String unitId,
		MatchManager matchManager,
		SystemConfig systemConfig,
		TextTemplateResolver textTemplateResolver
	) {
		var captainState = matchManager.getPlayerState(captain.getUuid());
		if (!captainState.isCaptain() || captainState.getTeamId() == null) {
			return SpawnResult.error("&c사령관만 소환 가능");
		}

		var definition = unitRegistry.get(unitId);
		if (definition == null) {
			return SpawnResult.error("&c알 수 없는 유닛");
		}
		if (captainState.getFactionId() != definition.factionId()) {
			return SpawnResult.error("&c현재 팩션에서 소환 불가");
		}

		var candidates = matchManager.getOnlineSpectatorUnits(captainState.getTeamId()).stream()
			.map(player -> {
				var state = matchManager.getPlayerState(player.getUuid());
				return new SpawnCandidate(player.getUuid(), state.getPreferredUnitId(), player.isSpectator());
			})
			.toList();
		var candidate = selectSpawnCandidate(unitId, candidates);
		if (candidate == null) {
			return SpawnResult.error("&c소환 가능한 관전자 유닛 없음");
		}
		if (!captainManaService.trySpendForSpawn(captain.getUuid(), definition.cost(), definition.spawnCooldownSeconds())) {
			return SpawnResult.error("&c마나 또는 생성 쿨다운 부족");
		}

		var target = captain.getServer().getPlayerManager().getPlayer(candidate.playerId());
		if (target == null) {
			return SpawnResult.error("&c대상 유닛 플레이어를 찾지 못함");
		}

		unitAbilityService.clearPlayerState(target.getUuid());
		matchManager.setCurrentUnit(target.getUuid(), definition.id());
		target.changeGameMode(GameMode.SURVIVAL);
		teleport(target, systemConfig.gameStart.unitSpawn);
		unitLoadoutService.applyUnitLoadout(target, definition, textTemplateResolver);
		DisguiseSupport.applyDisguise(target, definition.disguiseId());
		target.sendMessage(textTemplateResolver.format("&a소환됨: &f" + definition.displayName()), false);
		captain.sendMessage(textTemplateResolver.format("&a유닛 소환 완료: &f" + target.getName().getString() + " &7-> &f" + definition.displayName()), false);
		return SpawnResult.success(target.getUuid(), definition.id());
	}

	public void maintainActiveUnits(MatchManager matchManager) {
		for (var player : matchManager.getOnlinePlayers()) {
			var state = matchManager.getPlayerState(player.getUuid());
			var unitId = state.getCurrentUnitId();
			if (unitId == null) {
				continue;
			}
			var definition = unitRegistry.get(unitId);
			if (definition == null) {
				continue;
			}
			unitLoadoutService.maintainLoadout(player, definition);
		}
	}

	public void resetPlayer(ServerPlayerEntity player) {
		unitAbilityService.clearPlayerState(player.getUuid());
		DisguiseSupport.clearDisguise(player);
		unitLoadoutService.resetCombatState(player);
	}

	public CaptainManaService getCaptainManaService() {
		return captainManaService;
	}

	public UnitRegistry getUnitRegistry() {
		return unitRegistry;
	}

	public UnitLoadoutService getUnitLoadoutService() {
		return unitLoadoutService;
	}

	public UnitAbilityService getUnitAbilityService() {
		return unitAbilityService;
	}

	private void teleport(ServerPlayerEntity player, SystemConfig.PositionConfig position) {
		var world = resolveWorld(player, position.world);
		player.teleportTo(new TeleportTarget(
			world,
			new Vec3d(position.x, position.y, position.z),
			Vec3d.ZERO,
			position.yaw,
			position.pitch,
			TeleportTarget.NO_OP
		));
	}

	private ServerWorld resolveWorld(ServerPlayerEntity player, String worldId) {
		try {
			var key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
			var world = player.getServer().getWorld(key);
			return world != null ? world : player.getServer().getOverworld();
		} catch (Exception ignored) {
			return player.getServer().getOverworld();
		}
	}

	public record SpawnCandidate(UUID playerId, String preferredUnitId, boolean spectator) {
	}

	public record SpawnResult(boolean success, UUID targetPlayerId, String unitId, String message) {
		public static SpawnResult success(UUID targetPlayerId, String unitId) {
			return new SpawnResult(true, targetPlayerId, unitId, null);
		}

		public static SpawnResult error(String message) {
			return new SpawnResult(false, null, null, message);
		}
	}
}

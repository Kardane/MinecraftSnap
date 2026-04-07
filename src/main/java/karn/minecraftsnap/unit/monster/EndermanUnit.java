package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;

public class EndermanUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	private static final int TELEPORT_DUST_COLOR = 0xB04CFF;
	private static final float TELEPORT_DUST_SIZE = 1.4F;

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), context.unitDefinition().id(), weaponAttackDamage(), weaponAttackSpeed());
	}

	@Override
	public void onSkillUse(UnitContext context) {
		var cooldownTicks = skillCooldownTicksForBiome(context.currentBiomeId(), context.unitDefinition().abilityCooldownSeconds());
		context.activateSkill(cooldownTicks, () -> {
			var laneRuntime = context.laneRuntime();
			var state = context.state();
			var config = context.systemConfig();
			var player = context.player();
			if (laneRuntime == null || state == null || state.getTeamId() == null || config == null || player == null) {
				return false;
			}
			var targetLane = nextLane(laneRuntime.laneId());
			var spawn = targetSpawn(config, state.getTeamId(), laneRuntime.laneId());
			var world = context.world();
			if (spawn == null || world == null) {
				return false;
			}
			var from = player.getPos();
			var targetPos = new Vec3d(spawn.x, spawn.y, spawn.z);
			spawnPortalBurst(world, from);
			spawnLineParticles(world, from, targetPos);
			playTeleportSound(world, teleportSoundPosition(from));
			player.teleportTo(new TeleportTarget(world, targetPos, Vec3d.ZERO, spawn.yaw, spawn.pitch, TeleportTarget.NO_OP));
			spawnPortalBurst(world, targetPos);
			playTeleportSound(world, teleportSoundPosition(targetPos));
			player.sendMessage(context.format("&5" + laneLabel(targetLane) + " 스폰 위치로 이동했습니다."), true);
			return true;
		});
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		if (shouldTakeWaterDamage(context.player().isTouchingWater(), context.serverTicks())) {
			context.player().damage(context.world(), context.player().getDamageSources().generic(), waterDamageAmount());
		}
	}

	static LaneId nextLane(LaneId currentLane) {
		return switch (currentLane) {
			case LANE_1 -> LaneId.LANE_2;
			case LANE_2 -> LaneId.LANE_3;
			case LANE_3 -> LaneId.LANE_1;
		};
	}

	static SystemConfig.PositionConfig targetSpawn(SystemConfig config, TeamId teamId, LaneId currentLane) {
		if (config == null || teamId == null || currentLane == null) {
			return null;
		}
		var spawn = config.gameStart.unitSpawnFor(teamId, nextLane(currentLane));
		if (spawn == null || spawn.y < 0.0D) {
			return config.gameStart.captainSpawnFor(teamId);
		}
		return spawn;
	}

	static int pathSampleCount(Vec3d from, Vec3d to) {
		if (from == null || to == null) {
			return 1;
		}
		return Math.max(1, (int) Math.ceil(to.subtract(from).length() / 0.6D));
	}

	double weaponAttackDamage() {
		return 7.0D;
	}

	double weaponAttackSpeed() {
		return 2.0D;
	}

	long skillCooldownTicksForBiome(String biomeId, int defaultCooldownSeconds) {
		if (isEndBiome(biomeId)) {
			return 20L;
		}
		return defaultCooldownSeconds * 20L;
	}

	boolean isEndBiome(String biomeId) {
		return biomeId != null && biomeId.contains("end");
	}

	boolean shouldTakeWaterDamage(boolean touchingWater, long serverTicks) {
		return touchingWater && serverTicks > 0L && serverTicks % 20L == 0L;
	}

	float waterDamageAmount() {
		return 2.0F;
	}

	BlockPos teleportSoundPosition(Vec3d position) {
		return position == null ? BlockPos.ORIGIN : BlockPos.ofFloored(position);
	}

	private void spawnLineParticles(net.minecraft.server.world.ServerWorld world, Vec3d from, Vec3d to) {
		if (world == null || from == null || to == null) {
			return;
		}
		var delta = to.subtract(from);
		var steps = pathSampleCount(from, to);
		var effect = new DustParticleEffect(TELEPORT_DUST_COLOR, TELEPORT_DUST_SIZE);
		for (int i = 0; i <= steps; i++) {
			var pos = from.add(delta.multiply(i / (double) steps));
			for (var viewer : world.getPlayers()) {
				world.spawnParticles(viewer, effect, true, false, pos.x, pos.y + 1.0D, pos.z, 3, 0.03D, 0.03D, 0.03D, 0.0D);
			}
		}
	}

	private void spawnPortalBurst(net.minecraft.server.world.ServerWorld world, Vec3d position) {
		if (world == null || position == null) {
			return;
		}
		for (var viewer : world.getPlayers()) {
			world.spawnParticles(viewer, ParticleTypes.PORTAL, true, false, position.x, position.y + 1.0D, position.z, 120, 1.2D, 1.0D, 1.2D, 0.1D);
		}
	}

	private void playTeleportSound(net.minecraft.server.world.ServerWorld world, BlockPos position) {
		if (world == null || position == null) {
			return;
		}
		world.playSound(null, position, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번 라인";
			case LANE_2 -> "2번 라인";
			case LANE_3 -> "3번 라인";
		};
	}
}

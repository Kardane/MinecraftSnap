package karn.minecraftsnap.game;

import karn.minecraftsnap.biome.BiomeRuntimeContext;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UnitAbilityService {
	private static final int CAPTAIN_SKILL_COOLDOWN_SECONDS = 30;

	private final TextTemplateResolver textTemplateResolver;
	private final LaneRuntimeRegistry laneRuntimeRegistry;
	private final Map<UUID, Long> unitCooldownTicks = new HashMap<>();
	private final Map<UUID, Long> pendingCreeperBombTicks = new HashMap<>();

	public UnitAbilityService() {
		this(new TextTemplateResolver(), null);
	}

	public UnitAbilityService(TextTemplateResolver textTemplateResolver) {
		this(textTemplateResolver, null);
	}

	public UnitAbilityService(TextTemplateResolver textTemplateResolver, LaneRuntimeRegistry laneRuntimeRegistry) {
		this.textTemplateResolver = textTemplateResolver;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
	}

	public void tick(MinecraftServer server, MatchManager matchManager) {
		if (pendingCreeperBombTicks.isEmpty()) {
			return;
		}

		var completed = new java.util.ArrayList<UUID>();
		for (var entry : pendingCreeperBombTicks.entrySet()) {
			if (matchManager.getServerTicks() < entry.getValue()) {
				continue;
			}

			var player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player != null && matchManager.getPlayerState(player.getUuid()).getCurrentUnitId() != null) {
				explodeCreeper(player, matchManager);
			}
			completed.add(entry.getKey());
		}
		completed.forEach(pendingCreeperBombTicks::remove);
	}

	public boolean useCaptainSkill(ServerPlayerEntity captain, MatchManager matchManager, CaptainManaService captainManaService) {
		var state = matchManager.getPlayerState(captain.getUuid());
		if (!state.isCaptain() || state.getFactionId() == null) {
			return false;
		}

		var captainState = captainManaService.getOrCreate(captain.getUuid());
		if (captainState.getSkillCooldownSeconds() > 0) {
			captain.sendMessage(textTemplateResolver.format("&c사령관 스킬 쿨다운: &f" + captainState.getSkillCooldownSeconds() + "초"), true);
			return false;
		}

		switch (state.getFactionId()) {
			case VILLAGER -> captain.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 8, 1));
			case MONSTER -> captain.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 8, 0));
			case NETHER -> {
				captain.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 8, 0));
				captain.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 8, 0));
			}
		}

		captainManaService.triggerSkillCooldown(captain.getUuid(), CAPTAIN_SKILL_COOLDOWN_SECONDS);
		notifyActiveSkillHook(captain, null, matchManager);
		captain.sendMessage(textTemplateResolver.format("&d사령관 스킬 발동"), true);
		return true;
	}

	public boolean useUnitAbility(ServerPlayerEntity player, MatchManager matchManager, UnitRegistry unitRegistry) {
		var state = matchManager.getPlayerState(player.getUuid());
		var unitId = state.getCurrentUnitId();
		if (unitId == null) {
			return false;
		}

		var definition = unitRegistry.get(unitId);
		if (definition == null || definition.abilityType() == UnitDefinition.UnitAbilityType.NONE) {
			return false;
		}

		var nextUseTick = unitCooldownTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE);
		if (matchManager.getServerTicks() < nextUseTick) {
			var remaining = (int) Math.ceil((nextUseTick - matchManager.getServerTicks()) / 20.0D);
			player.sendMessage(textTemplateResolver.format("&c유닛 스킬 쿨다운: &f" + remaining + "초"), true);
			return false;
		}

		var success = switch (definition.abilityType()) {
			case HEAL_SELF -> {
				player.heal(6.0f);
				yield true;
			}
			case DASH -> {
				var dash = player.getRotationVec(1.0f).multiply(1.2D);
				player.addVelocity(dash.x, 0.3D, dash.z);
				yield true;
			}
			case GIVE_FIREWORKS -> {
				player.getInventory().insertStack(new net.minecraft.item.ItemStack(net.minecraft.item.Items.FIREWORK_ROCKET, 3));
				yield true;
			}
			case BONE_BLAST -> {
				for (var target : nearbyEnemyPlayers(player, matchManager, 4.0D)) {
					target.damage((ServerWorld) player.getWorld(), target.getDamageSources().mobAttack(player), 6.0f);
					target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 3, 1), player);
				}
				yield true;
			}
			case CREEPER_BOMB -> {
				pendingCreeperBombTicks.put(player.getUuid(), matchManager.getServerTicks() + 20L);
				player.sendMessage(textTemplateResolver.format("&c자폭 준비"), true);
				yield true;
			}
			case ZOMBIFIED_PIGLIN_RAGE -> {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 8, 1));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 8, 0));
				yield true;
			}
			case BLAZE_BURST -> {
				spawnFireballs(player);
				yield true;
			}
			case BRUTE_FRENZY -> {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 10, 1));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 10, 0));
				yield true;
			}
			case NONE -> false;
		};

		if (!success) {
			return false;
		}
		notifyActiveSkillHook(player, definition, matchManager);

		unitCooldownTicks.put(player.getUuid(), matchManager.getServerTicks() + definition.abilityCooldownSeconds() * 20L);
		player.getItemCooldownManager().set(player.getMainHandStack(), definition.abilityCooldownSeconds() * 20);
		return true;
	}

	public void handleEnemyKill(UUID killerId, MatchManager matchManager, CaptainManaService captainManaService, UnitRegistry unitRegistry) {
		var killerState = matchManager.getPlayerState(killerId);
		var unitId = killerState.getCurrentUnitId();
		if (unitId == null) {
			return;
		}

		var definition = unitRegistry.get(unitId);
		if (definition == null || definition.passiveType() != UnitDefinition.UnitPassiveType.ZOMBIE_COOLDOWN_REFUND) {
			return;
		}

		var captainId = matchManager.getCaptainId(killerState.getTeamId());
		if (captainId != null) {
			captainManaService.reduceSpawnCooldown(captainId, 2);
		}
	}

	public void handleUnitDeath(ServerPlayerEntity player, MatchManager matchManager, UnitRegistry unitRegistry) {
		var state = matchManager.getPlayerState(player.getUuid());
		var unitId = state.getCurrentUnitId();
		if (unitId == null) {
			return;
		}

		var definition = unitRegistry.get(unitId);
		if (definition == null) {
			return;
		}

		if (definition.passiveType() == UnitDefinition.UnitPassiveType.SLIME_SPLIT) {
			spawnSlimes(player);
		}
		if (definition.passiveType() == UnitDefinition.UnitPassiveType.PIGLIN_ZOMBIFY_ON_DEATH && player.getRandom().nextFloat() < 0.5f) {
			spawnZombifiedPiglin(player);
		}
	}

	public void clearPlayerState(UUID playerId) {
		unitCooldownTicks.remove(playerId);
		pendingCreeperBombTicks.remove(playerId);
	}

	private void explodeCreeper(ServerPlayerEntity player, MatchManager matchManager) {
		player.getWorld().playSound(
			null,
			player.getBlockPos(),
			SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
			SoundCategory.PLAYERS,
			1.0f,
			1.0f
		);
		for (var target : nearbyEnemyPlayers(player, matchManager, 5.0D)) {
			target.damage((ServerWorld) player.getWorld(), target.getDamageSources().explosion(player, player), 14.0f);
		}
		player.damage((ServerWorld) player.getWorld(), player.getDamageSources().explosion(player, player), 1000.0f);
	}

	private List<ServerPlayerEntity> nearbyEnemyPlayers(ServerPlayerEntity player, MatchManager matchManager, double radius) {
		var playerState = matchManager.getPlayerState(player.getUuid());
		var squaredRadius = radius * radius;
		return player.getServer().getPlayerManager().getPlayerList().stream()
			.filter(target -> target.getWorld() == player.getWorld())
			.filter(target -> !target.getUuid().equals(player.getUuid()))
			.filter(target -> player.squaredDistanceTo(target) <= squaredRadius)
			.filter(target -> {
				var targetState = matchManager.getPlayerState(target.getUuid());
				return targetState.getTeamId() != null
					&& playerState.getTeamId() != null
					&& targetState.getTeamId() != playerState.getTeamId()
					&& targetState.getRoleType() != RoleType.SPECTATOR;
			})
			.toList();
	}

	private void spawnFireballs(ServerPlayerEntity player) {
		var world = (ServerWorld) player.getWorld();
		var forward = player.getRotationVec(1.0f);
		var side = player.getRotationVector(0.0f, player.getYaw() + 90.0f).normalize().multiply(0.15D);
		for (int index = -1; index <= 1; index++) {
			var velocity = forward.add(side.multiply(index * 1.5D)).normalize();
			var fireball = new SmallFireballEntity(world, player, velocity);
			fireball.refreshPositionAndAngles(player.getX(), player.getEyeY() - 0.1D, player.getZ(), player.getYaw(), player.getPitch());
			world.spawnEntity(fireball);
		}
	}

	private void spawnSlimes(ServerPlayerEntity player) {
		var world = (ServerWorld) player.getWorld();
		for (int index = 0; index < 3; index++) {
			var slime = EntityType.SLIME.create(world, SpawnReason.MOB_SUMMONED);
			if (slime == null) {
				continue;
			}
			slime.setSize(1, true);
			slime.refreshPositionAndAngles(player.getX() + (index - 1), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
			world.spawnEntity(slime);
		}
	}

	private void spawnZombifiedPiglin(ServerPlayerEntity player) {
		var world = (ServerWorld) player.getWorld();
		var piglin = EntityType.ZOMBIFIED_PIGLIN.create(world, SpawnReason.MOB_SUMMONED);
		if (piglin == null) {
			return;
		}
		piglin.refreshPositionAndAngles(new BlockPos((int) player.getX(), (int) player.getY(), (int) player.getZ()), player.getYaw(), player.getPitch());
		world.spawnEntity(piglin);
	}

	private void notifyActiveSkillHook(ServerPlayerEntity player, UnitDefinition definition, MatchManager matchManager) {
		if (laneRuntimeRegistry == null) {
			return;
		}
		var runtime = laneRuntimeRegistry.findByPlayer(player);
		if (runtime == null || !runtime.hasActiveBiome()) {
			return;
		}
		var context = new BiomeRuntimeContext(
			player.getServer(),
			(ServerWorld) player.getWorld(),
			runtime,
			runtime.biomeEntry(),
			textTemplateResolver,
			matchManager.getServerTicks(),
			matchManager.getTotalSeconds() - matchManager.getRemainingSeconds()
		);
		runtime.biomeEffect().onActiveSkill(context, player, definition);
	}
}

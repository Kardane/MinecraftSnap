package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.integration.DisguiseAnimationSupport;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.SummonedMobSupport;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.IntPredicate;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SummonerUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	private static final String CAST_MODE_KEY = "summoner_cast_mode";
	private static final String CAST_COMPLETE_TICK_KEY = "summoner_cast_complete_tick";
	private static final String NEXT_FANG_TICK_KEY = "summoner_next_fang_tick";
	private static final String NEXT_FANG_INDEX_KEY = "summoner_next_fang_index";
	private static final long CAST_MODE_VEX = 1L;
	private static final long CAST_MODE_FANG = 2L;

	@Override
	public void onSkillUse(UnitContext context) {
		long castMode = hasFriendlyVexNearby(context) ? CAST_MODE_FANG : CAST_MODE_VEX;
		context.activateSkill(skillCooldownTicks(castMode), () -> {
			if (isCasting(context) || isSpawningFangs(context)) {
				return false;
			}
			beginCast(context, castMode);
			DisguiseAnimationSupport.startEvokerCast(context.player(), (int) castDurationTicks());
			context.world().playSound(null, context.player().getBlockPos(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.9f, 1.0f);
			return true;
		});
	}

	@Override
	public void onTick(UnitContext context) {
		clearFriendlyVexTargets(context);
		if (isCasting(context)) {
			applyCastingMoveSpeed(context);
			if (context.serverTicks() >= castCompleteTick(context)) {
				finishCast(context);
			}
			return;
		}
		restoreMoveSpeed(context);
		if (isSpawningFangs(context) && context.serverTicks() >= nextFangTick(context)) {
			spawnNextFang(context);
		}
	}

	@Override
	public void onDeath(UnitContext context, net.minecraft.entity.damage.DamageSource source) {
		clearCastState(context);
		restoreMoveSpeed(context);
	}

	int summonedVexCount() {
		return 2;
	}

	String summonedVexWeaponItemId() {
		return "minecraft:iron_sword";
	}

	int fangCount() {
		return 24;
	}

	double vexSearchRadius() {
		return 24.0D;
	}

	long castDurationTicks() {
		return 30L;
	}

	long fangIntervalTicks() {
		return 1L;
	}

	long vexSkillCooldownTicks() {
		return 20L * 10L;
	}

	long fangSkillCooldownTicks() {
		return 20L * 5L;
	}

	double fangDistance(int index) {
		return 1.0D + index * 1.0D;
	}

	int resolveFangSpawnY(int originY, int bottomY, IntPredicate solidBlockAtY) {
		if (solidBlockAtY == null) {
			return Integer.MIN_VALUE;
		}
		for (int y = originY; y >= bottomY; y--) {
			if (solidBlockAtY.test(y)) {
				return y + 1;
			}
		}
		return Integer.MIN_VALUE;
	}

	private boolean hasFriendlyVexNearby(UnitContext context) {
		var player = context.player();
		if (player == null || context.state() == null || context.state().getTeamId() == null) {
			return false;
		}
		return player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(vexSearchRadius()), entity ->
			entity instanceof VexEntity vex
				&& SummonedMobSupport.resolveManagedTeam(vex) == context.state().getTeamId())
			.stream()
			.findAny()
			.isPresent();
	}

	private void summonVexes(UnitContext context) {
		var world = context.world();
		var player = context.player();
		if (world == null || player == null) {
			return;
		}
		for (int index = 0; index < summonedVexCount(); index++) {
			var vex = EntityType.VEX.create(world, SpawnReason.MOB_SUMMONED);
			if (vex == null) {
				continue;
			}
			var offsetX = index == 0 ? -0.8D : 0.8D;
			vex.refreshPositionAndAngles(player.getX() + offsetX, player.getEyeY(), player.getZ(), player.getYaw(), 0.0f);
			vex.setCustomName(player.getName().copy());
			vex.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			vex.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
			SummonedMobSupport.applyFriendlyTeam(context, vex);
			vex.setTarget(null);
			world.spawnEntity(vex);
		}
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.9f, 1.0f);
	}

	private void spawnFangs(UnitContext context, int index) {
		var world = context.world();
		var player = context.player();
		if (world == null || player == null) {
			return;
		}
		var direction = horizontalDirection(player);
		var distance = fangDistance(index);
		var position = player.getPos().add(direction.multiply(distance));
		var blockPos = BlockPos.ofFloored(position);
		var spawnY = resolveFangSpawnY(
			blockPos.getY(),
			world.getBottomY(),
			y -> !world.getBlockState(new BlockPos(blockPos.getX(), y, blockPos.getZ())).isAir()
		);
		if (spawnY == Integer.MIN_VALUE) {
			return;
		}
		var fangs = EntityType.EVOKER_FANGS.create(world, SpawnReason.MOB_SUMMONED);
		if (fangs == null) {
			return;
		}
		fangs.refreshPositionAndAngles(position.x, spawnY, position.z, 0.0f, 0.0f);
		fangs.setOwner(player);
		world.spawnEntity(fangs);
	}

	private void beginCast(UnitContext context, long castMode) {
		context.setUnitRuntimeLong(CAST_MODE_KEY, castMode);
		context.setUnitRuntimeLong(CAST_COMPLETE_TICK_KEY, context.serverTicks() + castDurationTicks());
		context.removeUnitRuntimeLong(NEXT_FANG_TICK_KEY);
		context.removeUnitRuntimeLong(NEXT_FANG_INDEX_KEY);
	}

	private boolean isCasting(UnitContext context) {
		return context.getUnitRuntimeLong(CAST_MODE_KEY) != null && context.getUnitRuntimeLong(CAST_COMPLETE_TICK_KEY) != null;
	}

	private boolean isSpawningFangs(UnitContext context) {
		return context.getUnitRuntimeLong(NEXT_FANG_INDEX_KEY) != null && context.getUnitRuntimeLong(NEXT_FANG_TICK_KEY) != null;
	}

	private long castCompleteTick(UnitContext context) {
		var value = context.getUnitRuntimeLong(CAST_COMPLETE_TICK_KEY);
		return value == null ? Long.MIN_VALUE : value;
	}

	private long nextFangTick(UnitContext context) {
		var value = context.getUnitRuntimeLong(NEXT_FANG_TICK_KEY);
		return value == null ? Long.MAX_VALUE : value;
	}

	private void finishCast(UnitContext context) {
		var castMode = context.getUnitRuntimeLong(CAST_MODE_KEY);
		context.removeUnitRuntimeLong(CAST_MODE_KEY);
		context.removeUnitRuntimeLong(CAST_COMPLETE_TICK_KEY);
		restoreMoveSpeed(context);
		if (castMode == null) {
			return;
		}
		if (castMode == CAST_MODE_VEX) {
			summonVexes(context);
			return;
		}
		context.setUnitRuntimeLong(NEXT_FANG_INDEX_KEY, 0L);
		context.setUnitRuntimeLong(NEXT_FANG_TICK_KEY, context.serverTicks());
		context.world().playSound(null, context.player().getBlockPos(), SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.PLAYERS, 0.9f, 1.0f);
	}

	private void spawnNextFang(UnitContext context) {
		var nextIndexValue = context.getUnitRuntimeLong(NEXT_FANG_INDEX_KEY);
		int nextIndex = nextIndexValue == null ? fangCount() : (int) (long) nextIndexValue;
		if (nextIndex >= fangCount()) {
			context.removeUnitRuntimeLong(NEXT_FANG_INDEX_KEY);
			context.removeUnitRuntimeLong(NEXT_FANG_TICK_KEY);
			return;
		}
		spawnFangs(context, nextIndex);
		context.setUnitRuntimeLong(NEXT_FANG_INDEX_KEY, nextIndex + 1L);
		context.setUnitRuntimeLong(NEXT_FANG_TICK_KEY, context.serverTicks() + fangIntervalTicks());
	}

	private void clearCastState(UnitContext context) {
		context.removeUnitRuntimeLong(CAST_MODE_KEY);
		context.removeUnitRuntimeLong(CAST_COMPLETE_TICK_KEY);
		context.removeUnitRuntimeLong(NEXT_FANG_INDEX_KEY);
		context.removeUnitRuntimeLong(NEXT_FANG_TICK_KEY);
	}

	private void applyCastingMoveSpeed(UnitContext context) {
		var moveSpeed = context.player().getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		if (moveSpeed != null) {
			moveSpeed.setBaseValue(0.1D * context.unitDefinition().moveSpeedScale() * 0.5D);
		}
	}

	private void restoreMoveSpeed(UnitContext context) {
		var moveSpeed = context.player().getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		if (moveSpeed != null) {
			moveSpeed.setBaseValue(0.1D * context.unitDefinition().moveSpeedScale());
		}
	}

	private void clearFriendlyVexTargets(UnitContext context) {
		var player = context.player();
		if (player == null || context.state() == null || context.state().getTeamId() == null) {
			return;
		}
		player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(vexSearchRadius()), entity ->
			entity instanceof VexEntity vex
				&& SummonedMobSupport.resolveManagedTeam(vex) == context.state().getTeamId())
			.forEach(entity -> {
				var vex = (VexEntity) entity;
				if (vex.getTarget() instanceof net.minecraft.server.network.ServerPlayerEntity targetPlayer && !context.isEnemyUnit(targetPlayer)) {
					vex.setTarget(null);
				}
			});
	}

	private Vec3d horizontalDirection(net.minecraft.server.network.ServerPlayerEntity player) {
		var direction = player.getRotationVec(1.0f);
		var horizontal = new Vec3d(direction.x, 0.0D, direction.z);
		return horizontal.lengthSquared() > 1.0E-6D ? horizontal.normalize() : Vec3d.fromPolar(0.0f, player.getYaw()).normalize();
	}

	private long skillCooldownTicks(long castMode) {
		return castMode == CAST_MODE_FANG ? fangSkillCooldownTicks() : vexSkillCooldownTicks();
	}
}

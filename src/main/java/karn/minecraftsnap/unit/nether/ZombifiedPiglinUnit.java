package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.SummonedMobSupport;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ZombifiedPiglinUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	@Override
	public void onTick(UnitContext context) {
		context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireResistanceDurationTicks(), 0, true, false, false));
	}

	@Override
	public void onDamaged(UnitContext context, DamageSource source, float amount) {
		var player = context.player();
		var attacker = resolveEnemyAttacker(context, source);
		if (player == null || attacker == null || !isRetaliationWindow(player.timeUntilRegen)) {
			return;
		}
		var world = context.world();
		if (world == null) {
			return;
		}
		commandNearbyPiglins(context, attacker);
		if (!shouldTriggerRetaliation(player.getRandom().nextDouble())) {
			return;
		}
		var piglin = EntityType.ZOMBIFIED_PIGLIN.create(world, SpawnReason.MOB_SUMMONED);
		if (piglin == null) {
			return;
		}
		var random = player.getRandom();
		var x = player.getX() + random.nextBetween(-5, 5);
		var z = player.getZ() + random.nextBetween(-5, 5);
		if (!SummonedMobSupport.placeMobSafely(world, piglin, x, player.getY(), z, player.getYaw(), player.getPitch())) {
			return;
		}
		piglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
		piglin.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
		piglin.equipStack(EquipmentSlot.CHEST, teamChestplate(context));
		piglin.setEquipmentDropChance(EquipmentSlot.CHEST, 0.0f);
		SummonedMobSupport.applyFriendlyTeam(context, piglin);
		piglin.setTarget(attacker);
		world.spawnEntity(piglin);
	}

	private ItemStack teamChestplate(UnitContext context) {
		var stack = new ItemStack(Items.LEATHER_CHESTPLATE);
		var color = context.state().getTeamId() == karn.minecraftsnap.game.TeamId.BLUE ? 0x3366FF : 0xFF3333;
		stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color));
		stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.builder().build());
		return stack;
	}

	@Override
	public void onDeath(UnitContext context, DamageSource source) {
		if (!shouldRestoreCaptainManaOnDeath(context)) {
			return;
		}
		context.restoreCaptainMana(captainManaRestoreOnDeath());
	}

	boolean shouldRestoreCaptainManaOnDeath(UnitContext context) {
		return countFriendlyZombifiedPiglinUnitsOnLane(context) <= maxFriendlyZombifiedPiglinUnitsForRefund();
	}

	int countFriendlyZombifiedPiglinUnitsOnLane(UnitContext context) {
		if (context == null || context.laneRuntime() == null || context.matchManager() == null || context.matchManager().getServer() == null || context.state() == null || context.state().getTeamId() == null) {
			return Integer.MAX_VALUE;
		}
		var teamId = context.state().getTeamId();
		return (int) context.laneRuntime().resolveAliveUnitPlayers(context.matchManager().getServer()).stream()
			.map(player -> context.matchManager().getPlayerState(player.getUuid()))
			.filter(state -> state.getTeamId() == teamId)
			.filter(state -> "zombified_piglin".equals(state.getCurrentUnitId()))
			.count();
	}

	int maxFriendlyZombifiedPiglinUnitsForRefund() {
		return 5;
	}

	String summonedPiglinWeaponItemId() {
		return "minecraft:golden_sword";
	}

	int fireResistanceDurationTicks() {
		return 40;
	}

	boolean shouldTriggerRetaliation(double randomRoll) {
		return randomRoll < summonChance();
	}

	boolean isRetaliationWindow(int timeUntilRegen) {
		return timeUntilRegen <= retaliationInvulnerabilityThresholdTicks();
	}

	private Entity resolveAttacker(Entity entity) {
		if (entity instanceof ProjectileEntity projectile) {
			return projectile.getOwner();
		}
		return entity;
	}

	private LivingEntity resolveEnemyAttacker(UnitContext context, DamageSource source) {
		var attacker = resolveAttacker(source == null ? null : source.getAttacker());
		if (attacker instanceof LivingEntity living && context.isEnemyTarget(living)) {
			return living;
		}
		attacker = resolveAttacker(source == null ? null : source.getSource());
		if (attacker instanceof LivingEntity living && context.isEnemyTarget(living)) {
			return living;
		}
		return null;
	}

	private void commandNearbyPiglins(UnitContext context, LivingEntity attacker) {
		if (context == null || context.world() == null || context.player() == null || context.state() == null || context.state().getTeamId() == null || attacker == null) {
			return;
		}
		var box = context.player().getBoundingBox().expand(summonRange());
		for (var piglin : context.world().getEntitiesByClass(ZombifiedPiglinEntity.class, box, this::isAlive)) {
			if (SummonedMobSupport.resolveManagedTeam(piglin) != context.state().getTeamId()) {
				continue;
			}
			piglin.setTarget(attacker);
		}
	}

	private boolean isAlive(ZombifiedPiglinEntity piglin) {
		return piglin != null && piglin.isAlive();
	}

	double summonChance() {
		return 0.25D;
	}

	int retaliationInvulnerabilityThresholdTicks() {
		return 10;
	}

	double summonRange() {
		return 12.0D;
	}

	int captainManaRestoreOnDeath() {
		return 1;
	}
}

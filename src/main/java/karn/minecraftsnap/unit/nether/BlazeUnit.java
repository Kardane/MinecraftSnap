package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;
import static karn.minecraftsnap.unit.UnitSpecSupport.applyEnchantment;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;
import static net.minecraft.enchantment.Enchantments.FIRE_ASPECT;

public class BlazeUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	private static final String SHOTS_REMAINING_KEY = "blaze_shots_remaining";
	private static final String NEXT_SHOT_TICK_KEY = "blaze_next_shot_tick";

	public static final UnitDefinition DEFINITION = unit(
		"blaze",
		"블레이즈",
		FactionId.NETHER,
		true,
		3,
		15,
		14.0,
		1.2,
		item("minecraft:blaze_rod"),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		item("minecraft:blaze_rod"),
		"화염구 3연사",
		9,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:blaze"),
			List.of("&f화염구 발사&7- 화염구를 3개 연속으로 발사합니다","&f무기 &7- 블레이즈 막대"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), definition().id(), weaponAttackDamage(), weaponAttackSpeed());
		applyEnchantment(context.world(), context.player().getMainHandStack(), FIRE_ASPECT, weaponFireAspectLevel());
		context.removeUnitRuntimeLong(SHOTS_REMAINING_KEY);
		context.removeUnitRuntimeLong(NEXT_SHOT_TICK_KEY);
	}

	@Override
	public void onTick(UnitContext context) {
		context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireResistanceDurationTicks(), 0, true, false, false));
		var remaining = context.getUnitRuntimeLong(SHOTS_REMAINING_KEY);
		var nextShotTick = context.getUnitRuntimeLong(NEXT_SHOT_TICK_KEY);
		if (remaining == null || remaining <= 0 || nextShotTick == null || context.serverTicks() < nextShotTick) {
			return;
		}
		fireSingleShot(context);
		if (remaining <= 1) {
			context.removeUnitRuntimeLong(SHOTS_REMAINING_KEY);
			context.removeUnitRuntimeLong(NEXT_SHOT_TICK_KEY);
			return;
		}
		context.setUnitRuntimeLong(SHOTS_REMAINING_KEY, remaining - 1);
		context.setUnitRuntimeLong(NEXT_SHOT_TICK_KEY, context.serverTicks() + shotIntervalTicks());
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			context.setUnitRuntimeLong(SHOTS_REMAINING_KEY, (long) shotCount());
			context.setUnitRuntimeLong(NEXT_SHOT_TICK_KEY, context.serverTicks());
			return true;
		});
	}

	private void fireSingleShot(UnitContext context) {
		var world = context.world();
		var player = context.player();
		var direction = player.getRotationVec(1.0f).normalize();
		var fireball = new SmallFireballEntity(world, player, direction);
		fireball.refreshPositionAndAngles(player.getX() + direction.x * 0.5D, player.getEyeY() - 0.1D, player.getZ() + direction.z * 0.5D, player.getYaw(), player.getPitch());
		fireball.setVelocity(direction.multiply(1.1D));
		world.spawnEntity(fireball);
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.9f, 1.0f);
	}

	double weaponAttackDamage() {
		return 2.0D;
	}

	double weaponAttackSpeed() {
		return 1.0D;
	}

	int weaponFireAspectLevel() {
		return 2;
	}

	int shotCount() {
		return 3;
	}

	long shotIntervalTicks() {
		return 8L;
	}

	int fireResistanceDurationTicks() {
		return 40;
	}
}

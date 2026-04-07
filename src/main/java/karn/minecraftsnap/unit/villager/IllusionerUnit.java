package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.integration.DisguiseAnimationSupport;
import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class IllusionerUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	private static final String INVISIBLE_DISGUISE_END_TICK_KEY = "illusioner_invisible_disguise_end_tick";

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			if (context.player() == null) {
				return false;
			}
			context.world().playSound(null, context.player().getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.9f, 1.0f);
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, invisibilityDurationTicks(), 0, false, false, false), context.player());
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, invisibilityDurationTicks(), 0, false, false, false), context.player());
			if (shouldApplyInvisibilityToDisguise()) {
				context.setUnitRuntimeLong(INVISIBLE_DISGUISE_END_TICK_KEY, context.serverTicks() + invisibilityDurationTicks());
				DisguiseSupport.applyDisguise(context.player(), context.unitDefinition().disguise(), entity -> entity.setInvisible(true));
			}
			applyNearbyBlindness(context);
			DisguiseAnimationSupport.startIllusionerCast(context.player(), castAnimationDurationTicks());
			return true;
		});
	}

	@Override
	public void onTick(UnitContext context) {
		var invisibleDisguiseEndTick = context.getUnitRuntimeLong(INVISIBLE_DISGUISE_END_TICK_KEY);
		if (invisibleDisguiseEndTick == null || context.player() == null || context.serverTicks() < invisibleDisguiseEndTick) {
			return;
		}
		context.removeUnitRuntimeLong(INVISIBLE_DISGUISE_END_TICK_KEY);
		DisguiseSupport.applyDisguise(context.player(), context.unitDefinition().disguise());
	}

	@Override
	public void onDeath(UnitContext context, net.minecraft.entity.damage.DamageSource source) {
		context.removeUnitRuntimeLong(INVISIBLE_DISGUISE_END_TICK_KEY);
	}

	int invisibilityDurationTicks() {
		return 20 * 10;
	}

	int castAnimationDurationTicks() {
		return 30;
	}

	int nearbyBlindnessDurationTicks() {
		return 20 * 15;
	}

	double nearbyBlindnessRadius() {
		return 6.0D;
	}

	SoundEvent skillSound() {
		return SoundEvent.of(Identifier.of(skillSoundId()));
	}

	String skillSoundId() {
		return "minecraft:entity.illusioner.prepare_mirror";
	}

	boolean shouldApplyInvisibilityToDisguise() {
		return true;
	}

	private void applyNearbyBlindness(UnitContext context) {
		if (context.player() == null || context.world() == null) {
			return;
		}
		var radius = nearbyBlindnessRadius();
		var nearbyPlayers = context.world().getPlayers(player ->
			player != context.player()
				&& context.isEnemyUnit(player)
				&& player.squaredDistanceTo(context.player()) <= radius * radius
				&& player.getBoundingBox().intersects(Box.of(context.player().getPos(), radius * 2.0D, radius * 2.0D, radius * 2.0D))
		);
		for (var nearbyPlayer : nearbyPlayers) {
			nearbyPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, nearbyBlindnessDurationTicks(), 0, false, true, true), context.player());
		}
	}
}

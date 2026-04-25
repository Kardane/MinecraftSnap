package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class PiglinBruteUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		//context.player().getInventory().insertStack(new ItemStack(Items.GOLD_INGOT, supportGoldCount()));
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			var player = context.player();
			if (player == null) {
				return false;
			}
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, buffDurationTicks(), 0));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, buffDurationTicks(), 0));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, buffDurationTicks(), 0));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, buffDurationTicks(), 0));
			if (context.world() != null) {
				context.world().playSound(null, player.getBlockPos(), skillSound(), SoundCategory.PLAYERS, 0.9f, 1.0f);
			}
			return true;
		});
	}

	@Override
	public void onKill(UnitContext context, ServerPlayerEntity victim) {
		super.onKill(context, victim);
		context.rewardGold(1);
	}

	int supportGoldCount() {
		return 3;
	}

	int buffDurationTicks() {
		return 20 * 3;
	}

	SoundEvent skillSound() {
		return SoundEvent.of(Identifier.of(skillSoundId()));
	}

	String skillSoundId() {
		return "minecraft:entity.piglin.angry";
	}
}

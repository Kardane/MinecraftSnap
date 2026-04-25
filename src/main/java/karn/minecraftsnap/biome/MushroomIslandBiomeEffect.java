package karn.minecraftsnap.biome;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class MushroomIslandBiomeEffect extends NoOpBiomeEffect {
	private static final int STEW_INTERVAL_SECONDS = 15;
	private static final List<SuspiciousStewEffectsComponent.StewEffect> STEW_EFFECTS = List.of(
		new SuspiciousStewEffectsComponent.StewEffect(StatusEffects.NIGHT_VISION, 100),
		new SuspiciousStewEffectsComponent.StewEffect(StatusEffects.JUMP_BOOST, 120),
		new SuspiciousStewEffectsComponent.StewEffect(StatusEffects.WEAKNESS, 180),
		new SuspiciousStewEffectsComponent.StewEffect(StatusEffects.BLINDNESS, 160),
		new SuspiciousStewEffectsComponent.StewEffect(StatusEffects.SATURATION, 7),
		new SuspiciousStewEffectsComponent.StewEffect(StatusEffects.FIRE_RESISTANCE, 80),
		new SuspiciousStewEffectsComponent.StewEffect(StatusEffects.REGENERATION, 160),
		new SuspiciousStewEffectsComponent.StewEffect(StatusEffects.POISON, 240),
		new SuspiciousStewEffectsComponent.StewEffect(StatusEffects.WITHER, 160)
	);
	private int lastGrantedSecond = -1;

	@Override
	public void onTick(BiomeRuntimeContext context) {
		super.onTick(context);
		var secondsSinceReveal = context.secondsSinceReveal();
		if (!shouldGrantStew(context.serverTicks(), secondsSinceReveal)) {
			return;
		}
		lastGrantedSecond = secondsSinceReveal;
		context.playSound("minecraft:entity.generic.eat", 0.9f, 0.8f);
		for (var player : context.aliveUnitPlayers()) {
			player.getInventory().insertStack(createRandomStew(player.getRandom().nextInt(STEW_EFFECTS.size())));
		}
	}

	boolean shouldGrantStew(long serverTicks, int secondsSinceReveal) {
		return serverTicks % 20L == 0L
			&& secondsSinceReveal > 0
			&& secondsSinceReveal % STEW_INTERVAL_SECONDS == 0
			&& secondsSinceReveal != lastGrantedSecond;
	}

	private ItemStack createRandomStew(int effectIndex) {
		var stew = new ItemStack(Items.SUSPICIOUS_STEW);
		var effect = STEW_EFFECTS.get(Math.floorMod(effectIndex, STEW_EFFECTS.size()));
		stew.set(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffectsComponent.DEFAULT.with(effect));
		return stew;
	}
}

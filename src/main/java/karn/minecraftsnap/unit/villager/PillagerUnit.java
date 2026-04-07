package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import it.unimi.dsi.fastutil.ints.IntList;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class PillagerUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	private static final String QUICK_CHARGE_LEVEL_KEY = "villager_enchant_quickcharge";

	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		var definition = context.unitDefinition();
		var cooldownTicks = skillCooldownTicks(definition == null ? 0 : definition.abilityCooldownSeconds(), quickChargeLevel(context));
		context.activateSkill(cooldownTicks, () -> {
			var player = context.player();
			if (player == null) {
				return false;
			}
			var crossbow = findCrossbow(player);
			if (crossbow == null || !loadCrossbowWithRocket(crossbow, createSkillRocket(context.state() == null ? null : context.state().getTeamId()))) {
				player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 0.9f, 0.8f);
				return false;
			}
			player.getInventory().markDirty();
			player.playerScreenHandler.sendContentUpdates();
			player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.9f, 1.1f);
			return true;
		});
	}

	long skillCooldownTicks(int baseCooldownSeconds, int quickChargeLevel) {
		return Math.max(0, baseCooldownSeconds - Math.max(0, quickChargeLevel)) * 20L;
	}

	int quickChargeLevel(karn.minecraftsnap.unit.UnitContext context) {
		if (context == null || context.state() == null) {
			return 0;
		}
		var level = context.state().getUnitRuntimeInt(QUICK_CHARGE_LEVEL_KEY);
		return Math.max(0, level == null ? 0 : level);
	}

	ItemStack createSkillRocket(TeamId teamId) {
		var rocket = new ItemStack(Items.FIREWORK_ROCKET);
		rocket.set(DataComponentTypes.FIREWORKS, skillRocketPayload(teamId));
		return rocket;
	}

	FireworksComponent skillRocketPayload(TeamId teamId) {
		return new FireworksComponent(
			1,
			List.of(
				createExplosion(teamId),
				createExplosion(teamId)
			)
		);
	}

	boolean shouldLoadRocketIntoCrossbow() {
		return true;
	}

	private ItemStack findCrossbow(net.minecraft.server.network.ServerPlayerEntity player) {
		if (player == null) {
			return null;
		}
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			var stack = player.getInventory().getStack(slot);
			if (stack == null || stack.isEmpty() || stack.getItem() != Items.CROSSBOW) {
				continue;
			}
			return stack;
		}
		return null;
	}

	private FireworkExplosionComponent createExplosion(TeamId teamId) {
		return new FireworkExplosionComponent(
			FireworkExplosionComponent.Type.SMALL_BALL,
			IntList.of(fireworkColor(teamId)),
			IntList.of(),
			false,
			false
		);
	}

	int fireworkColor(TeamId teamId) {
		if (teamId == TeamId.RED) {
			return 0xFF3333;
		}
		if (teamId == TeamId.BLUE) {
			return 0x3366FF;
		}
		return 0xFFFFFF;
	}

	boolean loadCrossbowWithRocket(ItemStack crossbow, ItemStack rocket) {
		if (!shouldLoadRocketIntoCrossbow() || crossbow == null || crossbow.isEmpty() || crossbow.getItem() != Items.CROSSBOW || rocket == null || rocket.isEmpty()) {
			return false;
		}
		var chargedProjectiles = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
		if (chargedProjectiles != null && !chargedProjectiles.isEmpty()) {
			return false;
		}
		crossbow.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.of(rocket.copyWithCount(1)));
		return true;
	}
}

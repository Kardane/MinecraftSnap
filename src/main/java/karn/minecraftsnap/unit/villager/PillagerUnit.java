package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitLoadoutService;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;
import java.util.Random;

import it.unimi.dsi.fastutil.ints.IntList;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class PillagerUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	private static final int[] FIREWORK_COLORS = {
		0xFF5555,
		0x55FF55,
		0x5555FF,
		0xFFFF55,
		0xFF55FF,
		0x55FFFF,
		0xFFFFFF,
		0xFFAA00
	};

	public static final UnitDefinition DEFINITION = unit(
		"pillager",
		"약탈자",
		FactionId.VILLAGER,
		true,
		3,
		18.0,
		1.0,
		item("minecraft:crossbow"),
		none(),
		none(),
		none(),
		none(),
		none(),
		abilityItem("minecraft:firework_star", "폭죽 탄약 장전", 10),
		"폭죽 탄약 장전",
		10,
		UnitDefinition.AmmoType.ARROW,
		disguise("minecraft:pillager"),
			List.of("&f무기 &7- 쇠뇌","&f폭죽 보급 &7- 폭죽을 얻습니다."),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			var player = context.player();
			var triggerStack = player.getMainHandStack();
			if (hasUsableRocket(player, triggerStack)) {
				player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 0.9f, 0.8f);
				return false;
			}
			if (!player.getInventory().insertStack(createSkillRocket(new Random(context.serverTicks() ^ player.getUuid().getLeastSignificantBits())))) {
				player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 0.9f, 0.8f);
				return false;
			}
			player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.9f, 1.1f);
			return true;
		});
	}

	ItemStack createSkillRocket(Random random) {
		var rocket = new ItemStack(Items.FIREWORK_ROCKET);
		rocket.set(DataComponentTypes.FIREWORKS, skillRocketPayload(random));
		return rocket;
	}

	FireworksComponent skillRocketPayload(Random random) {
		return new FireworksComponent(
			1,
			List.of(
				createExplosion(random),
				createExplosion(random)
			)
		);
	}

	private boolean hasUsableRocket(net.minecraft.server.network.ServerPlayerEntity player, ItemStack triggerStack) {
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			var stack = player.getInventory().getStack(slot);
			if (stack == null || stack.isEmpty() || stack == triggerStack || stack.getItem() != Items.FIREWORK_ROCKET) {
				continue;
			}
			if (isAbilityTriggerRocket(stack)) {
				continue;
			}
			return true;
		}
		return false;
	}

	boolean hasUsableRocket(Iterable<ItemStack> stacks, ItemStack triggerStack) {
		for (var stack : stacks) {
			if (stack == null || stack.isEmpty() || stack == triggerStack || stack.getItem() != Items.FIREWORK_ROCKET) {
				continue;
			}
			if (isAbilityTriggerRocket(stack)) {
				continue;
			}
			return true;
		}
		return false;
	}

	private FireworkExplosionComponent createExplosion(Random random) {
		return new FireworkExplosionComponent(
			FireworkExplosionComponent.Type.SMALL_BALL,
			IntList.of(randomColor(random)),
			IntList.of(),
			false,
			false
		);
	}

	private int randomColor(Random random) {
		return FIREWORK_COLORS[random.nextInt(FIREWORK_COLORS.length)];
	}

	private boolean isAbilityTriggerRocket(ItemStack stack) {
		var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (customData == null) {
			return false;
		}
		var nbt = customData.copyNbt();
		return UnitLoadoutService.KIND_UNIT_ABILITY.equals(nbt.getString(UnitLoadoutService.CUSTOM_DATA_KIND))
			&& definition().id().equals(nbt.getString(UnitLoadoutService.CUSTOM_DATA_UNIT_ID));
	}
}

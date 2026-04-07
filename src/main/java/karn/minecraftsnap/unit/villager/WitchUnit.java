package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;

public class WitchUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	private static final String POTION_KIND_KEY = "minecraftsnap_witch_potion";

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		var player = context.player();
		var world = context.world();
		if (player == null || world == null) {
			return;
		}
		var weapon = new ItemStack(Items.STICK);
		karn.minecraftsnap.unit.UnitSpecSupport.applyEnchantment(
			world,
			weapon,
			net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.ENCHANTMENT, net.minecraft.util.Identifier.of("minecraft:knockback")),
			1
		);
		player.equipStack(EquipmentSlot.MAINHAND, weapon);
		player.equipStack(EquipmentSlot.OFFHAND, createPotionStack(regenerationChoice()));
		//player.getInventory().insertStack(createPotionStack(speedChoice()));
		//player.getInventory().insertStack(createPotionStack(slownessChoice()));
		//player.getInventory().insertStack(createPotionStack(poisonChoice()));
	}

	@Override
	public void onTick(UnitContext context) {
		var player = context.player();
		var world = context.world();
		if (player == null || world == null) {
			return;
		}
		var choice = activeChoice(player.getOffHandStack());
		if (choice == null || context.serverTicks() <= 0L) {
			return;
		}
		if (shouldPlayAuraSoundAtTick(context.serverTicks())) {
			world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITCH_DRINK, SoundCategory.PLAYERS, 0.8f, 1.0f);
		}
		if (shouldSpawnAuraParticlesAtTick(context.serverTicks())) {
			spawnAuraParticles(context, choice);
		}
		if (context.serverTicks() % effectIntervalTicks(choice) == 0L) {
			applyAura(context, choice);
		}
	}

	List<PotionChoice> potionChoices() {
		return List.of(
			regenerationChoice(),
			speedChoice(),
			slownessChoice(),
			poisonChoice()
		);
	}

	PotionChoice regenerationChoice() {
		return new PotionChoice("minecraft:ghast_tear", "regeneration", "재생 오라", "주변 아군에게 재생을 부여합니다.");
	}

	PotionChoice speedChoice() {
		return new PotionChoice("minecraft:sugar", "swiftness", "신속 오라", "주변 아군에게 신속을 부여합니다.");
	}

	PotionChoice slownessChoice() {
		return new PotionChoice("minecraft:gunpowder", "slowness", "구속 오라", "주변 적군에게 구속을 부여합니다.");
	}

	PotionChoice poisonChoice() {
		return new PotionChoice("minecraft:spider_eye", "poison", "독 오라", "주변 적군에게 독을 부여합니다.");
	}

	String weaponItemId() {
		return "minecraft:stick";
	}

	String weaponComponentsNbt() {
		return "{\"minecraft:enchantments\"={levels:{\"minecraft:knockback\":1}}}";
	}

	long effectIntervalTicks(PotionChoice choice) {
		if (choice == null) {
			return 20L;
		}
		return switch (choice.potionId()) {
			case "regeneration", "poison" -> 20L * 6L;
			case "swiftness", "slowness" -> 20L;
			default -> 20L;
		};
	}

	long particleIntervalTicks() {
		return 2L;
	}

	long auraSoundIntervalTicks() {
		return 20L * 5L;
	}

	int auraParticlePoints() {
		return 50;
	}

	boolean shouldSpawnAuraParticlesAtTick(long serverTicks) {
		return serverTicks > 0L && serverTicks % particleIntervalTicks() == 0L;
	}

	boolean shouldPlayAuraSoundAtTick(long serverTicks) {
		return serverTicks > 0L && serverTicks % auraSoundIntervalTicks() == 0L;
	}

	double effectRadius(PotionChoice choice) {
		if (choice == null) {
			return 0.0D;
		}
		return switch (choice.potionId()) {
			case "regeneration", "poison" -> 4.5D;
			case "swiftness", "slowness" -> 5.5D;
			default -> 0.0D;
		};
	}

	private ItemStack createPotionStack(PotionChoice choice) {
		var stack = new ItemStack(resolvePotionItem(choice));
		var customData = new NbtCompound();
		customData.putString(POTION_KIND_KEY, choice.potionId());
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
		stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(choice.displayName()));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal(choice.loreLine()))));
		return stack;
	}

	private Item resolvePotionItem(PotionChoice choice) {
		return switch (choice.itemId()) {
			case "minecraft:ghast_tear" -> Items.GHAST_TEAR;
			case "minecraft:sugar" -> Items.SUGAR;
			case "minecraft:gunpowder" -> Items.GUNPOWDER;
			case "minecraft:spider_eye" -> Items.SPIDER_EYE;
			default -> Items.BARRIER;
		};
	}

	PotionChoice activeChoice(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (customData != null) {
			var potionId = customData.copyNbt().getString(POTION_KIND_KEY).orElse("");
			var taggedChoice = choiceForPotionId(potionId);
			if (taggedChoice != null) {
				return taggedChoice;
			}
		}
		return potionChoices().stream()
			.filter(choice -> matchesPotionContents(stack, choice))
			.findFirst()
			.orElse(null);
	}

	PotionChoice choiceForPotionId(String potionId) {
		return potionChoices().stream()
			.filter(choice -> Objects.equals(choice.potionId(), potionId))
			.findFirst()
			.orElse(null);
	}

	private boolean matchesPotionContents(ItemStack stack, PotionChoice choice) {
		if (stack == null || stack.isEmpty() || choice == null) {
			return false;
		}
		return Objects.equals(net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString(), choice.itemId());
	}

	private void applyAura(UnitContext context, PotionChoice choice) {
		var radiusSquared = effectRadius(choice) * effectRadius(choice);
		for (var target : context.world().getPlayers()) {
			if (target.squaredDistanceTo(context.player()) > radiusSquared) {
				continue;
			}
			if (shouldAffectAlly(choice)) {
				if (target != context.player() && context.isEnemyUnit(target)) {
					continue;
				}
			} else if (!context.isEnemyUnit(target)) {
				continue;
			}
			target.addStatusEffect(effectInstance(choice));
		}
	}

	private boolean shouldAffectAlly(PotionChoice choice) {
		return choice != null && ("regeneration".equals(choice.potionId()) || "swiftness".equals(choice.potionId()));
	}

	private StatusEffectInstance effectInstance(PotionChoice choice) {
		return switch (choice.potionId()) {
			case "regeneration" -> new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 6, 0, true, false, true);
			case "swiftness" -> new StatusEffectInstance(StatusEffects.SPEED, 20 * 2, 1, true, false, true);
			case "slowness" -> new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 2, 1, true, false, true);
			case "poison" -> new StatusEffectInstance(StatusEffects.POISON, 20 * 6, 2, true, false, true);
			default -> new StatusEffectInstance(StatusEffects.LUCK, 1, 0, true, false, false);
		};
	}

	private void spawnAuraParticles(UnitContext context, PotionChoice choice) {
		var effect = new DustParticleEffect(particleColor(choice), 0.75F);
		var radius = effectRadius(choice);
		for (int index = 0; index < auraParticlePoints(); index++) {
			double angle = (Math.PI * 2.0D * index) / auraParticlePoints();
			double x = context.player().getX() + Math.cos(angle) * radius;
			double z = context.player().getZ() + Math.sin(angle) * radius;
			for (var viewer : context.world().getPlayers()) {
				context.world().spawnParticles(viewer, effect, true, false, x, context.player().getY() + 0.2D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	int particleColor(PotionChoice choice) {
		return switch (choice.potionId()) {
			case "regeneration" -> 0xCD5CAB;
			case "swiftness" -> 0x7CAFC6;
			case "slowness" -> 0x5A6C81;
			case "poison" -> 0x4E9331;
			default -> 0xFFFFFF;
		};
	}

	record PotionChoice(String itemId, String potionId, String displayName, String loreLine) {
	}
}

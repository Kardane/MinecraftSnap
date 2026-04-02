package karn.minecraftsnap.ui;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.MinecraftSnap;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.config.BiomeCatalog;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class WikiGuiService {
	private final TextTemplateResolver textTemplateResolver;
	private final UnitRegistry unitRegistry;
	private final Supplier<BiomeCatalog> biomeCatalogSupplier;
	private final Supplier<MatchPhase> phaseSupplier;
	private final UiSoundService uiSoundService;

	public WikiGuiService(TextTemplateResolver textTemplateResolver, UnitRegistry unitRegistry, Supplier<BiomeCatalog> biomeCatalogSupplier, Supplier<MatchPhase> phaseSupplier) {
		this(textTemplateResolver, unitRegistry, biomeCatalogSupplier, phaseSupplier, null);
	}

	public WikiGuiService(TextTemplateResolver textTemplateResolver, UnitRegistry unitRegistry, Supplier<BiomeCatalog> biomeCatalogSupplier, Supplier<MatchPhase> phaseSupplier, UiSoundService uiSoundService) {
		this.textTemplateResolver = textTemplateResolver;
		this.unitRegistry = unitRegistry;
		this.biomeCatalogSupplier = biomeCatalogSupplier;
		this.phaseSupplier = phaseSupplier;
		this.uiSoundService = uiSoundService;
	}

	public void open(ServerPlayerEntity player, MatchPhase phase) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X1, player, false);
		gui.setTitle(textTemplateResolver.formatUi(textConfig().wikiMainTitle));
		gui.setSlot(0, action(Items.OAK_SIGN, textConfig().wikiOverviewName, textConfig().wikiOverviewLore, null));
		gui.setSlot(2, action(Items.OAK_SIGN, textConfig().wikiCommanderName, textConfig().wikiCommanderLore, null));
		gui.setSlot(4, action(Items.OAK_SIGN, textConfig().wikiUnitButtonName, textConfig().wikiUnitButtonLore, null));
		gui.setSlot(6, action(Items.OAK_SIGN, textConfig().wikiFactionButtonName, textConfig().wikiFactionButtonLore, null));
		gui.setSlot(8, action(Items.OAK_SIGN, textConfig().wikiBiomeButtonName, textConfig().wikiBiomeButtonLore, null));
		gui.open();
	}

	public void openFactionIndex(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
		gui.setTitle(textTemplateResolver.formatUi(textConfig().wikiFactionIndexTitle));
		int slot = 0;
		for (var factionId : List.of(FactionId.VILLAGER, FactionId.MONSTER, FactionId.NETHER)) {
			for (var unit : unitRegistry.allByFaction(factionId)) {
				if (slot >= 54) {
					break;
				}
				gui.setSlot(slot++, item(displayItem(unit), "&f" + unit.displayName(), unitDetailLore(unit)));
			}
		}
		gui.open();
	}

	public void openUnitFactionIndex(ServerPlayerEntity player) {
		openFactionIndex(player);
	}

	public void openBiomeIndex(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.formatUi(textConfig().wikiBiomeIndexTitle));
		int slot = 0;
		for (var biome : biomeCatalogSupplier.get().biomes) {
			if (slot >= 27) {
				break;
			}
			gui.setSlot(slot++, item(displayItem(biome), "&a" + biome.displayName, biomeDetailLore(biome)));
		}
		gui.open();
	}

	static Item displayItem(UnitDefinition unit) {
		return itemById(displayItemId(unit));
	}

	static String displayItemId(UnitDefinition unit) {
		if (unit == null) {
			return "minecraft:barrier";
		}
		var disguise = unit.disguise();
		if (disguise != null && disguise.entityId != null && !disguise.entityId.isBlank()) {
			var entityId = disguise.entityId;
			if ("minecraft:villager".equals(entityId)) {
				return "minecraft:villager_spawn_egg";
			}
			var itemId = entityId + "_spawn_egg";
			if (hasKnownItemId(itemId)) {
				return itemId;
			}
		}
		var fallback = unit.mainHand();
		if (fallback != null && fallback.itemId != null && !fallback.itemId.isBlank()) {
			return fallback.itemId;
		}
		return "minecraft:barrier";
	}

	static Item displayItem(BiomeEntry biome) {
		return itemById(displayItemId(biome));
	}

	static String displayItemId(BiomeEntry biome) {
		if (biome == null) {
			return "minecraft:barrier";
		}
		if (biome.displayItemId != null && !biome.displayItemId.isBlank()) {
			if (hasKnownItemId(biome.displayItemId)) {
				return biome.displayItemId;
			}
		}
		return switch (biome.id) {
			case "desert" -> "minecraft:sand";
			case "swamp" -> "minecraft:lily_pad";
			case "badlands" -> "minecraft:red_sand";
			case "end" -> "minecraft:end_stone";
			case "deep_dark" -> "minecraft:sculk_shrieker";
			case "nether" -> "minecraft:netherrack";
			case "taiga" -> "minecraft:spruce_sapling";
			case "void" -> "minecraft:obsidian";
			case "plain" -> "minecraft:grass_block";
			default -> "minecraft:grass_block";
		};
	}

	private static Item itemById(String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return Items.AIR;
		}
		return switch (itemId) {
			case "minecraft:grass_block" -> Items.GRASS_BLOCK;
			case "minecraft:sand" -> Items.SAND;
			case "minecraft:lily_pad" -> Items.LILY_PAD;
			case "minecraft:red_sand" -> Items.RED_SAND;
			case "minecraft:end_stone" -> Items.END_STONE;
			case "minecraft:obsidian" -> Items.OBSIDIAN;
			case "minecraft:sculk_shrieker" -> Items.SCULK_SHRIEKER;
			case "minecraft:netherrack" -> Items.NETHERRACK;
			case "minecraft:spruce_sapling" -> Items.SPRUCE_SAPLING;
			case "minecraft:villager_spawn_egg" -> Items.VILLAGER_SPAWN_EGG;
			case "minecraft:pillager_spawn_egg" -> Items.PILLAGER_SPAWN_EGG;
			case "minecraft:vindicator_spawn_egg" -> Items.VINDICATOR_SPAWN_EGG;
			case "minecraft:evoker_spawn_egg" -> Items.EVOKER_SPAWN_EGG;
			case "minecraft:snow_golem_spawn_egg" -> Items.SNOW_GOLEM_SPAWN_EGG;
			case "minecraft:iron_golem_spawn_egg" -> Items.IRON_GOLEM_SPAWN_EGG;
			case "minecraft:zombie_spawn_egg" -> Items.ZOMBIE_SPAWN_EGG;
			case "minecraft:skeleton_spawn_egg" -> Items.SKELETON_SPAWN_EGG;
			case "minecraft:slime_spawn_egg" -> Items.SLIME_SPAWN_EGG;
			case "minecraft:creeper_spawn_egg" -> Items.CREEPER_SPAWN_EGG;
			case "minecraft:cave_spider_spawn_egg" -> Items.CAVE_SPIDER_SPAWN_EGG;
			case "minecraft:breeze_spawn_egg" -> Items.BREEZE_SPAWN_EGG;
			case "minecraft:guardian_spawn_egg" -> Items.GUARDIAN_SPAWN_EGG;
			case "minecraft:husk_spawn_egg" -> Items.HUSK_SPAWN_EGG;
			case "minecraft:drowned_spawn_egg" -> Items.DROWNED_SPAWN_EGG;
			case "minecraft:stray_spawn_egg" -> Items.STRAY_SPAWN_EGG;
			case "minecraft:bogged_spawn_egg" -> Items.BOGGED_SPAWN_EGG;
			case "minecraft:wither_skeleton_spawn_egg" -> Items.WITHER_SKELETON_SPAWN_EGG;
			case "minecraft:blaze_spawn_egg" -> Items.BLAZE_SPAWN_EGG;
			case "minecraft:magma_cube_spawn_egg" -> Items.MAGMA_CUBE_SPAWN_EGG;
			case "minecraft:ghast_spawn_egg" -> Items.GHAST_SPAWN_EGG;
			case "minecraft:enderman_spawn_egg" -> Items.ENDERMAN_SPAWN_EGG;
			case "minecraft:piglin_spawn_egg" -> Items.PIGLIN_SPAWN_EGG;
			case "minecraft:piglin_brute_spawn_egg" -> Items.PIGLIN_BRUTE_SPAWN_EGG;
			case "minecraft:zombified_piglin_spawn_egg" -> Items.ZOMBIFIED_PIGLIN_SPAWN_EGG;
			case "minecraft:wind_charge" -> Items.WIND_CHARGE;
			case "minecraft:prismarine_shard" -> Items.PRISMARINE_SHARD;
			case "minecraft:fire_charge" -> Items.FIRE_CHARGE;
			case "minecraft:spider_eye" -> Items.SPIDER_EYE;
			case "minecraft:snowball" -> Items.SNOWBALL;
			case "minecraft:iron_ingot" -> Items.IRON_INGOT;
			default -> Items.AIR;
		};
	}

	private static boolean hasKnownItemId(String itemId) {
		return switch (itemId) {
			case "minecraft:grass_block",
				"minecraft:sand",
				"minecraft:lily_pad",
				"minecraft:red_sand",
				"minecraft:end_stone",
				"minecraft:obsidian",
				"minecraft:sculk_shrieker",
				"minecraft:netherrack",
				"minecraft:spruce_sapling",
				"minecraft:villager_spawn_egg",
				"minecraft:pillager_spawn_egg",
				"minecraft:vindicator_spawn_egg",
				"minecraft:evoker_spawn_egg",
				"minecraft:snow_golem_spawn_egg",
				"minecraft:iron_golem_spawn_egg",
				"minecraft:zombie_spawn_egg",
				"minecraft:skeleton_spawn_egg",
				"minecraft:slime_spawn_egg",
				"minecraft:creeper_spawn_egg",
				"minecraft:cave_spider_spawn_egg",
				"minecraft:breeze_spawn_egg",
				"minecraft:guardian_spawn_egg",
				"minecraft:husk_spawn_egg",
				"minecraft:drowned_spawn_egg",
				"minecraft:stray_spawn_egg",
				"minecraft:bogged_spawn_egg",
				"minecraft:wither_skeleton_spawn_egg",
				"minecraft:blaze_spawn_egg",
				"minecraft:magma_cube_spawn_egg",
				"minecraft:ghast_spawn_egg",
				"minecraft:enderman_spawn_egg",
				"minecraft:piglin_spawn_egg",
				"minecraft:piglin_brute_spawn_egg",
				"minecraft:zombified_piglin_spawn_egg",
				"minecraft:wind_charge",
				"minecraft:prismarine_shard",
				"minecraft:fire_charge",
				"minecraft:spider_eye",
				"minecraft:snowball",
				"minecraft:iron_ingot",
				"minecraft:barrier" -> true;
			default -> false;
		};
	}

	private eu.pb4.sgui.api.elements.GuiElementInterface action(Item item, String name, List<String> lore, Runnable callback) {
		var builder = new GuiElementBuilder(item)
			.setName(textTemplateResolver.formatUi(name))
			.setLore(lore.stream().map(textTemplateResolver::formatUi).toList());
		if (callback != null) {
			builder.setCallback((index, clickType, actionType, gui) -> {
				if (uiSoundService != null && gui.getPlayer() instanceof ServerPlayerEntity player) {
					uiSoundService.playUiClick(player);
				}
				callback.run();
			});
		}
		return builder.build();
	}

	private eu.pb4.sgui.api.elements.GuiElementInterface item(Item item, String name, List<String> lore) {
		return new GuiElementBuilder(item)
			.setName(textTemplateResolver.formatUi(name))
			.setLore(lore.stream().map(textTemplateResolver::formatUi).toList())
			.build();
	}

	private List<String> unitDetailLore(UnitDefinition unit) {
		var lines = new ArrayList<String>();
		lines.add("&7체력: &c" + (int) unit.maxHealth());
		lines.add("&7이동속도 배율: &b" + unit.moveSpeedScale());
		lines.add("&7코스트: &b" + unit.cost());
		if (unit.hasActiveSkill()) {
			lines.add("&8스킬: &f" + unit.abilityName());
			lines.add("&8스킬 쿨다운: &f" + unit.abilityCooldownSeconds() + "초");
		}
		lines.addAll(unit.descriptionLines());
		return lines;
	}

	static List<String> biomeDetailLore(BiomeEntry biome) {
		var lines = new ArrayList<String>();
		lines.add("&7대표 바이옴: &f" + biome.minecraftBiomeId);
		lines.addAll(biome.descriptionLines);
		lines.addAll(biome.revealMessages);
		return lines;
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}

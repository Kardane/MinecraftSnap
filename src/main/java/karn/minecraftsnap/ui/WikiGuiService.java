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
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

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
		var biomes = biomeCatalogSupplier.get() == null || biomeCatalogSupplier.get().biomes == null
			? List.<BiomeEntry>of()
			: biomeCatalogSupplier.get().biomes;
		var gui = new SimpleGui(screenTypeForBiomeCount(biomes.size()), player, false);
		gui.setTitle(textTemplateResolver.formatUi(textConfig().wikiBiomeIndexTitle));
		int slot = 0;
		for (var biome : biomes) {
			if (slot >= gui.getSize()) {
				break;
			}
			gui.setSlot(slot++, item(displayItem(biome), "&a" + biome.displayName, biomeDetailLore(biome)));
		}
		gui.open();
	}

	private ScreenHandlerType<?> screenTypeForBiomeCount(int biomeCount) {
		var rows = Math.max(1, Math.min(6, (Math.max(0, biomeCount) + 8) / 9));
		return switch (rows) {
			case 1 -> ScreenHandlerType.GENERIC_9X1;
			case 2 -> ScreenHandlerType.GENERIC_9X2;
			case 3 -> ScreenHandlerType.GENERIC_9X3;
			case 4 -> ScreenHandlerType.GENERIC_9X4;
			case 5 -> ScreenHandlerType.GENERIC_9X5;
			default -> ScreenHandlerType.GENERIC_9X6;
		};
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
		try {
			var item = Registries.ITEM.get(Identifier.of(itemId));
			return item == Items.AIR ? Items.AIR : item;
		} catch (Exception ignored) {
			return Items.AIR;
		}
	}

	private static boolean hasKnownItemId(String itemId) {
		return itemById(itemId) != Items.AIR;
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

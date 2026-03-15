package karn.minecraftsnap.ui;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.MinecraftSnap;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.config.BiomeCatalog;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.FactionSpec;
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
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format(textConfig().wikiMainTitle));
		gui.setSlot(10, action(Items.BOOK, textConfig().wikiOverviewName, List.of(
			textConfig().wikiOverviewPhaseLoreTemplate.replace("{phase}", phase.getDisplayName()),
			textConfig().wikiOverviewRuleLore,
			textConfig().wikiOverviewHintLore
		), null));
		gui.setSlot(12, action(Items.EMERALD, textConfig().wikiFactionButtonName, List.of(
			textConfig().wikiFactionButtonLore1,
			textConfig().wikiFactionButtonLore2
		), () -> openFactionIndex(player)));
		gui.setSlot(14, action(Items.IRON_SWORD, textConfig().wikiUnitButtonName, List.of(
			textConfig().wikiUnitButtonLore1,
			textConfig().wikiUnitButtonLore2
		), () -> openUnitFactionIndex(player)));
		gui.setSlot(16, action(Items.GRASS_BLOCK, textConfig().wikiBiomeButtonName, List.of(
			textConfig().wikiBiomeButtonLore1,
			textConfig().wikiBiomeButtonLore2
		), () -> openBiomeIndex(player)));
		gui.open();
	}

	private void openFactionIndex(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format(textConfig().wikiFactionIndexTitle));
		gui.setSlot(11, factionAction(player, FactionId.VILLAGER, Items.EMERALD));
		gui.setSlot(13, factionAction(player, FactionId.MONSTER, Items.IRON_SWORD));
		gui.setSlot(15, factionAction(player, FactionId.NETHER, Items.BLAZE_ROD));
		gui.setSlot(18, homeAction(player));
		gui.open();
	}

	private void openFactionDetail(ServerPlayerEntity player, FactionId factionId) {
		var config = unitRegistry.getFactionSpec(factionId);
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&f" + (config == null ? factionId.name() : config.displayName())));
		var lines = new ArrayList<String>();
		if (config != null) {
			lines.addAll(config.summaryLines());
			lines.add(textConfig().factionSelectionCaptainSkillLoreTemplate.replace("{skill}", config.captainSkillName()));
			lines.addAll(config.captainSkillDescriptionLines());
		}
		gui.setSlot(13, action(iconOf(factionId), textConfig().wikiSummaryName, lines, null));
		gui.setSlot(18, homeAction(player));
		gui.setSlot(22, action(Items.ARROW, textConfig().wikiBackName, List.of(textConfig().wikiBackFactionLore), () -> openFactionIndex(player)));
		gui.open();
	}

	private void openUnitFactionIndex(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format(textConfig().wikiUnitIndexTitle));
		gui.setSlot(11, action(Items.EMERALD, textConfig().wikiVillagerCategoryName, List.of(textConfig().wikiVillagerCategoryLore), () -> openUnitList(player, FactionId.VILLAGER)));
		gui.setSlot(13, action(Items.IRON_SWORD, textConfig().wikiMonsterCategoryName, List.of(textConfig().wikiMonsterCategoryLore), () -> openUnitList(player, FactionId.MONSTER)));
		gui.setSlot(15, action(Items.BLAZE_ROD, textConfig().wikiNetherCategoryName, List.of(textConfig().wikiNetherCategoryLore), () -> openUnitList(player, FactionId.NETHER)));
		gui.setSlot(18, homeAction(player));
		gui.open();
	}

	private void openUnitList(ServerPlayerEntity player, FactionId factionId) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format(textConfig().wikiFactionUnitListTitleTemplate.replace("{faction}", unitRegistry.getFactionSpec(factionId).displayName())));
		int slot = 9;
		for (var unit : unitRegistry.allByFaction(factionId)) {
			if (slot >= 27) {
				break;
			}
			gui.setSlot(slot++, action(unit.mainHandItem(), "&f" + unit.displayName(), unitLore(unit), () -> openUnitDetail(player, factionId, unit)));
		}
		gui.setSlot(18, homeAction(player));
		gui.setSlot(22, action(Items.ARROW, textConfig().wikiBackName, List.of(textConfig().wikiBackUnitFactionLore), () -> openUnitFactionIndex(player)));
		gui.open();
	}

	private void openUnitDetail(ServerPlayerEntity player, FactionId factionId, UnitDefinition unit) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&f" + unit.displayName()));
		gui.setSlot(13, action(unit.mainHandItem(), "&f" + unit.displayName(), unitDetailLore(unit), null));
		gui.setSlot(18, homeAction(player));
		gui.setSlot(22, action(Items.ARROW, textConfig().wikiBackName, List.of(textConfig().wikiBackUnitListLore), () -> openUnitList(player, factionId)));
		gui.open();
	}

	private void openBiomeIndex(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format(textConfig().wikiBiomeIndexTitle));
		int slot = 9;
		for (var biome : biomeCatalogSupplier.get().biomes) {
			if (slot >= 27) {
				break;
			}
			gui.setSlot(slot++, action(Items.GRASS_BLOCK, "&a" + biome.displayName, biomeSummaryLore(biome), () -> openBiomeDetail(player, biome)));
		}
		gui.setSlot(18, homeAction(player));
		gui.open();
	}

	private void openBiomeDetail(ServerPlayerEntity player, BiomeEntry biome) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&a" + biome.displayName));
		gui.setSlot(13, action(Items.GRASS_BLOCK, "&a" + biome.displayName, biomeDetailLore(biome), null));
		gui.setSlot(18, homeAction(player));
		gui.setSlot(22, action(Items.ARROW, textConfig().wikiBackName, List.of(textConfig().wikiBackBiomeLore), () -> openBiomeIndex(player)));
		gui.open();
	}

	private eu.pb4.sgui.api.elements.GuiElementInterface homeAction(ServerPlayerEntity player) {
		return action(Items.COMPASS, textConfig().wikiHomeName, List.of(textConfig().wikiHomeLore), () -> open(player, phaseSupplier.get()));
	}

	private eu.pb4.sgui.api.elements.GuiElementInterface factionAction(ServerPlayerEntity player, FactionId factionId, Item item) {
		var config = unitRegistry.getFactionSpec(factionId);
		var lore = config == null ? List.of(textConfig().wikiNoDataLore) : config.summaryLines();
		return action(item, "&f" + (config == null ? factionId.name() : config.displayName()), lore, () -> openFactionDetail(player, factionId));
	}

	private eu.pb4.sgui.api.elements.GuiElementInterface action(Item item, String name, List<String> lore, Runnable callback) {
		var builder = new GuiElementBuilder(item)
			.setName(textTemplateResolver.format(name))
			.setLore(lore.stream().map(textTemplateResolver::format).toList());
		if (callback != null) {
			builder.glow();
			builder.setCallback((index, clickType, actionType, gui) -> {
				if (uiSoundService != null && gui.getPlayer() instanceof ServerPlayerEntity player) {
					uiSoundService.playUiClick(player);
				}
				callback.run();
			});
		}
		return builder.build();
	}

	private List<String> unitLore(UnitDefinition unit) {
		var lines = new ArrayList<String>();
		lines.add("&7코스트: &b" + unit.cost());
		lines.addAll(unit.descriptionLines());
		return lines;
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

	private List<String> biomeSummaryLore(BiomeEntry biome) {
		var lines = new ArrayList<String>();
		lines.add("&7대표 바이옴: &f" + biome.minecraftBiomeId);
		lines.addAll(biome.descriptionLines);
		return lines;
	}

	private List<String> biomeDetailLore(BiomeEntry biome) {
		var lines = new ArrayList<String>();
		lines.add("&7대표 바이옴: &f" + biome.minecraftBiomeId);
		lines.addAll(biome.descriptionLines);
		lines.addAll(biome.revealMessages);
		if (biome.pulseIntervalSeconds > 0) {
			lines.add("&8주기 알림: &f" + biome.pulseIntervalSeconds + "초");
			lines.addAll(biome.pulseMessages);
		}
		return lines;
	}

	private Item iconOf(FactionId factionId) {
		return switch (factionId) {
			case VILLAGER -> Items.EMERALD;
			case MONSTER -> Items.IRON_SWORD;
			case NETHER -> Items.BLAZE_ROD;
		};
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}

package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.config.BiomeCatalog;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.FactionConfigFile;
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

	public WikiGuiService(TextTemplateResolver textTemplateResolver, UnitRegistry unitRegistry, Supplier<BiomeCatalog> biomeCatalogSupplier) {
		this.textTemplateResolver = textTemplateResolver;
		this.unitRegistry = unitRegistry;
		this.biomeCatalogSupplier = biomeCatalogSupplier;
	}

	public void open(ServerPlayerEntity player, MatchPhase phase) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&6MCsnap 위키"));
		gui.setSlot(10, action(Items.BOOK, "&e게임 개요", List.of(
			"&f현재 페이즈: &a" + phase.getDisplayName(),
			"&79분 동안 3개 라인의 점령 점수 경쟁",
			"&7Shift+F로 상황별 GUI 열기"
		), null));
		gui.setSlot(12, action(Items.EMERALD, "&a팩션", List.of(
			"&7주민&우민 / 몬스터 / 네더",
			"&7요약과 사령관 스킬 설명 보기"
		), () -> openFactionIndex(player)));
		gui.setSlot(14, action(Items.IRON_SWORD, "&c유닛", List.of(
			"&7팩션별 유닛 목록과 상세 보기",
			"&7전직 유닛도 포함"
		), () -> openUnitFactionIndex(player)));
		gui.setSlot(16, action(Items.GRASS_BLOCK, "&2바이옴", List.of(
			"&7출현 가능한 바이옴 목록",
			"&7공개 메시지와 설명 확인"
		), () -> openBiomeIndex(player)));
		gui.open();
	}

	private void openFactionIndex(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&a팩션 도감"));
		gui.setSlot(11, factionAction(player, FactionId.VILLAGER, Items.EMERALD));
		gui.setSlot(13, factionAction(player, FactionId.MONSTER, Items.IRON_SWORD));
		gui.setSlot(15, factionAction(player, FactionId.NETHER, Items.BLAZE_ROD));
		gui.open();
	}

	private void openFactionDetail(ServerPlayerEntity player, FactionId factionId) {
		var config = unitRegistry.getFactionConfig(factionId);
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&f" + (config == null ? factionId.name() : config.displayName)));
		var lines = new ArrayList<String>();
		if (config != null) {
			lines.addAll(config.summaryLines);
			lines.add("&8사령관 스킬: &d" + config.captainSkill.name);
			lines.addAll(config.captainSkill.descriptionLines);
		}
		gui.setSlot(13, action(iconOf(factionId), "&f개요", lines, null));
		gui.setSlot(22, action(Items.ARROW, "&e뒤로", List.of("&7팩션 목록으로 복귀"), () -> openFactionIndex(player)));
		gui.open();
	}

	private void openUnitFactionIndex(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&c유닛 도감"));
		gui.setSlot(11, action(Items.EMERALD, "&a주민&우민", List.of("&7주민 계열 유닛 보기"), () -> openUnitList(player, FactionId.VILLAGER)));
		gui.setSlot(13, action(Items.IRON_SWORD, "&c몬스터", List.of("&7몬스터 및 전직 유닛 보기"), () -> openUnitList(player, FactionId.MONSTER)));
		gui.setSlot(15, action(Items.BLAZE_ROD, "&6네더", List.of("&7네더 유닛 보기"), () -> openUnitList(player, FactionId.NETHER)));
		gui.open();
	}

	private void openUnitList(ServerPlayerEntity player, FactionId factionId) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&f" + unitRegistry.getFactionConfig(factionId).displayName + " 유닛"));
		int slot = 9;
		for (var unit : unitRegistry.allByFaction(factionId)) {
			if (slot >= 27) {
				break;
			}
			gui.setSlot(slot++, action(unit.mainHandItem(), "&f" + unit.displayName(), unitLore(unit), () -> openUnitDetail(player, factionId, unit)));
		}
		gui.setSlot(22, action(Items.ARROW, "&e뒤로", List.of("&7유닛 분류로 복귀"), () -> openUnitFactionIndex(player)));
		gui.open();
	}

	private void openUnitDetail(ServerPlayerEntity player, FactionId factionId, UnitDefinition unit) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&f" + unit.displayName()));
		gui.setSlot(13, action(unit.mainHandItem(), "&f" + unit.displayName(), unitDetailLore(unit), null));
		gui.setSlot(22, action(Items.ARROW, "&e뒤로", List.of("&7이전 유닛 목록으로 복귀"), () -> openUnitList(player, factionId)));
		gui.open();
	}

	private void openBiomeIndex(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&2바이옴 도감"));
		int slot = 9;
		for (var biome : biomeCatalogSupplier.get().biomes) {
			if (slot >= 27) {
				break;
			}
			gui.setSlot(slot++, action(Items.GRASS_BLOCK, "&a" + biome.displayName, biomeSummaryLore(biome), () -> openBiomeDetail(player, biome)));
		}
		gui.open();
	}

	private void openBiomeDetail(ServerPlayerEntity player, BiomeEntry biome) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&a" + biome.displayName));
		gui.setSlot(13, action(Items.GRASS_BLOCK, "&a" + biome.displayName, biomeDetailLore(biome), null));
		gui.setSlot(22, action(Items.ARROW, "&e뒤로", List.of("&7바이옴 목록으로 복귀"), () -> openBiomeIndex(player)));
		gui.open();
	}

	private eu.pb4.sgui.api.elements.GuiElementInterface factionAction(ServerPlayerEntity player, FactionId factionId, Item item) {
		var config = unitRegistry.getFactionConfig(factionId);
		var lore = config == null ? List.of("&7데이터 없음") : config.summaryLines;
		return action(item, "&f" + (config == null ? factionId.name() : config.displayName), lore, () -> openFactionDetail(player, factionId));
	}

	private eu.pb4.sgui.api.elements.GuiElementInterface action(Item item, String name, List<String> lore, Runnable callback) {
		var builder = new GuiElementBuilder(item)
			.setName(textTemplateResolver.format(name))
			.setLore(lore.stream().map(textTemplateResolver::format).toList());
		if (callback != null) {
			builder.glow();
			builder.setCallback((index, clickType, actionType, gui) -> callback.run());
		}
		return builder.build();
	}

	private List<String> unitLore(UnitDefinition unit) {
		var lines = new ArrayList<String>();
		lines.add("&7코스트: &b" + unit.cost());
		lines.add("&7생성 쿨: &e" + unit.spawnCooldownSeconds() + "초");
		lines.addAll(unit.descriptionLines());
		return lines;
	}

	private List<String> unitDetailLore(UnitDefinition unit) {
		var lines = new ArrayList<String>();
		lines.add("&7체력: &c" + (int) unit.maxHealth());
		lines.add("&7이동속도 배율: &b" + unit.moveSpeedScale());
		lines.add("&7코스트: &b" + unit.cost());
		lines.add("&7생성 쿨: &e" + unit.spawnCooldownSeconds() + "초");
		if (unit.abilityItem() != null) {
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
}

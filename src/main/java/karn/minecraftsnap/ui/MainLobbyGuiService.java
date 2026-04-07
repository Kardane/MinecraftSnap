package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.PlayerStats;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MainLobbyGuiService {
	private final TextTemplateResolver textTemplateResolver;
	private final Supplier<StatsRepository> statsRepositorySupplier;
	private final Supplier<MatchPhase> phaseSupplier;
	private final UiSoundService uiSoundService;
	private final Consumer<ServerPlayerEntity> rulesOpener;
	private final Consumer<ServerPlayerEntity> unitIndexOpener;
	private final Consumer<ServerPlayerEntity> biomeIndexOpener;
	private final Consumer<ServerPlayerEntity> adminToolsOpener;

	public MainLobbyGuiService(
		TextTemplateResolver textTemplateResolver,
		Supplier<StatsRepository> statsRepositorySupplier,
		Supplier<MatchPhase> phaseSupplier,
		UiSoundService uiSoundService,
		Consumer<ServerPlayerEntity> rulesOpener,
		Consumer<ServerPlayerEntity> unitIndexOpener,
		Consumer<ServerPlayerEntity> biomeIndexOpener,
		Consumer<ServerPlayerEntity> adminToolsOpener
	) {
		this.textTemplateResolver = textTemplateResolver;
		this.statsRepositorySupplier = statsRepositorySupplier;
		this.phaseSupplier = phaseSupplier;
		this.uiSoundService = uiSoundService;
		this.rulesOpener = rulesOpener;
		this.unitIndexOpener = unitIndexOpener;
		this.biomeIndexOpener = biomeIndexOpener;
		this.adminToolsOpener = adminToolsOpener;
	}

	public void open(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X1, player, false);
		gui.setTitle(textTemplateResolver.formatUi(textConfig().mainLobbyTitle));
		gui.setSlot(0, captainPreferenceButton(player, gui));
		gui.setSlot(1, statsButton(player, gui));
		gui.setSlot(3, action(gui, Items.KNOWLEDGE_BOOK, textConfig().mainLobbyRulesName, textConfig().mainLobbyRulesLore, () -> rulesOpener.accept(player)));
		gui.setSlot(4, action(gui, Items.CREEPER_SPAWN_EGG, textConfig().mainLobbyUnitGuideName, textConfig().mainLobbyUnitGuideLore, () -> unitIndexOpener.accept(player)));
		gui.setSlot(5, action(gui, Items.GRASS_BLOCK, textConfig().mainLobbyBiomeGuideName, textConfig().mainLobbyBiomeGuideLore, () -> biomeIndexOpener.accept(player)));
		if (player.hasPermissionLevel(4)) {
			gui.setSlot(7, action(gui, Items.COMMAND_BLOCK, textConfig().mainLobbyAdminToolsName, List.of(), () -> adminToolsOpener.accept(player)));
		}
		gui.setSlot(8, action(gui, Items.BARRIER, textConfig().mainLobbyCloseName, List.of(), gui::close));
		gui.open();
	}

	static List<String> personalStatsLore(TextConfigFile textConfig, PlayerStats stats) {
		return resolveLoreLines(textConfig.mainLobbyStatsLore, Map.ofEntries(
			Map.entry("{score}", Integer.toString(stats.ladder)),
			Map.entry("{win_rate}", percentText(stats)),
			Map.entry("{games}", Integer.toString(totalGames(stats))),
			Map.entry("{captain_games}", Integer.toString(stats.captainGames)),
			Map.entry("{average_capture}", decimalText(averageCapture(stats))),
			Map.entry("{kd}", decimalText(kd(stats))),
			Map.entry("{kills}", Integer.toString(stats.kills)),
			Map.entry("{deaths}", Integer.toString(stats.deaths)),
			Map.entry("{advances}", Integer.toString(stats.advanceCount)),
			Map.entry("{assists}", Integer.toString(stats.assists)),
			Map.entry("{damage}", decimalText(stats.totalDamageDealt)),
			Map.entry("{healing}", decimalText(stats.totalHealingDone)),
			Map.entry("{play_time}", durationText(stats.playTimeSeconds))
		));
	}

	static List<String> resolveLoreLines(List<String> templates, Map<String, String> placeholders) {
		if (templates == null || templates.isEmpty()) {
			return List.of();
		}
		return templates.stream()
			.map(line -> {
				var resolved = line;
				for (var entry : placeholders.entrySet()) {
					resolved = resolved.replace(entry.getKey(), entry.getValue());
				}
				return resolved;
			})
			.toList();
	}

	static String toggleStateText(TextConfigFile textConfig, String preference) {
		return switch (normalizedCaptainPreference(preference)) {
			case "captain" -> textConfig.mainLobbyCaptainPreferenceEnabledLore;
			case "avoid_captain" -> textConfig.mainLobbyCaptainPreferenceAvoidedLore;
			default -> textConfig.mainLobbyCaptainPreferenceNeutralLore;
		};
	}

	static String nextCaptainPreference(String preference) {
		return switch (normalizedCaptainPreference(preference)) {
			case "captain" -> "none";
			case "avoid_captain" -> "captain";
			default -> "avoid_captain";
		};
	}

	private static String normalizedCaptainPreference(String preference) {
		if ("captain".equalsIgnoreCase(preference)) {
			return "captain";
		}
		if ("avoid_captain".equalsIgnoreCase(preference)) {
			return "avoid_captain";
		}
		return "none";
	}

	static int totalGames(PlayerStats stats) {
		return stats.wins + stats.losses;
	}

	static double kd(PlayerStats stats) {
		return stats.deaths <= 0 ? stats.kills : (double) stats.kills / (double) stats.deaths;
	}

	static double averageCapture(PlayerStats stats) {
		int games = totalGames(stats);
		return games <= 0 ? 0.0 : (double) stats.captures / (double) games;
	}

	static String percentText(PlayerStats stats) {
		int games = totalGames(stats);
		if (games <= 0) {
			return "0.0%";
		}
		return decimalText(((double) stats.wins * 100.0) / (double) games) + "%";
	}

	private GuiElementInterface captainPreferenceButton(ServerPlayerEntity player, SimpleGui gui) {
		var repository = statsRepositorySupplier.get();
		var stats = repository.getOrCreate(player.getUuid(), player.getName().getString());
		return action(
			gui,
			Items.GOLDEN_HELMET,
			textConfig().mainLobbyCaptainPreferenceName,
			resolveLoreLines(textConfig().mainLobbyCaptainPreferenceLore, Map.of("{state}", toggleStateText(textConfig(), stats.preference))),
			() -> {
				repository.setPreference(player.getUuid(), player.getName().getString(), nextCaptainPreference(stats.preference));
				if (uiSoundService != null) {
					uiSoundService.playUiConfirm(player);
				}
				open(player);
			},
			false
		);
	}

	private GuiElementInterface statsButton(ServerPlayerEntity player, SimpleGui gui) {
		var stats = statsRepositorySupplier.get().getOrCreate(player.getUuid(), player.getName().getString());
		return action(gui, Items.BOOK, textConfig().mainLobbyStatsName, personalStatsLore(textConfig(), stats), () -> {
		});
	}

	private GuiElementInterface item(Item item, String name, List<String> lore) {
		return new GuiElementBuilder(item)
			.setName(textTemplateResolver.formatUi(name))
			.setLore(lore.stream().map(textTemplateResolver::formatUi).toList())
			.build();
	}

	private GuiElementInterface action(SimpleGui gui, Item item, String name, List<String> lore, Runnable callback) {
		return action(gui, item, name, lore, callback, true);
	}

	private GuiElementInterface action(SimpleGui gui, Item item, String name, List<String> lore, Runnable callback, boolean playClickSound) {
		return new GuiElementBuilder(item)
			.setName(textTemplateResolver.formatUi(name))
			.setLore(lore.stream().map(textTemplateResolver::formatUi).toList())
			.setCallback((index, clickType, actionType, slotGui) -> {
				if (playClickSound && uiSoundService != null) {
					uiSoundService.playUiClick(gui.getPlayer());
				}
				callback.run();
			})
			.build();
	}

	private static String decimalText(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static String durationText(int totalSeconds) {
		int seconds = Math.max(0, totalSeconds);
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int remainingSeconds = seconds % 60;
		return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, remainingSeconds);
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}

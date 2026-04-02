package karn.minecraftsnap.ui;

import karn.minecraftsnap.audio.UiSoundService;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.FactionSpec;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.function.Consumer;

public class FactionSelectionGuiService {
	private final TextTemplateResolver textTemplateResolver;
	private final UnitRegistry unitRegistry;
	private final UiSoundService uiSoundService;

	public FactionSelectionGuiService(TextTemplateResolver textTemplateResolver, UnitRegistry unitRegistry) {
		this(textTemplateResolver, unitRegistry, null);
	}

	public FactionSelectionGuiService(TextTemplateResolver textTemplateResolver, UnitRegistry unitRegistry, UiSoundService uiSoundService) {
		this.textTemplateResolver = textTemplateResolver;
		this.unitRegistry = unitRegistry;
		this.uiSoundService = uiSoundService;
	}

	public void open(ServerPlayerEntity player, TeamId teamId, FactionId selectedFaction, Consumer<FactionId> onSelect) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.formatUi(teamId == TeamId.RED ? textConfig().factionSelectionRedTitle : textConfig().factionSelectionBlueTitle));
		gui.setSlot(11, buildFactionSlot(gui, FactionId.VILLAGER, Items.EMERALD, selectedFaction, onSelect));
		gui.setSlot(13, buildFactionSlot(gui, FactionId.MONSTER, Items.IRON_SWORD, selectedFaction, onSelect));
		gui.setSlot(15, buildFactionSlot(gui, FactionId.NETHER, Items.BLAZE_ROD, selectedFaction, onSelect));
		gui.open();
	}

	private GuiElementInterface buildFactionSlot(
		SimpleGui gui,
		FactionId factionId,
		net.minecraft.item.Item item,
		FactionId selectedFaction,
		Consumer<FactionId> onSelect
	) {
		var config = unitRegistry.getFactionSpec(factionId);
		var name = config == null ? factionId.name() : config.displayName();
		var builder = new GuiElementBuilder(item)
			.setName(textTemplateResolver.formatUi("&f" + name))
			.setLore(buildLore(config, selectedFaction == factionId))
			.setCallback((index, clickType, action, slotGui) -> {
				if (uiSoundService != null) {
					uiSoundService.playUiConfirm(gui.getPlayer());
				}
				onSelect.accept(factionId);
				gui.close();
			});

		return builder.build();
	}

	private List<net.minecraft.text.Text> buildLore(FactionSpec config, boolean selected) {
		var lines = new java.util.ArrayList<String>();
		if (config != null) {
			lines.addAll(config.summaryLines());
			if (config.captainSkillName() != null && !config.captainSkillName().isBlank()) {
				lines.add(textConfig().factionSelectionCaptainSkillLoreTemplate.replace("{skill}", config.captainSkillName()));
			}
		}
		lines.add(selected ? textConfig().factionSelectionSelectedLore : textConfig().factionSelectionClickLore);
		return lines(lines.toArray(String[]::new));
	}

	private List<net.minecraft.text.Text> lines(String... values) {
		return java.util.Arrays.stream(values)
			.map(textTemplateResolver::formatUi)
			.toList();
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}

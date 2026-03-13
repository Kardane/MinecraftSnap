package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.UnitAbilityService;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.server.MinecraftServer;

public class UnitHudService {
	private final MatchManager matchManager;
	private final UnitRegistry unitRegistry;
	private final UnitAbilityService unitAbilityService;
	private final TextTemplateResolver textTemplateResolver;

	public UnitHudService(
		MatchManager matchManager,
		UnitRegistry unitRegistry,
		UnitAbilityService unitAbilityService,
		TextTemplateResolver textTemplateResolver
	) {
		this.matchManager = matchManager;
		this.unitRegistry = unitRegistry;
		this.unitAbilityService = unitAbilityService;
		this.textTemplateResolver = textTemplateResolver;
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || matchManager.getServerTicks() % 10L != 0L) {
			return;
		}
		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() == null || player.isSpectator()) {
				continue;
			}
			var definition = unitRegistry.get(state.getCurrentUnitId());
			if (definition == null) {
				continue;
			}
			var cooldown = unitAbilityService.remainingUnitCooldownSeconds(player.getUuid(), matchManager.getServerTicks());
			player.sendMessage(textTemplateResolver.format(formatActionBar(definition, cooldown, systemConfig)), true);
		}
	}

	static String formatActionBar(UnitDefinition definition, int remainingCooldownSeconds, SystemConfig systemConfig) {
		var displayConfig = systemConfig == null ? new SystemConfig.DisplayConfig() : systemConfig.display;
		var skillName = definition == null || definition.abilityName() == null || definition.abilityName().isBlank()
			? displayConfig.unitHudFallbackSkillName
			: definition.abilityName();
		var unitName = definition == null ? displayConfig.unitHudUnknownUnitName : definition.displayName();
		var cooldown = remainingCooldownSeconds <= 0 ? displayConfig.unitHudReadyMessage : "&e" + remainingCooldownSeconds + "초";
		return displayConfig.unitHudTemplate
			.replace("{unit}", unitName)
			.replace("{skill}", skillName)
			.replace("{cooldown}", cooldown);
	}
}

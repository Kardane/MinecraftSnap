package karn.minecraftsnap.command;

import karn.minecraftsnap.StyledChatSupport;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;

public class TeamChatService {
	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;

	public TeamChatService(MatchManager matchManager, TextTemplateResolver textTemplateResolver) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
	}

	public boolean handleChatMessage(SignedMessage message, ServerPlayerEntity sender, Object ignoredParams, SystemConfig systemConfig) {
		var content = TeamChatParser.extractContent(message.getSignedContent());
		if (content == null) {
			return true;
		}

		var state = matchManager.getPlayerState(sender.getUuid());
		if (!state.canUseTeamChat()) {
			sender.sendMessage(textTemplateResolver.format(systemConfig.teamChat.noTeamMessage), false);
			return false;
		}

		if (content.isBlank()) {
			sender.sendMessage(textTemplateResolver.format(systemConfig.teamChat.emptyMessage), false);
			return false;
		}

		var renderedMessage = createTeamChatMessage(sender, state, content, systemConfig);
		for (var recipient : matchManager.getOnlineTeamMembers(state.getTeamId())) {
			recipient.sendMessage(renderedMessage, false);
		}

		return false;
	}

	private Text createTeamChatMessage(ServerPlayerEntity sender, PlayerMatchState state, String content, SystemConfig systemConfig) {
		var template = systemConfig.teamChat.format;
		var marker = "{message}";
		var markerIndex = template.indexOf(marker);
		var prefix = markerIndex >= 0 ? template.substring(0, markerIndex) : template + " ";
		var suffix = markerIndex >= 0 ? template.substring(markerIndex + marker.length()) : "";

		var placeholders = Map.of(
			"{team}", state.getTeamId().getDisplayName(),
			"{role}", state.getRoleType().getDisplayName(),
			"{player}", sender.getName().getString()
		);

		return Text.empty()
			.append(textTemplateResolver.format(prefix, placeholders))
			.append(StyledChatSupport.format(sender.getCommandSource(), content))
			.append(textTemplateResolver.format(suffix, placeholders));
	}
}

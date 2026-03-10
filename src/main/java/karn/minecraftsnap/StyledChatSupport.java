package karn.minecraftsnap;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class StyledChatSupport {
	private static final String STYLED_CHAT_MOD_ID = "styledchat";
	private static final String STYLED_CHAT_UTILS_CLASS = "eu.pb4.styledchat.StyledChatUtils";

	private static Method formatForMethod;
	private static boolean available;

	private StyledChatSupport() {
	}

	public static void initialize() {
		if (!FabricLoader.getInstance().isModLoaded(STYLED_CHAT_MOD_ID)) {
			MinecraftSnap.LOGGER.info("[{}] StyledChat 미탑재 상태", MinecraftSnap.MOD_ID);
			available = false;
			formatForMethod = null;
			return;
		}

		try {
			var utilsClass = Class.forName(STYLED_CHAT_UTILS_CLASS);
			formatForMethod = utilsClass.getMethod("formatFor", ServerCommandSource.class, String.class);
			available = true;
			MinecraftSnap.LOGGER.info("[{}] StyledChat 연동 활성화", MinecraftSnap.MOD_ID);
		} catch (ClassNotFoundException | NoSuchMethodException exception) {
			available = false;
			formatForMethod = null;
			MinecraftSnap.LOGGER.warn("[{}] StyledChat API 연결 실패", MinecraftSnap.MOD_ID, exception);
		}
	}

	public static boolean isAvailable() {
		return available && formatForMethod != null;
	}

	public static Text format(ServerCommandSource source, String input) {
		if (!isAvailable()) {
			return Text.literal(input);
		}

		try {
			return (Text) formatForMethod.invoke(null, source, input);
		} catch (IllegalAccessException | InvocationTargetException exception) {
			MinecraftSnap.LOGGER.warn("[{}] StyledChat 포맷 적용 실패", MinecraftSnap.MOD_ID, exception);
			return Text.literal(input);
		}
	}
}

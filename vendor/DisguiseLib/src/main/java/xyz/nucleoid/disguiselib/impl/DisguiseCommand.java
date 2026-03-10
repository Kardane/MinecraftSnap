package xyz.nucleoid.disguiselib.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.command.argument.EntityArgumentType.entities;
import static net.minecraft.command.suggestion.SuggestionProviders.SUMMONABLE_ENTITIES;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DisguiseCommand {

	private static final Text NO_PERMISSION_ERROR = Text.translatable("commands.help.failed");

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess commandRegistryAccess,
			CommandManager.RegistrationEnvironment registrationEnvironment) {
		dispatcher.register(literal("disguise")
				.requires(Permissions.require("disguiselib.disguise", 2))
				.then(argument("target", entities())
						.then(literal("as")
								.then(argument("disguise",
										new RegistryEntryReferenceArgumentType<>(commandRegistryAccess,
												RegistryKeys.ENTITY_TYPE))
										.suggests(SuggestionProviders.cast(SUMMONABLE_ENTITIES))
										.executes(DisguiseCommand::setDisguise)
										.then(argument("nbt", NbtCompoundArgumentType.nbtCompound())
												.executes(DisguiseCommand::setDisguise))))
						.then(literal("clear").executes(DisguiseCommand::clearDisguise))));
	}

	private static int clearDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "target");
		ServerCommandSource src = ctx.getSource();
		AtomicInteger successCount = new AtomicInteger(0);

		entities.forEach(entity -> {
			if (((EntityDisguise) entity).isDisguised()) {
				((EntityDisguise) entity).removeDisguise();
				successCount.incrementAndGet();
			}
		});

		int count = successCount.get();
		if (count > 0) {
			src.sendFeedback(() -> Text.literal("Cleared disguise from " + count + " entity(ies)")
					.formatted(Formatting.GREEN), true);
			return count;
		} else {
			src.sendError(Text.literal("No disguised entities found").formatted(Formatting.RED));
			return 0;
		}
	}

	private static int setDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "target");
		ServerCommandSource src = ctx.getSource();
		var type = RegistryEntryReferenceArgumentType.getRegistryEntry(ctx, "disguise", RegistryKeys.ENTITY_TYPE);
		var disguiseId = Registries.ENTITY_TYPE.getId(type.value());

		// 플레이어 타입은 허용하지 않음
		if (type.value() == EntityType.PLAYER) {
			src.sendError(Text.literal("Disguising as player is not supported").formatted(Formatting.RED));
			return 0;
		}

		NbtCompound nbt;
		try {
			nbt = NbtCompoundArgumentType.getNbtCompound(ctx, "nbt").copy();
		} catch (IllegalArgumentException ignored) {
			nbt = new NbtCompound();
		}
		nbt.putString("id", disguiseId.toString());

		NbtCompound finalNbt = nbt;
		AtomicInteger successCount = new AtomicInteger(0);

		entities.forEach(entity -> EntityType.loadEntityWithPassengers(finalNbt, ctx.getSource().getWorld(),
				SpawnReason.LOAD, (entityx) -> {
					((EntityDisguise) entity).disguiseAs(entityx);
					successCount.incrementAndGet();
					return entityx;
				}));

		int count = successCount.get();
		if (count > 0) {
			String disguiseName = type.value().getName().getString();
			src.sendFeedback(() -> Text.literal("Disguised " + count + " entity(ies) as " + disguiseName)
					.formatted(Formatting.GREEN), true);
			return count;
		} else {
			src.sendError(Text.literal("Failed to disguise entities").formatted(Formatting.RED));
			return 0;
		}
	}
}
package karn.minecraftsnap.game;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.CaptainWeatherGuiService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

final class VillagerCaptainSkill extends AbstractCaptainFactionSkillStrategy {
	static final int COOLDOWN_SECONDS = 60;
	private static final int RECALL_RESPONSE_SECONDS = 15;
	private static final String KIND_RECALL_RESPONSE = "villager_recall_response";
	private static final String CUSTOM_DATA_LANE_ID = "minecraftsnap_lane_id";
	private static final String CUSTOM_DATA_CAPTAIN_ID = "minecraftsnap_captain_id";
	private final Map<TeamId, RecallRequest> recallRequests = new HashMap<>();

	VillagerCaptainSkill(
		MatchManager matchManager,
		LaneRuntimeRegistry laneRuntimeRegistry,
		CaptainWeatherGuiService captainWeatherGuiService,
		CaptainManaService captainManaService,
		TextTemplateResolver textTemplateResolver,
		UiSoundService uiSoundService,
		Random random
	) {
		super(matchManager, laneRuntimeRegistry, captainWeatherGuiService, captainManaService, textTemplateResolver, uiSoundService, random);
	}

	@Override
	public FactionId factionId() {
		return FactionId.VILLAGER;
	}

	@Override
	public boolean use(ServerPlayerEntity captain, PlayerMatchState state, SystemConfig systemConfig) {
		var targetLane = UnitSpawnService.nearestLaneForCaptain(captain, systemConfig);
		if (!matchManager.isLaneRevealed(targetLane)) {
			captain.sendMessage(textTemplateResolver.format(textConfig().closedLaneMessage), true);
			playUiDeny(captain);
			return false;
		}
		var targets = new ArrayList<>(teamUnits(state.getTeamId()));
		if (targets.isEmpty()) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainVillagerNoTargetMessage), true);
			playUiDeny(captain);
			return false;
		}
		recallRequests.put(state.getTeamId(), new RecallRequest(
			captain.getUuid(),
			state.getTeamId(),
			targetLane,
			matchManager.getServerTicks() + RECALL_RESPONSE_SECONDS * 20L,
			targets.stream().map(ServerPlayerEntity::getUuid).collect(java.util.stream.Collectors.toSet())
		));
		for (var player : targets) {
			player.getInventory().insertStack(createRecallItem(captain, targetLane));
			showRecallTitle(player);
			player.playSoundToPlayer(SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
		}
		captainManaService.triggerSkillCooldown(captain.getUuid(), COOLDOWN_SECONDS);
		captain.sendMessage(textTemplateResolver.format(textConfig().captainVillagerRequestStartedMessage.replace("{lane}", laneLabel(targetLane))), false);
		return true;
	}

	boolean handleRecallResponse(ServerPlayerEntity player, ItemStack stack, SystemConfig systemConfig) {
		if (player == null || stack == null || stack.isEmpty() || systemConfig == null || !isRecallItem(stack)) {
			return false;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (!state.isUnit() || state.getTeamId() == null || state.getCurrentUnitId() == null || player.isSpectator()) {
			return true;
		}
		var request = recallRequests.get(state.getTeamId());
		if (request == null || matchManager.getServerTicks() > request.expiresAtTick || !request.eligiblePlayerIds.contains(player.getUuid())) {
			player.sendMessage(textTemplateResolver.format(textConfig().captainVillagerRecallExpiredMessage), true);
			return true;
		}
		if (request.targetLane != laneFromItem(stack)) {
			return true;
		}
		if (request.respondedPlayerIds.add(player.getUuid())) {
			removeOneRecallItem(player, stack);
			broadcastRecallProgress(request);
		}
		return true;
	}

	@Override
	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || systemConfig == null || recallRequests.isEmpty()) {
			return;
		}
		var expired = recallRequests.values().stream()
			.filter(request -> matchManager.getServerTicks() >= request.expiresAtTick)
			.toList();
		for (var request : expired) {
			resolveRecallRequest(server, systemConfig, request);
			recallRequests.remove(request.teamId);
		}
	}

	@Override
	public void clearRuntimeState(MinecraftServer server, SystemConfig systemConfig) {
		if (server != null) {
			for (var request : recallRequests.values()) {
				removeRecallItems(server, request);
			}
		}
		recallRequests.clear();
	}

	static boolean isRecallItem(ItemStack stack) {
		var customData = stack == null ? null : stack.get(DataComponentTypes.CUSTOM_DATA);
		return customData != null && KIND_RECALL_RESPONSE.equals(customData.copyNbt().getString(UnitLoadoutService.CUSTOM_DATA_KIND).orElse(""));
	}

	private ItemStack createRecallItem(ServerPlayerEntity captain, LaneId targetLane) {
		var stack = new ItemStack(Items.PAPER);
		stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("minecraft:goat_horn"));
		stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.formatUi(textConfig().captainVillagerRecallItemNameTemplate.replace("{lane}", laneLabel(targetLane))));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(textTemplateResolver.formatUi(textConfig().captainVillagerRecallItemLore))));
		var nbt = new NbtCompound();
		nbt.putString(UnitLoadoutService.CUSTOM_DATA_KIND, KIND_RECALL_RESPONSE);
		nbt.putString(CUSTOM_DATA_LANE_ID, targetLane.name());
		if (captain != null) {
			nbt.putString(CUSTOM_DATA_CAPTAIN_ID, captain.getUuidAsString());
		}
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
		return stack;
	}

	private LaneId laneFromItem(ItemStack stack) {
		var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (customData == null) {
			return null;
		}
		try {
			return LaneId.valueOf(customData.copyNbt().getString(CUSTOM_DATA_LANE_ID).orElse(""));
		} catch (Exception ignored) {
			return null;
		}
	}

	private void showRecallTitle(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
		player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 60, 10));
		player.networkHandler.sendPacket(new TitleS2CPacket(textTemplateResolver.format(textConfig().captainVillagerRecallTitle)));
		player.networkHandler.sendPacket(new SubtitleS2CPacket(textTemplateResolver.format(textConfig().captainVillagerRecallSubtitle)));
	}

	private void broadcastRecallProgress(RecallRequest request) {
		var message = textConfig().captainVillagerRecallProgressMessage
			.replace("{count}", Integer.toString(request.respondedPlayerIds.size()))
			.replace("{total}", Integer.toString(request.eligiblePlayerIds.size()));
		for (var player : matchManager.getOnlinePlayers()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (state.getTeamId() == request.teamId && state.getRoleType() != RoleType.NONE && state.getRoleType() != RoleType.SPECTATOR) {
				player.sendMessage(textTemplateResolver.format(message), false);
			}
		}
	}

	private void resolveRecallRequest(MinecraftServer server, SystemConfig systemConfig, RecallRequest request) {
		removeRecallItems(server, request);
		if (request.respondedPlayerIds.isEmpty()) {
			captainManaService.triggerSkillCooldown(request.captainId, 0);
			var captain = server.getPlayerManager().getPlayer(request.captainId);
			if (captain != null) {
				captain.sendMessage(textTemplateResolver.format(textConfig().captainVillagerRecallNoResponseMessage), false);
			}
			return;
		}
		var captain = server.getPlayerManager().getPlayer(request.captainId);
		var targetSpawn = UnitSpawnService.safeUnitSpawn(systemConfig, request.teamId, request.targetLane);
		var world = resolveWorld(server, systemConfig.world);
		var targetPos = new Vec3d(targetSpawn.x, targetSpawn.y, targetSpawn.z);
		for (var playerId : request.respondedPlayerIds) {
			var player = server.getPlayerManager().getPlayer(playerId);
			if (player == null) {
				continue;
			}
			spawnLineParticles(world, player.getPos(), targetPos, ParticleTypes.END_ROD);
			player.teleportTo(new TeleportTarget(world, targetPos, Vec3d.ZERO, targetSpawn.yaw, targetSpawn.pitch, TeleportTarget.NO_OP));
			applyRecallBuffs(captain, player);
		}
		recordCaptainSkillUse(FactionId.VILLAGER);
		playGlobal(world, SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.PLAYERS, 10.0f, 1.0f);
		var detailMessage = textConfig().captainVillagerSuccessMessage.replace("{lane}", laneLabel(request.targetLane));
		if (captain != null) {
			captain.sendMessage(textTemplateResolver.format(detailMessage), false);
			playEventSuccess(captain);
		}
		broadcastCaptainSkill(captain, detailMessage);
	}

	private void applyRecallBuffs(ServerPlayerEntity captain, ServerPlayerEntity player) {
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 30, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 30, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 30, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 30, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 30, 1));
		fullHealAndTrack(captain, player);
	}

	private void removeRecallItems(MinecraftServer server, RecallRequest request) {
		for (var playerId : request.eligiblePlayerIds) {
			var player = server.getPlayerManager().getPlayer(playerId);
			if (player != null) {
				removeRecallItems(player);
			}
		}
	}

	private void removeRecallItems(ServerPlayerEntity player) {
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.size(); slot++) {
			if (isRecallItem(inventory.getStack(slot))) {
				inventory.setStack(slot, ItemStack.EMPTY);
			}
		}
		player.currentScreenHandler.sendContentUpdates();
	}

	private void removeOneRecallItem(ServerPlayerEntity player, ItemStack usedStack) {
		usedStack.decrement(1);
		player.currentScreenHandler.sendContentUpdates();
	}

	private record RecallRequest(
		UUID captainId,
		TeamId teamId,
		LaneId targetLane,
		long expiresAtTick,
		HashSet<UUID> eligiblePlayerIds,
		HashSet<UUID> respondedPlayerIds
	) {
		private RecallRequest(UUID captainId, TeamId teamId, LaneId targetLane, long expiresAtTick, java.util.Set<UUID> eligiblePlayerIds) {
			this(captainId, teamId, targetLane, expiresAtTick, new HashSet<>(eligiblePlayerIds), new HashSet<>());
		}
	}
}

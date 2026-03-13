package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class CreeperUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	private static final String BOMB_TICK_KEY = "creeper_bomb_tick";
	public static final UnitDefinition DEFINITION = unit(
		"creeper",
		"크리퍼",
		FactionId.MONSTER,
		true,
		5,
		25,
		20.0,
		1.0,
		item("minecraft:tnt"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"자폭",
		20,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:creeper"),
		List.of("&71초 뒤 자폭", "&7근접 폭발 특화"),
		List.of(advanceOption(
			"charged_creeper",
			"대전된 크리퍼",
			List.of("&7천둥 아래에서 대전됨"),
			List.of("minecraft:plains", "minecraft:forest", "minecraft:dark_forest"),
			List.of("thunder"),
			10
		))
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		var triggerTick = context.getUnitRuntimeLong(BOMB_TICK_KEY);
		if (triggerTick == null || context.serverTicks() < triggerTick) {
			return;
		}
		context.removeUnitRuntimeLong(BOMB_TICK_KEY);
		explode(context);
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			context.setUnitRuntimeLong(BOMB_TICK_KEY, context.serverTicks() + 20L);
			context.player().sendMessage(context.format(textConfig().creeperSelfDestructPrimedMessage), true);
			return true;
		});
	}

	private void explode(UnitContext context) {
		var player = context.player();
		player.getWorld().playSound(
			null,
			player.getBlockPos(),
			SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
			SoundCategory.PLAYERS,
			1.0f,
			1.0f
		);
		for (var target : nearbyEnemyPlayers(context, 5.0D)) {
			context.dealExplosionDamage(target, 14.0f);
		}
		player.damage(context.world(), player.getDamageSources().explosion(player, player), 1000.0f);
	}

	private java.util.List<ServerPlayerEntity> nearbyEnemyPlayers(UnitContext context, double radius) {
		var player = context.player();
		var squaredRadius = radius * radius;
		return player.getServer().getPlayerManager().getPlayerList().stream()
			.filter(target -> target.getWorld() == player.getWorld())
			.filter(target -> !target.getUuid().equals(player.getUuid()))
			.filter(target -> player.squaredDistanceTo(target) <= squaredRadius)
			.filter(context::isEnemyUnit)
			.toList();
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}

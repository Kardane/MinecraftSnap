package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class CreeperUnit extends AbstractMonsterUnit {
	private static final String BOMB_TICK_KEY = "creeper_bomb_tick";

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
			context.player().sendMessage(context.format("&c자폭 준비"), true);
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
}

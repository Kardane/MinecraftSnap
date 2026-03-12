package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public class SkeletonUnit extends AbstractMonsterUnit {
	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			for (var target : nearbyEnemyPlayers(context, 4.0D)) {
				context.dealMobDamage(target, 6.0f);
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 3, 1), context.player());
			}
			return true;
		});
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

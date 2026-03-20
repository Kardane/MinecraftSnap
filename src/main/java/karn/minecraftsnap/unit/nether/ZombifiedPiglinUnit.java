package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.SummonedMobSupport;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ZombifiedPiglinUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"zombified_piglin",
		"좀비 피글린",
		FactionId.NETHER,
		true,
		1,
		20.0,
		1.0,
		item("minecraft:golden_sword"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:zombified_piglin"),
			List.of("&f패시브 &7- 피격시 낮은 확률로 주변에 좀비 피글린 생성","&f무기 &7- 금 검"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onDamaged(UnitContext context, DamageSource source, float amount) {
		if (!hasTriggeringAttacker(source)) {
			return;
		}
		if (context.player().getRandom().nextDouble() >= summonChance()) {
			return;
		}
		var world = context.world();
		var player = context.player();
		var piglin = EntityType.ZOMBIFIED_PIGLIN.create(world, SpawnReason.MOB_SUMMONED);
		if (piglin == null) {
			return;
		}
		var random = player.getRandom();
		var x = player.getX() + random.nextBetween(-5, 5);
		var z = player.getZ() + random.nextBetween(-5, 5);
		piglin.refreshPositionAndAngles(x, player.getY(), z, player.getYaw(), player.getPitch());
		world.spawnEntity(piglin);
		SummonedMobSupport.applyFriendlyTeam(context, piglin);
	}

	boolean hasTriggeringAttacker(DamageSource source) {
		if (source == null) {
			return false;
		}
		return resolveAttacker(source.getAttacker()) != null || resolveAttacker(source.getSource()) != null;
	}

	private Entity resolveAttacker(Entity entity) {
		if (entity instanceof ProjectileEntity projectile) {
			return projectile.getOwner();
		}
		return entity;
	}

	double summonChance() {
		return 0.1D;
	}

	double summonRange() {
		return 5.0D;
	}
}

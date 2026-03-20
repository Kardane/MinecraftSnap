package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.SummonedMobSupport;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SlimeUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"slime",
		"슬라임",
		FactionId.MONSTER,
		true,
		2,
		18.0,
		1.0,
		item("minecraft:slime_ball"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:slime", "{Size:3}"),
			List.of("&f패시브 &7- 점프력이 상승합니다. 사망시 분열합니다","&f무기 &7- 슬라임 볼"),
		List.of(advanceOption(
			"giant_slime",
			"거대 슬라임",
			List.of("&7늪에서 15초 버티면 적응"),
			List.of("minecraft:swamp", "minecraft:mangrove_swamp"),
			List.of(),
			300
		))
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), definition().id(), weaponAttackDamage(), weaponAttackSpeed());
	}

	@Override
	public boolean shouldCancelMove(UnitContext context, net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
		var player = context.player();
		if (player == null || packet == null || !player.isOnGround()) {
			return false;
		}
		if (player.getVelocity().y > 0.05D) {
			return false;
		}
		return packet.getY(player.getY()) > player.getY() + 0.05D;
	}

	@Override
	public void applyAttributes(UnitContext context) {
		super.applyAttributes(context);
		applyJumpStrength(context);
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		applyJumpStrength(context);
	}

	@Override
	public void onDeath(karn.minecraftsnap.unit.UnitContext context, DamageSource source) {
		var world = context.world();
		var player = context.player();
		for (int index = 0; index < spawnedSlimeCount(); index++) {
			var slime = EntityType.SLIME.create(world, SpawnReason.MOB_SUMMONED);
			if (slime == null) {
				continue;
			}
			slime.setSize(spawnedSlimeSize(), true);
			slime.refreshPositionAndAngles(player.getX() + (index - 1), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
			slime.setCustomName(player.getName().copy());
			world.spawnEntity(slime);
			SummonedMobSupport.applyFriendlyTeam(context, slime);
		}
	}

	private void applyJumpStrength(UnitContext context) {
		var jumpStrength = context.player().getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		if (jumpStrength != null) {
			jumpStrength.setBaseValue(jumpStrengthValue());
		}
	}

	int spawnedSlimeCount() {
		return 4;
	}

	int spawnedSlimeSize() {
		return 2;
	}

	double weaponAttackDamage() {
		return 3.0D;
	}

	double weaponAttackSpeed() {
		return 2.0D;
	}

	double jumpStrengthValue() {
		return 0.8D;
	}
}

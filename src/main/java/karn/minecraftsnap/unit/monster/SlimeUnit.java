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
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SlimeUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), context.unitDefinition().id(), weaponAttackDamage(), weaponAttackSpeed());
	}

	@Override
	public boolean shouldCancelMove(UnitContext context, net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
		var player = context.player();
		if (player == null || packet == null || !player.isOnGround()) {
			return false;
		}
		return shouldCancelUpwardMove(player.isOnGround(), player.getVelocity().y, player.getY(), packet.getY(player.getY()));
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
		if (isPoisonImmune()) {
			context.player().removeStatusEffect(StatusEffects.POISON);
		}
	}

	boolean isPoisonImmune() {
		return true;
	}

	boolean shouldCancelUpwardMove(boolean onGround, double velocityY, double currentY, double packetY) {
		return onGround
			&& velocityY <= 0.05D
			&& packetY > currentY + 1.0D;
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
			SummonedMobSupport.applyFriendlyTeam(context, slime);
			world.spawnEntity(slime);
		}
	}

	private void applyJumpStrength(UnitContext context) {
		var jumpStrength = context.player().getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		if (jumpStrength != null) {
			jumpStrength.setBaseValue(context.unitDefinition().extraAttributes().jumpStrengthOrDefault(jumpStrengthValue()));
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

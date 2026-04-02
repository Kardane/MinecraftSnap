package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class BoggedUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"bogged",
		"보그드",
		FactionId.MONSTER,
		false,
		4,
		16.0,
		0.75,
		item("minecraft:bow"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.POISON_ARROW,
		disguise("minecraft:bogged"),
		List.of("&f패시브&7- 독 화살을 사용합니다.", "&f무기 &7- 활"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		if (isPoisonImmune()) {
			context.player().removeStatusEffect(StatusEffects.POISON);
		}
	}

	boolean isPoisonImmune() {
		return true;
	}
}

package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.attribute.EntityAttributes;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class GiantSlimeUnit extends SlimeUnit {
	public static final UnitDefinition DEFINITION = unit(
		"giant_slime",
		"거대 슬라임",
		FactionId.MONSTER,
		false,
		0,
		30.0,
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
		disguise("minecraft:slime", "{Size:5}"),
			List.of("&f패시브 &7- 크기가 더욱 증가하고, 사망시 분열합니다","&f무기 &7- 슬라임 볼"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void applyAttributes(UnitContext context) {
		super.applyAttributes(context);
		applyScale(context);
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		applyScale(context);
	}

	private void applyScale(UnitContext context) {
		var scale = context.player().getAttributeInstance(EntityAttributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(2.0D);
		}
	}

	@Override
	int spawnedSlimeCount() {
		return 3;
	}

	@Override
	int spawnedSlimeSize() {
		return 5;
	}
}

package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.config.EntitySpecEntry;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ChargedCreeperUnit extends CreeperUnit {
	private static final EntitySpecEntry POWERED_DISGUISE = disguise("minecraft:creeper", "{powered:1b}");
	private static final EntitySpecEntry POWERED_IGNITED_DISGUISE = disguise("minecraft:creeper", "{powered:1b,ignited:1b}");
	public static final UnitDefinition DEFINITION = unit(
		"charged_creeper",
		"대전된 크리퍼",
		FactionId.MONSTER,
		false,
		0,
		0,
		24.0,
		1.05,
		item("minecraft:tnt"),
		none(),
		none(),
		none(),
		none(),
		none(),
		item("minecraft:tnt"),
		"자폭",
		20,
		UnitDefinition.AmmoType.NONE,
		POWERED_DISGUISE,
		List.of("&7대전 상태 유지", "&76칸 내 적에게 피해 99"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	EntitySpecEntry restingDisguise() {
		return POWERED_DISGUISE;
	}

	@Override
	EntitySpecEntry activeDisguise() {
		return POWERED_IGNITED_DISGUISE;
	}

	@Override
	double blastRadius() {
		return 6.0D;
	}

	@Override
	float blastDamage() {
		return 99.0f;
	}

	@Override
	boolean powered() {
		return true;
	}
}

package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.unit.monster.BoggedUnit;
import karn.minecraftsnap.unit.monster.ChargedCreeperUnit;
import karn.minecraftsnap.unit.monster.CreeperUnit;
import karn.minecraftsnap.unit.monster.DrownedUnit;
import karn.minecraftsnap.unit.monster.GiantSlimeUnit;
import karn.minecraftsnap.unit.monster.HuskUnit;
import karn.minecraftsnap.unit.monster.SkeletonUnit;
import karn.minecraftsnap.unit.monster.SlimeUnit;
import karn.minecraftsnap.unit.monster.StrayUnit;
import karn.minecraftsnap.unit.monster.WitherSkeletonUnit;
import karn.minecraftsnap.unit.monster.ZombieUnit;
import karn.minecraftsnap.unit.nether.BlazeUnit;
import karn.minecraftsnap.unit.nether.MagmaCubeUnit;
import karn.minecraftsnap.unit.nether.PiglinBruteUnit;
import karn.minecraftsnap.unit.nether.PiglinUnit;
import karn.minecraftsnap.unit.nether.ZombifiedPiglinUnit;
import karn.minecraftsnap.unit.villager.ArmorerVillagerUnit;
import karn.minecraftsnap.unit.villager.PillagerUnit;
import karn.minecraftsnap.unit.villager.VillagerUnit;
import karn.minecraftsnap.unit.villager.VindicatorUnit;

import java.util.LinkedHashMap;
import java.util.Map;

public class UnitClassRegistry {
	private final Map<String, ConfiguredUnitClass> unitClasses = new LinkedHashMap<>();

	public UnitClassRegistry() {
		register(new VillagerUnit());
		register(new ArmorerVillagerUnit());
		register(new VindicatorUnit());
		register(new PillagerUnit());
		register(new ZombieUnit());
		register(new HuskUnit());
		register(new DrownedUnit());
		register(new SkeletonUnit());
		register(new StrayUnit());
		register(new BoggedUnit());
		register(new WitherSkeletonUnit());
		register(new SlimeUnit());
		register(new GiantSlimeUnit());
		register(new CreeperUnit());
		register(new ChargedCreeperUnit());
		register(new PiglinUnit());
		register(new ZombifiedPiglinUnit());
		register(new BlazeUnit());
		register(new MagmaCubeUnit());
		register(new PiglinBruteUnit());
	}

	public UnitClass get(String unitId) {
		return unitClasses.get(unitId);
	}

	public java.util.Collection<ConfiguredUnitClass> configuredUnits() {
		return unitClasses.values();
	}

	public void validateAgainst(UnitRegistry unitRegistry) {
		for (var definition : unitRegistry.all()) {
			if (!unitClasses.containsKey(definition.id())) {
				throw new IllegalStateException("유닛 클래스 누락: " + definition.id());
			}
		}
	}

	private void register(ConfiguredUnitClass unitClass) {
		unitClasses.put(unitClass.definition().id(), unitClass);
	}
}

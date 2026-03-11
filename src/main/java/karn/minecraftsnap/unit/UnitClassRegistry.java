package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.unit.monster.ChargedCreeperUnit;
import karn.minecraftsnap.unit.monster.CreeperUnit;
import karn.minecraftsnap.unit.monster.SkeletonSniperUnit;
import karn.minecraftsnap.unit.monster.SkeletonUnit;
import karn.minecraftsnap.unit.monster.SlimeBruteUnit;
import karn.minecraftsnap.unit.monster.SlimeUnit;
import karn.minecraftsnap.unit.monster.ZombieUnit;
import karn.minecraftsnap.unit.monster.ZombieVeteranUnit;
import karn.minecraftsnap.unit.nether.BlazeUnit;
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
	private final Map<String, UnitClass> unitClasses = new LinkedHashMap<>();

	public UnitClassRegistry() {
		register("villager", new VillagerUnit());
		register("armorer_villager", new ArmorerVillagerUnit());
		register("vindicator", new VindicatorUnit());
		register("pillager", new PillagerUnit());
		register("zombie", new ZombieUnit());
		register("skeleton", new SkeletonUnit());
		register("slime", new SlimeUnit());
		register("creeper", new CreeperUnit());
		register("zombie_veteran", new ZombieVeteranUnit());
		register("skeleton_sniper", new SkeletonSniperUnit());
		register("slime_brute", new SlimeBruteUnit());
		register("charged_creeper", new ChargedCreeperUnit());
		register("piglin", new PiglinUnit());
		register("zombified_piglin", new ZombifiedPiglinUnit());
		register("blaze", new BlazeUnit());
		register("piglin_brute", new PiglinBruteUnit());
	}

	public UnitClass get(String unitId) {
		return unitClasses.get(unitId);
	}

	public void validateAgainst(UnitRegistry unitRegistry) {
		for (var definition : unitRegistry.all()) {
			if (!unitClasses.containsKey(definition.id())) {
				throw new IllegalStateException("유닛 클래스 누락: " + definition.id());
			}
		}
	}

	private void register(String unitId, UnitClass unitClass) {
		unitClasses.put(unitId, unitClass);
	}
}

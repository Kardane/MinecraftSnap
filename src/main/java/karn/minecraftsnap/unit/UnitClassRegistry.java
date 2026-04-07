package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.unit.monster.BoggedUnit;
import karn.minecraftsnap.unit.monster.BreezeUnit;
import karn.minecraftsnap.unit.monster.CaveSpiderUnit;
import karn.minecraftsnap.unit.monster.ChargedCreeperUnit;
import karn.minecraftsnap.unit.monster.CreeperUnit;
import karn.minecraftsnap.unit.monster.DrownedUnit;
import karn.minecraftsnap.unit.monster.ElderGuardianUnit;
import karn.minecraftsnap.unit.monster.EndermanUnit;
import karn.minecraftsnap.unit.monster.GuardianUnit;
import karn.minecraftsnap.unit.monster.GiantSlimeUnit;
import karn.minecraftsnap.unit.monster.HuskUnit;
import karn.minecraftsnap.unit.monster.SilverfishUnit;
import karn.minecraftsnap.unit.monster.SkeletonUnit;
import karn.minecraftsnap.unit.monster.SlimeUnit;
import karn.minecraftsnap.unit.monster.StrayUnit;
import karn.minecraftsnap.unit.monster.ZombieUnit;
import karn.minecraftsnap.unit.nether.BlazeUnit;
import karn.minecraftsnap.unit.nether.GhastUnit;
import karn.minecraftsnap.unit.nether.HoglinUnit;
import karn.minecraftsnap.unit.nether.MagmaCubeUnit;
import karn.minecraftsnap.unit.nether.PiglinBruteUnit;
import karn.minecraftsnap.unit.nether.PiglinUnit;
import karn.minecraftsnap.unit.nether.WitherSkeletonUnit;
import karn.minecraftsnap.unit.nether.ZombifiedPiglinUnit;
import karn.minecraftsnap.unit.villager.IllusionerUnit;
import karn.minecraftsnap.unit.villager.IronGolemUnit;
import karn.minecraftsnap.unit.villager.PillagerUnit;
import karn.minecraftsnap.unit.villager.SnowGolemUnit;
import karn.minecraftsnap.unit.villager.SummonerUnit;
import karn.minecraftsnap.unit.villager.VillagerUnit;
import karn.minecraftsnap.unit.villager.VindicatorUnit;
import karn.minecraftsnap.unit.villager.WitchUnit;

import java.util.LinkedHashMap;
import java.util.Map;

public class UnitClassRegistry {
	private final Map<String, ConfiguredUnitClass> unitClasses = new LinkedHashMap<>();

	public UnitClassRegistry() {
		register("villager", new VillagerUnit());
		register("snow_golem", new SnowGolemUnit());
		register("iron_golem", new IronGolemUnit());
		register("summoner", new SummonerUnit());
		register("witch", new WitchUnit());
		register("illusioner", new IllusionerUnit());
		register("vindicator", new VindicatorUnit());
		register("pillager", new PillagerUnit());
		register("zombie", new ZombieUnit());
		register("silverfish", new SilverfishUnit());
		register("cave_spider", new CaveSpiderUnit());
		register("breeze", new BreezeUnit());
		register("guardian", new GuardianUnit());
		register("elder_guardian", new ElderGuardianUnit());
		register("husk", new HuskUnit());
		register("drowned", new DrownedUnit());
		register("skeleton", new SkeletonUnit());
		register("stray", new StrayUnit());
		register("bogged", new BoggedUnit());
		register("wither_skeleton", new WitherSkeletonUnit());
		register("slime", new SlimeUnit());
		register("giant_slime", new GiantSlimeUnit());
		register("creeper", new CreeperUnit());
		register("charged_creeper", new ChargedCreeperUnit());
		register("piglin", new PiglinUnit());
		register("zombified_piglin", new ZombifiedPiglinUnit());
		register("enderman", new EndermanUnit());
		register("blaze", new BlazeUnit());
		register("magma_cube", new MagmaCubeUnit());
		register("ghast", new GhastUnit());
		register("hoglin", new HoglinUnit());
		register("piglin_brute", new PiglinBruteUnit());
	}

	public UnitClass get(String unitId) {
		return unitClasses.get(unitId);
	}

	public java.util.Collection<ConfiguredUnitClass> configuredUnits() {
		return unitClasses.values();
	}

	public java.util.Set<String> configuredUnitIds() {
		return new java.util.LinkedHashSet<>(unitClasses.keySet());
	}

	public void validateAgainst(UnitRegistry unitRegistry) {
		for (var definition : unitRegistry.all()) {
			if (!unitClasses.containsKey(definition.id())) {
				throw new IllegalStateException("유닛 클래스 누락: " + definition.id());
			}
		}
		for (var unitId : unitClasses.keySet()) {
			if (unitRegistry.get(unitId) == null) {
				throw new IllegalStateException("유닛 컨픽 누락: " + unitId);
			}
		}
	}

	private void register(String unitId, ConfiguredUnitClass unitClass) {
		unitClasses.put(unitId, unitClass);
	}
}

package karn.minecraftsnap.game;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FactionSpecs {
	public static final FactionSpec VILLAGER = new FactionSpec(
		"주민&우민",
		List.of("&7균형형 팩션", "&7거래와 유지력이 강점"),
		"습격 소집",
		List.of("&7실제 스킬 로직은 기존 구현 유지", "&7위키와 GUI 설명은 Java 기준")
	);

	public static final FactionSpec MONSTER = new FactionSpec(
		"몬스터",
		List.of("&7전직과 기습 중심", "&7환경 적응이 핵심"),
		"날씨 변화",
		List.of("&7실제 스킬 로직은 기존 구현 유지", "&7위키와 GUI 설명은 Java 기준")
	);

	public static final FactionSpec NETHER = new FactionSpec(
		"네더",
		List.of("&7교전 강화형 팩션", "&7금괴 경제와 돌파력"),
		"포탈 생성",
		List.of("&7실제 스킬 로직은 기존 구현 유지", "&7위키와 GUI 설명은 Java 기준")
	);

	private FactionSpecs() {
	}

	public static Map<FactionId, FactionSpec> defaults() {
		var specs = new EnumMap<FactionId, FactionSpec>(FactionId.class);
		specs.put(FactionId.VILLAGER, VILLAGER);
		specs.put(FactionId.MONSTER, MONSTER);
		specs.put(FactionId.NETHER, NETHER);
		return specs;
	}

	public static FactionSpec get(FactionId factionId) {
		return defaults().get(factionId);
	}
}

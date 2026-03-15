package karn.minecraftsnap.game;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FactionSpecs {
	public static final FactionSpec VILLAGER = new FactionSpec(
		"주민&우민",
		List.of("&7주민과 우민 유닛을 사용합니다.","&7유닛은 적 처치, 점령을 통해 에메랄드를 얻고","&7상점을 이용할 수 있습니다."),
		"사령관 스킬 - 습격 소집",
		List.of("&7[마나 4 필요] 다른 라인의 아군 유닛을 사령관이 있는 라인으로 소집합니다.", "&7유닛은 체력을 모두 회복하고, 재생, 신속, 성급함이 부여됩니다.")
	);

	public static final FactionSpec MONSTER = new FactionSpec(
		"몬스터",
		List.of("&7몬스터 유닛을 사용합니다.", "&7유닛은 위치한 바이옴에 적응하여 전직할 수 있습니다."),
		"사령관 스킬 - 날씨 변화",
		List.of("&7[마나 4 필요] 맑음 / 비 / 폭풍우 선택합니다.", "&7비와 폭풍우는 아군 유닛을 버프하며", "&7폭풍우는 적에게 낮은 확률로 피해를 입힙니다.")
	);

	public static final FactionSpec NETHER = new FactionSpec(
		"네더",
		List.of("&7네더 유닛을 사용합니다.", "&7유닛은 적을 처치하여 금괴를 얻고","&7상점을 이용할 수 있습니다."),
		"사령관 스킬 - 포탈 생성",
		List.of("&7[마나 5 필요] 가장 가까운 라인에 포탈을 생성합니다.", "&7해당 라인에 있는 아군은 힘을 얻고, 유닛 소환시 소모한 마나의 일부를 환급받습니다.")
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

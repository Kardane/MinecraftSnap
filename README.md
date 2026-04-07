# MinecraftSnap

Minecraft **1.21.8 Fabric 서버 전용** 미니게임 모드 템플릿.

쓸데없는 보일러플레이트 줄이고, 서버사이드 미니게임에서 자주 쓰는 라이브러리(Polymer/Patbox/권한/맵/대화/NBT 툴팁)를 한 번에 묶어둔 시작점이다.

## 1. 프로젝트 개요

- 목적: Fabric 기반 서버 미니게임 모드를 빠르게 시작
- 런타임 타입: **Dedicated Server 전용 초기화기**(`DedicatedServerModInitializer`)
- 기본 엔트리포인트:
  - `karn.minecraftsnap.MinecraftSnap`
- 기본 모드 ID:
  - `minecraftsnap`

## 2. 사용 환경 (상세)

### 필수 환경

| 항목 | 값 |
|---|---|
| OS | Linux/macOS/Windows (Java 21 실행 가능 환경) |
| JDK | **21** |
| Minecraft | **1.21.8** |
| Fabric Loader | **0.18.0** |
| Fabric API | **0.136.0+1.21.8** |
| Gradle Wrapper | **9.2.1** (`gradle/wrapper/gradle-wrapper.properties`) |

### 빌드/실행 전 체크

- `JAVA_HOME`이 JDK 21 경로를 가리켜야 함
- `java -version` 결과에 21이 보여야 함
- 사내망/방화벽 환경이면 다음 저장소 접근이 가능해야 함
  - `https://maven.fabricmc.net`
  - `https://maven.nucleoid.xyz`
  - `https://jitpack.io`
  - `https://repo.maven.apache.org`

### 호환성 주의

- `DialogUtils`는 `com.github.Kardane:DialogUtils:v1.7.2-mc1.21.8` 백포팅 버전 기준으로 고정
- 베이스 타겟(`1.21.8`)과 맞춘 상태라, 기존 `1.21.9` 마이너 불일치 리스크 제거

## 3. 포함 라이브러리

### 코어

| 라이브러리 | 버전 |
|---|---|
| Fabric Loader | 0.18.0 |
| Fabric API | 0.136.0+1.21.8 |

### Polymer 계열

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| polymer-core | 0.13.9+1.21.8 | 서버사이드 커스텀 아이템/블록 기반 |
| polymer-blocks | 0.13.13+1.21.8 | 커스텀 블록 표현 |
| polymer-resource-pack | 0.13.13+1.21.8 | 리소스팩 생성/병합 |
| polymer-virtual-entity | 0.13.9+1.21.8 | 가상 엔티티/디스플레이 |
| polymer-autohost | 0.13.13+1.21.8 | 리소스팩 자동 호스팅 |

### Patbox 유틸리티

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| sgui | 1.10.2+1.21.8 | 서버사이드 GUI |
| placeholder-api | 2.7.2+1.21.8 | 플레이스홀더 파싱 |
| map-canvas-api | 0.5.1+1.21.5 | 맵 캔버스 렌더링 |
| player-data-api | 0.8.0+1.21.6 | 플레이어 데이터 저장 |
| predicate-api | 0.6.0+1.21.2 | 조건식 처리 |
| server-translations-api | 2.5.1+1.21.5 | 다국어 문자열 처리 |

### 외부 라이브러리

| 라이브러리 | 버전 | 좌표 |
|---|---|---|
| fabric-permissions-api | 0.4.0 | `me.lucko:fabric-permissions-api` |
| RGBMapUtils | `6b89c1177972c96fc01f1186d13fa9b1bd4681a7` | `com.github.biryeongtrain:RGBMapUtils` |
| DialogUtils | v1.7.2-mc1.21.8 | `com.github.Kardane:DialogUtils` |
| DisguiseLib | `v1.21.8.4` (`17d8f670fb187824f476ddf855661580f0283bf9`) | `libs/disguiselib-v1.21.8.4.jar` |

## 4. 프로젝트 구조

```text
src/main/
├── java/karn/minecraftsnap/
│   ├── MinecraftSnap.java
│   └── mixin/ExampleMixin.java
└── resources/
    ├── fabric.mod.json
    └── minecraftsnap.mixins.json
```

- `MinecraftSnap.java`
  - 서버 시작 시 초기화 진입점
  - 커맨드/이벤트 등록 메서드 자리 제공
- `ExampleMixin.java`
  - `MinecraftServer#tick` 주입 예제
  - 실제 프로젝트에서는 필요한 로직만 남기고 정리 권장

## 5. 빠른 시작

### Linux/macOS (bash)

```bash
# 1) 소스 동기화
./gradlew --version

# 2) IDE용 소스 생성
./gradlew genSources

# 3) 빌드
./gradlew build

# 4) 개발 서버 실행
./gradlew runServer
```

### Windows (PowerShell)

```powershell
# 1) 소스 동기화
.\gradlew.bat --version

# 2) IDE용 소스 생성
.\gradlew.bat genSources

# 3) 빌드
.\gradlew.bat build

# 4) 개발 서버 실행
.\gradlew.bat runServer
```

## 6. 사용 예시

아래 예시는 템플릿에서 실제로 가장 먼저 붙이는 코드들만 최소로 정리했다.

### 예시 1) 권한 기반 커맨드 등록

`MinecraftSnap#registerCommands()`에서 시작.

```java
import com.mojang.brigadier.Command;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

private void registerCommands() {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
        dispatcher.register(CommandManager.literal("minigame")
            .requires(Permissions.require("minecraftsnap.command.minigame", 2))
            .executes(ctx -> {
                ctx.getSource().sendFeedback(() -> Text.literal("미니게임 템플릿 정상 동작"), false);
                return Command.SINGLE_SUCCESS;
            })
        );
    });
}
```

### 예시 2) 서버 라이프사이클 이벤트 등록

```java
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

private void registerEvents() {
    ServerLifecycleEvents.SERVER_STARTED.register(server -> {
        LOGGER.info("[{}] 서버 시작 완료", MOD_ID);
    });

    ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
        LOGGER.info("[{}] 서버 종료 시작", MOD_ID);
    });
}
```

### 예시 3) SGUI로 간단한 메뉴 열기

```java
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public static void openMenu(ServerPlayerEntity player) {
    SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
    gui.setTitle(Text.literal("미니게임 메뉴"));

    gui.setSlot(13, new GuiElementBuilder(Items.EMERALD)
        .setName(Text.literal("게임 시작"))
        .setCallback((index, clickType, actionType) -> {
            player.sendMessage(Text.literal("매치메이킹 시작"), false);
            gui.close();
        })
    );

    gui.open();
}
```

### 예시 4) RGBMapUtils로 64x64 이미지 인코딩

```java
import kim.biryeong.maprgbutils.RgbMapCodec;

BufferedImage input64x64 = ...; // 64x64 이미지
RgbMapCodec codec = RgbMapCodec.createDefault();
int[] mapIndexes = codec.encodeImageToMapIndexes(input64x64);

// mapIndexes(0..127)를 맵 데이터 적용 로직으로 전달
```

### 예시 5) 플레이스홀더 API 파싱

```java
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public static Text renderHudText(ServerPlayerEntity player) {
    Text raw = Text.literal("환영합니다: %player:name%");
    return Placeholders.parseText(raw, PlaceholderContext.of(player));
}
```

## 7. 의존성 추가/교체 방법

새 라이브러리를 넣을 때는 이 순서만 지키면 된다.

1. `repositories`에 저장소 추가 (필요한 경우만)
2. `dependencies`에 `modImplementation`/`include(...)`/`compileOnly` 중 목적에 맞게 추가
3. 직접 관리하는 로컬 JAR 의존성은 `libs/`에 두고 버전을 파일명으로 고정
4. 버전 문자열은 `gradle.properties`로 분리
5. `fabric.mod.json`의 `depends`/`suggests` 정리

## 8. 트러블슈팅

### `JAVA_HOME is not set`

- 원인: JDK 21 경로 미설정
- 해결: `JAVA_HOME` 설정 후 터미널 재실행

### JitPack 의존성 해석 실패

- 원인: 태그/버전 문자열 불일치 또는 네트워크 제한
- 해결:
  1. 저장소 태그 확인 후 버전 재설정
  2. 사내망이면 `jitpack.io` 허용

### 서버 시작 시 `Mixin apply failed`

- 원인: 대상 메서드 시그니처 변경, 매핑 불일치
- 해결:
  1. 해당 Mixin 임시 비활성화
  2. 타겟 메서드/매핑 재확인

## 9. 권장 개발 루틴

1. 기능 단위로 커맨드/이벤트 먼저 뚫기
2. UI 필요하면 SGUI 붙이기
3. 맵/텍스트 연출이 필요할 때 RGBMapUtils/DialogUtils 적용
4. 빌드 + 런서버 로그 확인 후 다음 기능 진행

일단 이 템플릿은 "빠르게 게임 로직 실험"에는 충분하다. 정교한 아키텍처는 기능이 생긴 다음에 자르는 게 맞다.

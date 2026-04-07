package karn.minecraftsnap.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisguiseAnimationSupportTest {
	@AfterEach
	void tearDown() {
		DisguiseAnimationSupport.resetDispatcherForTest();
	}

	@Test
	void startMethodsForwardExpectedAnimationKinds() {
		var calls = new ArrayList<String>();
		DisguiseAnimationSupport.setDispatcherForTest(new RecordingDispatcher(calls));

		DisguiseAnimationSupport.startGhastCharge(null, 20);
		DisguiseAnimationSupport.startEvokerCast(null, 30);
		DisguiseAnimationSupport.startIllusionerCast(null, 20);

		assertEquals(List.of("ghast:20", "evoker:30", "illusioner:20"), calls);
	}

	@Test
	void tickAndClearForwardToDispatcher() {
		var calls = new ArrayList<String>();
		DisguiseAnimationSupport.setDispatcherForTest(new RecordingDispatcher(calls));

		DisguiseAnimationSupport.tick(null);
		DisguiseAnimationSupport.clear(null);

		assertEquals(List.of("tick", "clear"), calls);
	}

	private record RecordingDispatcher(List<String> calls) implements DisguiseAnimationSupport.Dispatcher {
		@Override
		public void startGhastCharge(net.minecraft.entity.Entity entity, int ticks) {
			calls.add("ghast:" + ticks);
		}

		@Override
		public void startEvokerCast(net.minecraft.entity.Entity entity, int ticks) {
			calls.add("evoker:" + ticks);
		}

		@Override
		public void startIllusionerCast(net.minecraft.entity.Entity entity, int ticks) {
			calls.add("illusioner:" + ticks);
		}

		@Override
		public void clear(net.minecraft.entity.Entity entity) {
			calls.add("clear");
		}

		@Override
		public void tick(net.minecraft.server.MinecraftServer server) {
			calls.add("tick");
		}
	}
}

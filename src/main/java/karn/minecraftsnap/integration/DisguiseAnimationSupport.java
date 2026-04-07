package karn.minecraftsnap.integration;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class DisguiseAnimationSupport {
	interface Dispatcher {
		void startGhastCharge(Entity entity, int ticks);

		void startEvokerCast(Entity entity, int ticks);

		void startIllusionerCast(Entity entity, int ticks);

		void clear(Entity entity);

		void tick(MinecraftServer server);
	}

	private static Dispatcher dispatcher;

	private DisguiseAnimationSupport() {
	}

	public static void startGhastCharge(Entity entity, int ticks) {
		if (ticks > 0) {
			dispatcher().startGhastCharge(entity, ticks);
		}
	}

	public static void startEvokerCast(Entity entity, int ticks) {
		if (ticks > 0) {
			dispatcher().startEvokerCast(entity, ticks);
		}
	}

	public static void startIllusionerCast(Entity entity, int ticks) {
		if (ticks > 0) {
			dispatcher().startIllusionerCast(entity, ticks);
		}
	}

	public static void clear(Entity entity) {
		dispatcher().clear(entity);
	}

	public static void tick(MinecraftServer server) {
		dispatcher().tick(server);
	}

	static void setDispatcherForTest(Dispatcher testDispatcher) {
		dispatcher = testDispatcher;
	}

	static void resetDispatcherForTest() {
		dispatcher = null;
	}

	private static Dispatcher dispatcher() {
		if (dispatcher == null) {
			dispatcher = new ControllerDispatcher();
		}
		return dispatcher;
	}

	private static final class ControllerDispatcher implements Dispatcher {
		private static final String CONTROLLER_CLASS_NAME = "xyz.nucleoid.disguiselib.impl.PlayerDisguiseAnimationController";
		private static final String ANIMATION_TYPE_CLASS_NAME = "xyz.nucleoid.disguiselib.impl.PlayerDisguiseAnimationType";

		private final Method startMethod;
		private final Method clearMethod;
		private final Method tickMethod;
		private final Object ghastChargeType;
		private final Object evokerCastType;
		private final Object illusionerCastType;

		private ControllerDispatcher() {
			try {
				Class<?> controllerClass = Class.forName(CONTROLLER_CLASS_NAME);
				Class<?> animationTypeClass = Class.forName(ANIMATION_TYPE_CLASS_NAME);
				this.startMethod = findMethod(controllerClass, "start", 3);
				this.clearMethod = findMethod(controllerClass, "clear", 1);
				this.tickMethod = controllerClass.getMethod("tick", MinecraftServer.class);
				this.ghastChargeType = enumConstant(animationTypeClass, "GHAST_CHARGE");
				this.evokerCastType = enumConstant(animationTypeClass, "EVOKER_CAST");
				this.illusionerCastType = enumConstant(animationTypeClass, "ILLUSIONER_CAST");
			} catch (ClassNotFoundException | NoSuchMethodException exception) {
				throw new IllegalStateException("DisguiseLib animation controller initialization failed", exception);
			}
		}

		@Override
		public void startGhastCharge(Entity entity, int ticks) {
			invoke(startMethod, entity, ghastChargeType, ticks);
		}

		@Override
		public void startEvokerCast(Entity entity, int ticks) {
			invoke(startMethod, entity, evokerCastType, ticks);
		}

		@Override
		public void startIllusionerCast(Entity entity, int ticks) {
			invoke(startMethod, entity, illusionerCastType, ticks);
		}

		@Override
		public void clear(Entity entity) {
			invoke(clearMethod, entity);
		}

		@Override
		public void tick(MinecraftServer server) {
			if (server != null) {
				invoke(tickMethod, server);
			}
		}

		private static Method findMethod(Class<?> type, String name, int parameterCount) throws NoSuchMethodException {
			for (Method method : type.getMethods()) {
				if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
					return method;
				}
			}
			throw new NoSuchMethodException(type.getName() + "#" + name + "/" + parameterCount);
		}

		private static Object enumConstant(Class<?> type, String name) {
			return Enum.valueOf(type.asSubclass(Enum.class), name);
		}

		private static void invoke(Method method, Object... arguments) {
			try {
				method.invoke(null, arguments);
			} catch (IllegalAccessException | InvocationTargetException exception) {
				throw new IllegalStateException("DisguiseLib animation call failed", exception);
			}
		}
	}
}

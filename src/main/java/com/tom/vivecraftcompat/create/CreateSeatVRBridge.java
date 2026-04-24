package com.tom.vivecraftcompat.create;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.vivecraft.client_vr.ClientDataHolderVR;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;

import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.ViveCraftCompat;
import com.tom.vivecraftcompat.events.VRUpdateControllersEvent;
import net.neoforged.neoforge.common.NeoForge;

public class CreateSeatVRBridge {
	private static final String CREATE_MOD_ID = "create";

	private static boolean reflectionInitialized;
	private static boolean reflectionUnavailable;
	private static Class<?> abstractContraptionEntityClass;
	private static Class<?> carriageContraptionEntityClass;
	private static Field seatRotationActiveField;
	private static Field yRotationField;
	private static Method getRotationStateMethod;
	private static Method getViewYRotMethod;
	private static Method getPartialTicksMethod;
	private static Method getShortestAngleDiffMethod;
	private static Method wrapAngle180Method;
	private static int previousContraptionId;
	private static float previousContraptionYaw;
	private static boolean initialized;

	public static void init() {
		if (initialized || !isCreateLoaded())
			return;

		initialized = true;
		NeoForge.EVENT_BUS.register(CreateSeatVRBridge.class);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void syncSeatRotation(VRUpdateControllersEvent event) {
		syncSeatRotation();
	}

	public static float afterProcessBindings(float seatedRotation) {
		syncSeatRotation();
		return seatedRotation;
	}

	private static void syncSeatRotation() {
		if (!isCreateLoaded() || !VRMode.isVR()) {
			reset();
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !minecraft.player.isPassenger()) {
			reset();
			return;
		}

		if (!isReflectionReady() || !isCreateSeatRotationActive()) {
			reset();
			return;
		}

		Entity vehicle = minecraft.player.getVehicle();
		if (vehicle == null || !abstractContraptionEntityClass.isInstance(vehicle)) {
			reset();
			return;
		}

		float yaw = getContraptionYaw(vehicle);
		if (previousContraptionId != vehicle.getId()) {
			previousContraptionId = vehicle.getId();
			previousContraptionYaw = yaw;
			return;
		}

		float yawDiff = getShortestAngleDiff(yaw, previousContraptionYaw);
		previousContraptionYaw = yaw;
		if (yawDiff == 0.0F)
			return;

		ClientDataHolderVR dataHolder = ClientDataHolderVR.getInstance();
		dataHolder.vrSettings.worldRotation = wrapAngle180(dataHolder.vrSettings.worldRotation + yawDiff);
	}

	private static float getContraptionYaw(Entity vehicle) {
		try {
			if (carriageContraptionEntityClass.isInstance(vehicle))
				return wrapAngle180((float) getViewYRotMethod.invoke(vehicle, getPartialTicks()));

			Object rotationState = getRotationStateMethod.invoke(vehicle);
			return wrapAngle180(yRotationField.getFloat(rotationState));
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Create contraption seat rotation", e);
			return previousContraptionYaw;
		}
	}

	private static boolean isCreateSeatRotationActive() {
		try {
			return seatRotationActiveField.getBoolean(null);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Create seated rotation state", e);
			return false;
		}
	}

	private static float getPartialTicks() throws ReflectiveOperationException {
		return (float) getPartialTicksMethod.invoke(null);
	}

	private static float getShortestAngleDiff(float current, float previous) {
		try {
			return (float) getShortestAngleDiffMethod.invoke(null, (double) current, (double) previous);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to calculate Create contraption seat rotation delta", e);
			return 0.0F;
		}
	}

	private static float wrapAngle180(float angle) {
		try {
			return (float) wrapAngle180Method.invoke(null, angle);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to wrap Create contraption seat rotation", e);
			return angle % 360.0F;
		}
	}

	private static boolean isReflectionReady() {
		if (reflectionUnavailable)
			return false;
		if (reflectionInitialized)
			return true;

		try {
			abstractContraptionEntityClass = Class.forName("com.simibubi.create.content.contraptions.AbstractContraptionEntity");
			carriageContraptionEntityClass = Class.forName("com.simibubi.create.content.trains.entity.CarriageContraptionEntity");
			Class<?> passengerRotationClass = Class
					.forName("com.simibubi.create.content.contraptions.actors.seat.ContraptionPlayerPassengerRotation");
			Class<?> animationTickHolderClass = Class.forName("net.createmod.catnip.animation.AnimationTickHolder");
			Class<?> angleHelperClass = Class.forName("net.createmod.catnip.math.AngleHelper");

			seatRotationActiveField = passengerRotationClass.getDeclaredField("active");
			seatRotationActiveField.setAccessible(true);
			getRotationStateMethod = abstractContraptionEntityClass.getMethod("getRotationState");
			getViewYRotMethod = carriageContraptionEntityClass.getMethod("getViewYRot", float.class);
			getPartialTicksMethod = animationTickHolderClass.getMethod("getPartialTicks");
			getShortestAngleDiffMethod = angleHelperClass.getMethod("getShortestAngleDiff", double.class, double.class);
			wrapAngle180Method = angleHelperClass.getMethod("wrapAngle180", float.class);

			Class<?> rotationStateClass = null;
			for (Class<?> declaredClass : abstractContraptionEntityClass.getDeclaredClasses()) {
				if ("ContraptionRotationState".equals(declaredClass.getSimpleName())) {
					rotationStateClass = declaredClass;
					break;
				}
			}
			if (rotationStateClass == null)
				throw new NoSuchFieldException("ContraptionRotationState");

			yRotationField = rotationStateClass.getField("yRotation");
			reflectionInitialized = true;
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to initialize Create contraption seat VR rotation reflection", e);
			return false;
		}
	}

	private static boolean isCreateLoaded() {
		return ModList.get().isLoaded(CREATE_MOD_ID);
	}

	private static void reset() {
		previousContraptionId = 0;
		previousContraptionYaw = 0.0F;
	}

	private static void disableReflection(String message, Exception exception) {
		if (!reflectionUnavailable)
			ViveCraftCompat.LOGGER.warn(message, exception);
		reflectionUnavailable = true;
		reset();
	}
}

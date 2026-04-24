package com.tom.vivecraftcompat.simulated;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

import it.unimi.dsi.fastutil.Function;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.gameplay.VRPlayer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.ViveCraftCompat;

public class SableVRBridge {
	private static boolean initialized;
	private static boolean reflectionInitialized;
	private static boolean reflectionUnavailable;
	private static Object helper;
	private static Class<?> clientSubLevelClass;
	private static Class<?> levelPoseProviderExtensionClass;
	private static Method getContainingLevelPositionMethod;
	private static Method getContainingEntityMethod;
	private static Method getTrackingOrVehicleSubLevelMethod;
	private static Method getVehicleSubLevelMethod;
	private static Method projectOutOfSubLevelMethod;
	private static Method getVelocityVec3Method;
	private static Method getEyePositionInterpolatedMethod;
	private static Method pushPoseSupplierMethod;
	private static Method popPoseSupplierMethod;
	private static Method logicalPoseMethod;
	private static Method renderPoseMethod;
	private static Method renderPosePartialTickMethod;
	private static Method orientationMethod;
	private static Method uniqueIdMethod;
	private static Method transformPositionInverseVec3Method;
	private static Object previousSeatSubLevelKey;
	private static Object previousSeatSubLevel;
	private static Object previousStandingSubLevelKey;
	private static boolean seatedVrTransformActive;
	private static boolean seatedVrOrientationTransformActive;
	private static int vrGuiTransformSuppression;
	private static final Quaterniond baseSeatOrientation = new Quaterniond();
	private static final Quaterniond seatedVrTransform = new Quaterniond();
	private static final Quaterniond previousStandingOrientation = new Quaterniond();
	private static final Vector3d seatedVrPositionOffset = new Vector3d();
	private static final Vector3d vanillaVrEyePosition = new Vector3d();
	private static final Vector3d sableVrEyePosition = new Vector3d();
	private static final Vector3d previousSeatExitVelocity = new Vector3d();

	public static void init() {
		if (initialized)
			return;

		initialized = true;
	}

	public static void syncSeatedPhysicsRotationFrame() {
		syncSeatedPhysicsRotation();
	}

	public static void pushGuiVrTransformSuppression() {
		vrGuiTransformSuppression++;
	}

	public static void popGuiVrTransformSuppression() {
		if (vrGuiTransformSuppression > 0)
			vrGuiTransformSuppression--;
	}

	public static boolean shouldStabilizeGuiVrModelView() {
		return seatedVrTransformActive && seatedVrOrientationTransformActive;
	}

	public static Vec3 transformVrPosition(VRData data, Vec3 position) {
		if (!shouldTransformVrData(data) || position == null)
			return position;

		Vector3d transformed = new Vector3d(position.x - vanillaVrEyePosition.x, position.y - vanillaVrEyePosition.y,
				position.z - vanillaVrEyePosition.z);
		if (seatedVrOrientationTransformActive)
			seatedVrTransform.transform(transformed);
		return new Vec3(sableVrEyePosition.x + transformed.x, sableVrEyePosition.y + transformed.y,
				sableVrEyePosition.z + transformed.z);
	}

	public static Vector3f transformVrDirection(VRData data, Vector3f direction) {
		if (!shouldTransformVrOrientation(data) || direction == null)
			return direction;

		Vector3d transformed = seatedVrTransform.transform(new Vector3d(direction.x, direction.y, direction.z));
		return new Vector3f((float) transformed.x, (float) transformed.y, (float) transformed.z);
	}

	public static Matrix4f transformVrMatrix(VRData data, Matrix4f matrix) {
		if (!shouldTransformVrOrientation(data) || matrix == null)
			return matrix;

		return new Matrix4f().rotation(toQuaternionf(seatedVrTransform)).mul(matrix);
	}

	public static Vec3 getSeatedHandRayOrigin(Entity player, Vec3 controllerOrigin, float partialTick) {
		if (!VRMode.isVR() || player == null || controllerOrigin == null || !isReflectionReady())
			return controllerOrigin;

		Object subLevel = getTrackingOrVehicleSubLevel(player);
		if (subLevel == null)
			return controllerOrigin;

		try {
			if (seatedVrTransformActive)
				return controllerOrigin;

			Vec3 sableEye = getSeatedEyePosition(player, partialTick);
			return sableEye.add(controllerOrigin.subtract(player.getEyePosition(partialTick)));
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to project a VR hand origin from a Sable seated player", e);
			return controllerOrigin;
		}
	}

	public static Vec3 getSeatedHandRayDirection(Entity player, Vec3 rawDirection) {
		if (rawDirection == null)
			return rawDirection;

		return rawDirection.normalize();
	}

	public static Vec3 getSeatedEyePosition(Entity player, float partialTick) throws ReflectiveOperationException {
		if (player == null)
			return Vec3.ZERO;
		if (!VRMode.isVR() || !isReflectionReady() || getTrackingOrVehicleSubLevel(player) == null)
			return player.getEyePosition(partialTick);

		return toVec3(getEyePositionInterpolatedMethod.invoke(helper, player, partialTick),
				player.getEyePosition(partialTick));
	}

	public static boolean isInSeatedInteractionRange(Player player, Position target, double reachBuffer) {
		if (player == null || target == null)
			return false;

		double distance = player.blockInteractionRange() + reachBuffer;
		Vec3 projectedTarget = projectOutOfSubLevel(player.level(), new Vec3(target.x(), target.y(), target.z()));
		try {
			return getSeatedEyePosition(player, getPartialTick()).distanceToSqr(projectedTarget) < distance * distance;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Sable seated interaction range", e);
			return player.getEyePosition().distanceToSqr(projectedTarget) < distance * distance;
		}
	}

	public static Vec3 transformPositionIntoContainingSubLevel(Level level, Position containedPosition, Vec3 worldPosition) {
		if (level == null || containedPosition == null || worldPosition == null || !isReflectionReady()
				|| transformPositionInverseVec3Method == null)
			return worldPosition;

		Object subLevel = getContainingSubLevel(level,
				new Vec3(containedPosition.x(), containedPosition.y(), containedPosition.z()));
		if (subLevel == null)
			return worldPosition;

		try {
			Object pose = getSubLevelPose(subLevel);
			if (pose == null)
				return worldPosition;
			return toVec3(transformPositionInverseVec3Method.invoke(pose, worldPosition), worldPosition);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to transform a VR hand position into a Sable sublevel", e);
			return worldPosition;
		}
	}

	public static Vec3 transformDirectionIntoContainingSubLevel(Level level, Position containedPosition, Vec3 worldDirection) {
		if (level == null || containedPosition == null || worldDirection == null || !isReflectionReady())
			return worldDirection;

		Object subLevel = getContainingSubLevel(level,
				new Vec3(containedPosition.x(), containedPosition.y(), containedPosition.z()));
		if (subLevel == null)
			return worldDirection;

		Quaterniond orientation = getSubLevelSeatSyncOrientation(subLevel);
		if (orientation == null)
			return worldDirection;

		Vec3 normalized = worldDirection.normalize();
		Vector3d local = orientation.transformInverse(new Vector3d(normalized.x, normalized.y, normalized.z));
		return new Vec3(local.x, local.y, local.z).normalize();
	}

	public static Direction getSeatedPlacementHorizontalDirection(Level level, Player player, Position targetPosition) {
		Vec3 localView = getSeatedPlacementViewVector(level, player, targetPosition);
		if (localView == null)
			return null;

		if (localView.x * localView.x + localView.z * localView.z < 1.0E-6D)
			return null;

		return Direction.getNearest(localView.x, 0.0D, localView.z);
	}

	public static Direction getSeatedPlacementVerticalDirection(Level level, Player player, Position targetPosition) {
		Vec3 localView = getSeatedPlacementViewVector(level, player, targetPosition);
		if (localView == null)
			return null;

		return localView.y < 0.0D ? Direction.DOWN : Direction.UP;
	}

	public static Direction[] getSeatedPlacementDirections(Level level, Player player, Position targetPosition) {
		Vec3 localView = getSeatedPlacementViewVector(level, player, targetPosition);
		if (localView == null)
			return null;

		Direction[] directions = Direction.values().clone();
		Arrays.sort(directions, Comparator.comparingDouble(direction -> -directionDot(direction, localView)));
		return directions;
	}

	public static Float getSeatedPlacementYaw(Level level, Player player, Position targetPosition) {
		Vec3 localView = getSeatedPlacementViewVector(level, player, targetPosition);
		if (localView == null || localView.x * localView.x + localView.z * localView.z < 1.0E-6D)
			return null;

		return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-localView.x, localView.z)));
	}

	private static Vec3 getSeatedPlacementViewVector(Level level, Player player, Position targetPosition) {
		if (level == null || player == null || targetPosition == null || !VRMode.isVR() || !isReflectionReady())
			return null;

		if (getTrackingOrVehicleSubLevel(player) == null)
			return null;

		Object targetSubLevel = getContainingSubLevel(level, targetPosition);
		if (targetSubLevel == null)
			return null;

		Quaterniond orientation = getSubLevelOrientation(targetSubLevel);
		if (orientation == null)
			return null;

		Vec3 worldView = getVrWorldViewVector(player);
		Vector3d local = orientation.transformInverse(new Vector3d(worldView.x, worldView.y, worldView.z));
		return new Vec3(local.x, local.y, local.z).normalize();
	}

	private static Vec3 getVrWorldViewVector(Player player) {
		VRPlayer vrPlayer = VRPlayer.get();
		if (vrPlayer != null && vrPlayer.vrdata_world_render != null) {
			Vector3f direction = vrPlayer.vrdata_world_render.hmd.getDirection();
			if (direction != null) {
				Vec3 worldView = new Vec3(direction.x, direction.y, direction.z);
				if (worldView.lengthSqr() > 1.0E-6D)
					return worldView.normalize();
			}
		}

		return player.getViewVector(1.0F).normalize();
	}

	private static double directionDot(Direction direction, Vec3 vector) {
		return direction.getStepX() * vector.x + direction.getStepY() * vector.y + direction.getStepZ() * vector.z;
	}

	public static HitResult clipWithRenderPose(Level level, ClipContext context) {
		if (level == null || context == null)
			return null;
		if (!isReflectionReady() || levelPoseProviderExtensionClass == null || !levelPoseProviderExtensionClass.isInstance(level))
			return level.clip(context);

		boolean pushed = false;
		try {
			pushPoseSupplierMethod.invoke(level, (Function<Object, Object>) SableVRBridge::getRenderPoseForClip);
			pushed = true;
			return level.clip(context);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to raytrace a VR hand ray through Sable render poses", e);
			return level.clip(context);
		} finally {
			if (pushed) {
				try {
					popPoseSupplierMethod.invoke(level);
				} catch (ReflectiveOperationException | RuntimeException e) {
					disableReflection("Failed to restore Sable render-pose raytrace state", e);
				}
			}
		}
	}

	private static Object getRenderPoseForClip(Object subLevel) {
		try {
			return getSubLevelPose(subLevel);
		} catch (ReflectiveOperationException | RuntimeException e) {
			try {
				return logicalPoseMethod.invoke(subLevel);
			} catch (ReflectiveOperationException | RuntimeException ignored) {
				return null;
			}
		}
	}

	public static Vec3 projectRayOrigin(Vec3 origin) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!VRMode.isVR() || minecraft.level == null || origin == null)
			return origin;

		return projectOutOfSubLevel(minecraft.level, origin);
	}

	public static Vec3 projectOutOfSubLevel(Level level, Vec3 position) {
		if (level == null || position == null || !isReflectionReady())
			return position;

		Object subLevel = getContainingSubLevel(level, position);
		if (subLevel == null)
			return position;

		try {
			Object projected = projectOutOfSubLevelMethod.invoke(helper, level, position);
			return toVec3(projected, position);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to project a VR ray origin out of a Sable sublevel", e);
			return position;
		}
	}

	public static Vector3f projectRayDirection(Vec3 origin, Vector3f direction) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!VRMode.isVR() || minecraft.level == null || origin == null || direction == null || !isReflectionReady())
			return direction;

		Object subLevel = getContainingSubLevel(minecraft.level, origin);
		if (subLevel == null)
			return direction;

		Quaterniond orientation = getSubLevelOrientation(subLevel);
		if (orientation == null)
			return direction;

		Vector3d projected = orientation.transform(new Vector3d(direction.x, direction.y, direction.z));
		return new Vector3f((float) projected.x, (float) projected.y, (float) projected.z);
	}

	private static void syncSeatedPhysicsRotation() {
		if (!VRMode.isVR()) {
			resetSeat();
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !isReflectionReady()) {
			resetSeat();
			return;
		}

		Object trackingOrVehicleSubLevel = getTrackingOrVehicleSubLevel(minecraft.player);
		Object vehicleSubLevel = getVehicleSubLevel(minecraft.player);
		if (trackingOrVehicleSubLevel == null) {
			if (previousSeatSubLevel != null)
				applyPreviousSeatExitVelocity(minecraft.player);
			resetSeat();
			return;
		}

		Quaterniond orientation = getSubLevelOrientation(trackingOrVehicleSubLevel);
		if (orientation == null) {
			resetSeat();
			return;
		}
		if (!updateSeatedVrPositionOffset(minecraft))
			return;

		seatedVrTransformActive = true;
		if (vehicleSubLevel != null) {
			seatedVrOrientationTransformActive = true;
			resetStandingRotationState();
			Object subLevelKey = getSubLevelKey(vehicleSubLevel);
			if (previousSeatSubLevelKey == null || !previousSeatSubLevelKey.equals(subLevelKey)) {
				previousSeatSubLevelKey = subLevelKey;
				baseSeatOrientation.set(orientation);
			}
			seatedVrTransform.set(orientation).mul(new Quaterniond(baseSeatOrientation).conjugate()).normalize();
			previousSeatSubLevel = vehicleSubLevel;
			updatePreviousSeatExitVelocity(minecraft.player);
		} else {
			seatedVrOrientationTransformActive = false;
			seatedVrTransform.identity();
			previousSeatSubLevelKey = null;
			baseSeatOrientation.identity();
			previousSeatSubLevel = null;
			previousSeatExitVelocity.zero();
			syncStandingWorldRotation(trackingOrVehicleSubLevel);
		}
	}

	private static boolean updateSeatedVrPositionOffset(Minecraft minecraft) {
		if (minecraft.player == null) {
			seatedVrPositionOffset.zero();
			vanillaVrEyePosition.zero();
			sableVrEyePosition.zero();
			return true;
		}

		try {
			float partialTick = getPartialTick();
			Vec3 vanillaEye = minecraft.player.getEyePosition(partialTick);
			Vec3 sableEye = getSeatedEyePosition(minecraft.player, partialTick);
			vanillaVrEyePosition.set(vanillaEye.x, vanillaEye.y, vanillaEye.z);
			sableVrEyePosition.set(sableEye.x, sableEye.y, sableEye.z);
			seatedVrPositionOffset.set(sableEye.x - vanillaEye.x, sableEye.y - vanillaEye.y,
					sableEye.z - vanillaEye.z);
			if (!Double.isFinite(seatedVrPositionOffset.x) || !Double.isFinite(seatedVrPositionOffset.y)
					|| !Double.isFinite(seatedVrPositionOffset.z)) {
				seatedVrPositionOffset.zero();
				vanillaVrEyePosition.zero();
				sableVrEyePosition.zero();
			}
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to sync a Vivecraft VR pose with a Sable sublevel position", e);
			return false;
		}
	}

	private static void updatePreviousSeatExitVelocity(Entity player) {
		previousSeatExitVelocity.zero();
		if (player == null || getVelocityVec3Method == null)
			return;

		try {
			Vec3 velocity = toVec3(getVelocityVec3Method.invoke(helper, player.level(), player.position()), Vec3.ZERO);
			if (!isFinite(velocity) || velocity.lengthSqr() < 1.0E-8D)
				return;

			Vec3 perTickVelocity = velocity.scale(0.05D);
			previousSeatExitVelocity.set(perTickVelocity.x, perTickVelocity.y, perTickVelocity.z);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Sable vehicle velocity for a VR seat dismount", e);
		}
	}

	private static void applyPreviousSeatExitVelocity(Entity player) {
		if (player == null || previousSeatExitVelocity.lengthSquared() < 1.0E-8D)
			return;

		Vec3 currentVelocity = player.getDeltaMovement();
		if (!isFinite(currentVelocity))
			currentVelocity = Vec3.ZERO;

		player.setDeltaMovement(currentVelocity.add(previousSeatExitVelocity.x, previousSeatExitVelocity.y,
				previousSeatExitVelocity.z));
	}

	private static boolean isFinite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}

	private static void syncStandingWorldRotation(Object trackingSubLevel) {
		if (trackingSubLevel == null)
			return;

		Quaterniond current = getSubLevelSeatSyncOrientation(trackingSubLevel);
		if (current == null) {
			resetStandingRotationState();
			return;
		}

		Object subLevelKey = getSubLevelKey(trackingSubLevel);
		if (previousStandingSubLevelKey == null || !previousStandingSubLevelKey.equals(subLevelKey)) {
			previousStandingSubLevelKey = subLevelKey;
			previousStandingOrientation.set(current);
			return;
		}

		Quaterniond relativeOrientation = new Quaterniond(current).div(previousStandingOrientation, new Quaterniond());
		previousStandingOrientation.set(current);
		if (Math.abs(relativeOrientation.w) < 1.0E-6D)
			return;

		double angleDiff = 2.0D * relativeOrientation.y / relativeOrientation.w;
		if (!Double.isFinite(angleDiff) || Math.abs(angleDiff) < 1.0E-6D)
			return;

		ClientDataHolderVR dataHolder = ClientDataHolderVR.getInstance();
		dataHolder.vrSettings.worldRotation = Mth.wrapDegrees(dataHolder.vrSettings.worldRotation
				+ (float) Math.toDegrees(angleDiff));
	}

	private static boolean shouldTransformVrData(VRData data) {
		if (!seatedVrTransformActive || vrGuiTransformSuppression > 0 || data == null)
			return false;

		VRPlayer vrPlayer = VRPlayer.get();
		return vrPlayer != null && (data == vrPlayer.vrdata_world_pre || data == vrPlayer.vrdata_world_post
				|| data == vrPlayer.vrdata_world_render);
	}

	private static boolean shouldTransformVrOrientation(VRData data) {
		return seatedVrOrientationTransformActive && shouldTransformVrData(data);
	}

	private static Quaternionf toQuaternionf(Quaterniond quaternion) {
		return new Quaternionf((float) quaternion.x, (float) quaternion.y, (float) quaternion.z,
				(float) quaternion.w);
	}

	private static Object getContainingSubLevel(Level level, Position position) {
		try {
			return getContainingLevelPositionMethod.invoke(helper, level, position);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query the Sable sublevel containing a VR ray", e);
			return null;
		}
	}

	private static Object getTrackingOrVehicleSubLevel(Entity entity) {
		try {
			return getTrackingOrVehicleSubLevelMethod.invoke(helper, entity);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query the Sable sublevel carrying the VR player", e);
		}
		return null;
	}

	private static Object getVehicleSubLevel(Entity entity) {
		try {
			return getVehicleSubLevelMethod.invoke(helper, entity);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query the Sable vehicle sublevel carrying the VR player", e);
		}
		return null;
	}

	private static Object getSubLevelKey(Object subLevel) {
		if (subLevel == null || uniqueIdMethod == null)
			return subLevel;

		try {
			Object uniqueId = uniqueIdMethod.invoke(subLevel);
			return uniqueId != null ? uniqueId : subLevel;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return subLevel;
		}
	}

	private static Quaterniond getSubLevelOrientation(Object subLevel) {
		try {
			Object pose = getSubLevelPose(subLevel);
			if (pose == null)
				return null;

			Object orientation = orientationMethod.invoke(pose);
			if (orientation instanceof Quaterniondc quaternion)
				return new Quaterniond(quaternion);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Sable sublevel orientation", e);
		}
		return null;
	}

	private static Quaterniond getSubLevelSeatSyncOrientation(Object subLevel) {
		try {
			Object pose = getSubLevelRenderPose(subLevel);
			if (pose == null)
				return null;

			Object orientation = orientationMethod.invoke(pose);
			if (orientation instanceof Quaterniondc quaternion)
				return new Quaterniond(quaternion);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Sable sublevel seat orientation", e);
		}
		return null;
	}

	private static Object getSubLevelPose(Object subLevel) throws ReflectiveOperationException {
		if (canInvoke(renderPosePartialTickMethod, subLevel))
			return renderPosePartialTickMethod.invoke(subLevel, getPartialTick());
		if (canInvoke(renderPoseMethod, subLevel))
			return renderPoseMethod.invoke(subLevel);
		return logicalPoseMethod.invoke(subLevel);
	}

	private static Object getSubLevelRenderPose(Object subLevel) throws ReflectiveOperationException {
		if (canInvoke(renderPoseMethod, subLevel))
			return renderPoseMethod.invoke(subLevel);
		return logicalPoseMethod.invoke(subLevel);
	}

	private static boolean canInvoke(Method method, Object target) {
		return method != null && target != null && method.getDeclaringClass().isInstance(target);
	}

	private static float getPartialTick() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return 1.0F;
		boolean advanceGameTime = minecraft.player == null || !minecraft.level.tickRateManager().isEntityFrozen(minecraft.player);
		return minecraft.getTimer().getGameTimeDeltaPartialTick(advanceGameTime);
	}

	private static Vec3 toVec3(Object value, Vec3 fallback) {
		if (value instanceof Vec3 vec3)
			return vec3;
		if (value instanceof Vector3dc vector)
			return new Vec3(vector.x(), vector.y(), vector.z());
		return fallback;
	}

	private static boolean isReflectionReady() {
		if (reflectionInitialized)
			return !reflectionUnavailable;
		if (reflectionUnavailable)
			return false;

		try {
			Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
			Field helperField = sableClass.getField("HELPER");
			helper = helperField.get(null);
		} catch (ClassNotFoundException e) {
			reflectionUnavailable = true;
			return false;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to initialize Sable VR reflection", e);
			return false;
		}

		try {
			Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
			clientSubLevelClass = findOptionalClass("dev.ryanhcode.sable.sublevel.ClientSubLevel");
			levelPoseProviderExtensionClass = findOptionalClass(
					"dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension");
			Class<?> helperClass = helper.getClass();
			getContainingLevelPositionMethod = helperClass.getMethod("getContaining", Level.class, Position.class);
			getContainingEntityMethod = helperClass.getMethod("getContaining", Entity.class);
			getTrackingOrVehicleSubLevelMethod = helperClass.getMethod("getTrackingOrVehicleSubLevel", Entity.class);
			getVehicleSubLevelMethod = helperClass.getMethod("getVehicleSubLevel", Entity.class);
			projectOutOfSubLevelMethod = helperClass.getMethod("projectOutOfSubLevel", Level.class, Vec3.class);
			getVelocityVec3Method = findOptionalMethod(helperClass, "getVelocity", Level.class, Vec3.class);
			getEyePositionInterpolatedMethod = helperClass.getMethod("getEyePositionInterpolated", Entity.class, float.class);
			logicalPoseMethod = subLevelClass.getMethod("logicalPose");
			orientationMethod = logicalPoseMethod.getReturnType().getMethod("orientation");
			uniqueIdMethod = subLevelClass.getMethod("getUniqueId");
			transformPositionInverseVec3Method = findOptionalMethod(logicalPoseMethod.getReturnType(),
					"transformPositionInverse", Vec3.class);

			if (levelPoseProviderExtensionClass != null) {
				pushPoseSupplierMethod = levelPoseProviderExtensionClass.getMethod("sable$pushPoseSupplier",
						Function.class);
				popPoseSupplierMethod = levelPoseProviderExtensionClass.getMethod("sable$popPoseSupplier");
			}

			renderPoseMethod = findOptionalMethod(subLevelClass, "renderPose");
			renderPosePartialTickMethod = findOptionalMethod(subLevelClass, "renderPose", float.class);

			if (clientSubLevelClass != null) {
				if (renderPoseMethod == null)
					renderPoseMethod = findOptionalMethod(clientSubLevelClass, "renderPose");
				if (renderPosePartialTickMethod == null)
					renderPosePartialTickMethod = findOptionalMethod(clientSubLevelClass, "renderPose", float.class);
			}

			reflectionInitialized = true;
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to initialize Sable VR reflection", e);
			return false;
		}
	}

	private static Class<?> findOptionalClass(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private static Method findOptionalMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
		try {
			return owner.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	private static void resetSeat() {
		previousSeatSubLevelKey = null;
		previousSeatSubLevel = null;
		resetStandingRotationState();
		seatedVrTransformActive = false;
		seatedVrOrientationTransformActive = false;
		baseSeatOrientation.identity();
		seatedVrTransform.identity();
		seatedVrPositionOffset.zero();
		vanillaVrEyePosition.zero();
		sableVrEyePosition.zero();
		previousSeatExitVelocity.zero();
	}

	private static void resetStandingRotationState() {
		previousStandingSubLevelKey = null;
		previousStandingOrientation.identity();
	}

	private static void disableReflection(String message, Exception exception) {
		if (!reflectionUnavailable)
			ViveCraftCompat.LOGGER.warn(message, exception);
		reflectionUnavailable = true;
		resetSeat();
	}
}

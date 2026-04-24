package com.tom.vivecraftcompat.mixin.compat.simulated;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.ryanhcode.sable.Sable;
import com.tom.vivecraftcompat.VRHelper;
import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.ViveCraftCompat;
import com.tom.vivecraftcompat.simulated.SableVRBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(GameRenderer.class)
public class SimulatedThrottleLeverGameRendererMixin {
	private static final String THROTTLE_LEVER_CLIENT_GRIP_HANDLER =
			"dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverClientGripHandler";
	private static final Collection<Object> NO_LEVERS = List.of();
	private static boolean reflectionUnavailable;
	private static Method getNearbyThrottleLeversMethod;
	private static Method raycastLeverMethod;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "pick(F)V", at = @At("TAIL"))
	private void vivecraftcompat$pickThrottleLeverFromVrHand(float partialTick, CallbackInfo ci) {
		if (!VRMode.isVRHandAiming())
			return;
		if (this.minecraft == null)
			return;

		LocalPlayer player = this.minecraft.player;
		if (player == null)
			return;

		Vec3 eyePos = SableVRBridge.getSeatedHandRayOrigin(player, VRHelper.getRayOrigin(), partialTick);
		Vec3 direction = SableVRBridge.getSeatedHandRayDirection(player,
				new Vec3(VRHelper.getRayDirection().x, VRHelper.getRayDirection().y, VRHelper.getRayDirection().z));

		HitResult mcHitResult = this.minecraft.hitResult;
		double minDistance = mcHitResult != null && mcHitResult.getType() != HitResult.Type.MISS
				? Sable.HELPER.distanceSquaredWithSubLevels(player.level(), eyePos, mcHitResult.getLocation())
				: Double.MAX_VALUE;

		for (Object lever : getNearbyThrottleLevers()) {
			if (!(lever instanceof BlockEntity blockEntity) || blockEntity.isRemoved())
				continue;

			Double hitResultDistance = raycastLever(eyePos, direction, lever, partialTick);
			if (hitResultDistance == null || hitResultDistance >= minDistance)
				continue;

			minDistance = hitResultDistance;
			BlockPos blockPos = blockEntity.getBlockPos();
			this.minecraft.hitResult = new BlockHitResult(blockPos.getCenter(), Direction.UP, blockPos, false);
		}
	}

	@SuppressWarnings("unchecked")
	private static Collection<Object> getNearbyThrottleLevers() {
		if (!isReflectionReady())
			return NO_LEVERS;

		try {
			return (Collection<Object>) getNearbyThrottleLeversMethod.invoke(null);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Simulated nearby throttle levers", e);
			return NO_LEVERS;
		}
	}

	private static Double raycastLever(Vec3 eyePos, Vec3 direction, Object lever, float partialTick) {
		if (!isReflectionReady())
			return null;

		try {
			Object result = raycastLeverMethod.invoke(null, eyePos, direction, lever, partialTick);
			return result instanceof Double value ? value : null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to raycast a Simulated throttle lever in VR", e);
			return null;
		}
	}

	private static boolean isReflectionReady() {
		if (reflectionUnavailable)
			return false;
		if (getNearbyThrottleLeversMethod != null && raycastLeverMethod != null)
			return true;

		try {
			Class<?> gripHandlerClass = Class.forName(THROTTLE_LEVER_CLIENT_GRIP_HANDLER);
			getNearbyThrottleLeversMethod = gripHandlerClass.getMethod("getNearbyThrottleLevers");
			Method candidate = null;
			for (Method method : gripHandlerClass.getMethods()) {
				if (!"raycastLever".equals(method.getName()))
					continue;
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length == 4 && parameterTypes[0] == Vec3.class && parameterTypes[1] == Vec3.class
						&& parameterTypes[3] == float.class) {
					candidate = method;
					break;
				}
			}
			raycastLeverMethod = candidate;
			if (raycastLeverMethod == null)
				throw new NoSuchMethodException("raycastLever");
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to initialize Simulated throttle lever VR compatibility", e);
			return false;
		}
	}

	private static void disableReflection(String message, Exception exception) {
		if (!reflectionUnavailable)
			ViveCraftCompat.LOGGER.warn(message, exception);
		reflectionUnavailable = true;
	}

}

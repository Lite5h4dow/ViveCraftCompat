package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.tom.vivecraftcompat.VRHelper;
import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.simulated.SableVRBridge;

@Mixin(targets = "dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlock")
public class SimulatedSteeringWheelBlockMixin {

	@Redirect(
			method = "lookingAtWheel(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;FLnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"),
			remap = false)
	private static Vec3 useVRRayOrigin(Player player, float partialTick) {
		return VRMode.isVRHandAiming() ? SableVRBridge.getSeatedHandRayOrigin(player, VRHelper.getRayOrigin(), partialTick)
				: player.getEyePosition(partialTick);
	}

	@Redirect(
			method = "lookingAtWheel(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;FLnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"),
			remap = false)
	private static Vec3 useVRRayDirection(Player player, float partialTick) {
		if (!VRMode.isVRHandAiming())
			return player.getViewVector(partialTick);

		var direction = VRHelper.getRayDirection();
		return SableVRBridge.getSeatedHandRayDirection(player, new Vec3(direction.x, direction.y, direction.z));
	}
}

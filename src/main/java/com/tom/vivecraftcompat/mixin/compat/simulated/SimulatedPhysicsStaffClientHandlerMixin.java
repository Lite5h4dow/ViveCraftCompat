package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.simulated.SimulatedVRInputBridge;

@Mixin(targets = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler")
public class SimulatedPhysicsStaffClientHandlerMixin {

	@Inject(method = "getStaffFocusPos", at = @At("HEAD"), cancellable = true, remap = false)
	private static void vivecraftcompat$useVRHandFocus(Player player, boolean mainHand, float partialTick,
			CallbackInfoReturnable<Vec3> cir) {
		if (player.isLocalPlayer() && VRMode.isVRHandAiming())
			cir.setReturnValue(SimulatedVRInputBridge.getStaffHandRayOrigin(player));
	}

	@Inject(method = "isRotating", at = @At("HEAD"), cancellable = true, remap = false)
	private void vivecraftcompat$useVRRotateMode(CallbackInfoReturnable<Boolean> cir) {
		if (SimulatedVRInputBridge.isPhysicsStaffRotateMode())
			cir.setReturnValue(true);
	}

	@Redirect(
			method = "onItemUsed",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"),
			remap = false)
	private HitResult vivecraftcompat$pickTargetFromHand(LocalPlayer player, double range, float partialTick, boolean hitFluids) {
		return SimulatedVRInputBridge.pickFromStaffHand(player, range, partialTick, hitFluids);
	}

	@Redirect(
			method = "startDraggingSubLevel",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEyePosition()Lnet/minecraft/world/phys/Vec3;"),
			remap = false)
	private Vec3 vivecraftcompat$useHandDistanceOrigin(LocalPlayer player) {
		return VRMode.isVRHandAiming() ? SimulatedVRInputBridge.getStaffHandRayOrigin(player) : player.getEyePosition();
	}

	@Redirect(
			method = "sendDraggingData",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"),
			remap = false)
	private Vec3 vivecraftcompat$useHandDragDirection(Player player) {
		return VRMode.isVRHandAiming() ? SimulatedVRInputBridge.getStaffHandRayDirection(player) : player.getLookAngle();
	}
}

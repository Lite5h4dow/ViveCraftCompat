package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.simulated.SimulatedVRInputBridge;

@Mixin(targets = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItemRenderer")
public class SimulatedPhysicsStaffItemRendererMixin {

	@Redirect(
			method = "render",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"),
			remap = false)
	private Vec3 vivecraftcompat$aimStaffModelFromHand(Player player, float partialTick) {
		return VRMode.isVRHandAiming() ? SimulatedVRInputBridge.getStaffHandRayOrigin(player) : player.getEyePosition(partialTick);
	}
}

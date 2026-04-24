package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;

import com.tom.vivecraftcompat.simulated.SimulatedVRInputBridge;

@Mixin(targets = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffRenderHandler")
public class SimulatedPhysicsStaffRenderHandlerMixin {

	@Redirect(
			method = "updateHoverPos",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"),
			remap = false)
	private static HitResult vivecraftcompat$pickHoverFromHand(LocalPlayer player, double range, float partialTick, boolean hitFluids) {
		return SimulatedVRInputBridge.pickFromStaffHand(player, range, partialTick, hitFluids);
	}
}

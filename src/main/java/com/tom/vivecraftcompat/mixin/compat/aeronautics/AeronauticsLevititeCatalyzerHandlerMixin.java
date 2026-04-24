package com.tom.vivecraftcompat.mixin.compat.aeronautics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.tom.vivecraftcompat.VRHelper;
import com.tom.vivecraftcompat.VRMode;

@Mixin(targets = "dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeCatalyzerHandler")
public class AeronauticsLevititeCatalyzerHandlerMixin {

	@Redirect(
			method = "gatherContext(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/ClipContext;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEyePosition()Lnet/minecraft/world/phys/Vec3;"),
			remap = false)
	private static Vec3 useVRRayOrigin(Player player) {
		return VRMode.isVRHandAiming() ? VRHelper.getRayOrigin() : player.getEyePosition();
	}
}

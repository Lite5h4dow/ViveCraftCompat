package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.tom.vivecraftcompat.VRHelper;
import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.simulated.SableVRBridge;

@Mixin(targets = "dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity")
public class SimulatedLinkedTypewriterBlockEntityMixin {

	// LinkedTypewriterBlockEntity.playerInRange() measures from player.getEyePosition(),
	// which in VR is the real-world headset position relative to the play area, not the
	// block-space position of the player entity. Standing at the typewriter with your head
	// anywhere but dead centre blows the 4.5-block interaction range check on the first
	// tick, so the block fires disconnectUser() and the player is dropped off the keyboard
	// immediately. Measure from the seated VR hand ray origin instead.
	@Redirect(
			method = "playerInRange(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEyePosition()Lnet/minecraft/world/phys/Vec3;"),
			remap = false)
	private static Vec3 useVRRayOrigin(Player player) {
		return VRMode.isVRHandAiming() ? SableVRBridge.getSeatedHandRayOrigin(player, VRHelper.getRayOrigin(), 1.0F)
				: player.getEyePosition();
	}
}

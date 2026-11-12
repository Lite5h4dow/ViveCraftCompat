package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.player.Player;

import com.tom.vivecraftcompat.simulated.SableVRBridge;

/**
 * Seated VR reach for the sublevel handle.
 *
 * ClientHandleHandler.activeTick gates the extendo grip on
 * BlockHoldInteraction.inInteractionRange(player, Vector3dc, range), measured
 * from the player entity's eye position. When the player is seated in a Sable
 * sublevel that eye position is not where the VR camera is, so the grip drops
 * while the hand is still on it. Route the check through the same seated-aware
 * calculation the steering wheel uses. The range argument is absolute here, so
 * it is passed through untouched rather than added to the player's reach.
 */
@Mixin(targets = "dev.simulated_team.simulated.content.blocks.handle.ClientHandleHandler")
public class SimulatedClientHandleHandlerMixin {

	@Redirect(
			method = "activeTick",
			at = @At(value = "INVOKE", target = "Ldev/simulated_team/simulated/util/hold_interaction/BlockHoldInteraction;inInteractionRange(Lnet/minecraft/world/entity/player/Player;Lorg/joml/Vector3dc;D)Z"),
			remap = false)
	private boolean vivecraftcompat$useSableSeatedHandleRange(Player player, Vector3dc target, double range) {
		return SableVRBridge.isInSeatedInteractionRange(player, target, range);
	}
}

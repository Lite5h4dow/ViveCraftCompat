package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.Position;
import net.minecraft.world.entity.player.Player;

import com.tom.vivecraftcompat.simulated.SableVRBridge;

@Mixin(targets = "dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelHandler")
public class SimulatedSteeringWheelHandlerMixin {

	@Redirect(
			method = "activeTick",
			at = @At(value = "INVOKE", target = "Ldev/simulated_team/simulated/util/hold_interaction/BlockHoldInteraction;inInteractionRange(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/Position;)Z"),
			remap = false)
	private boolean vivecraftcompat$useSableSeatedInteractionRange(Player player, Position target) {
		return SableVRBridge.isInSeatedInteractionRange(player, target, 0.0D);
	}
}

package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;

import com.tom.vivecraftcompat.simulated.SableVRBridge;

@Mixin(UseOnContext.class)
public class SimulatedUseOnContextMixin {

	@Inject(method = "getHorizontalDirection", at = @At("HEAD"), cancellable = true)
	private void vivecraftcompat$useSableLocalHorizontalDirection(CallbackInfoReturnable<Direction> cir) {
		UseOnContext context = (UseOnContext) (Object) this;
		Player player = context.getPlayer();
		Direction direction = SableVRBridge.getSeatedPlacementHorizontalDirection(context.getLevel(), player,
				context.getClickedPos().getCenter());
		if (direction != null)
			cir.setReturnValue(direction);
	}

	@Inject(method = "getRotation", at = @At("HEAD"), cancellable = true)
	private void vivecraftcompat$useSableLocalRotation(CallbackInfoReturnable<Float> cir) {
		UseOnContext context = (UseOnContext) (Object) this;
		Player player = context.getPlayer();
		Float yaw = SableVRBridge.getSeatedPlacementYaw(context.getLevel(), player, context.getClickedPos().getCenter());
		if (yaw != null)
			cir.setReturnValue(yaw);
	}
}

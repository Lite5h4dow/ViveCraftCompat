package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;

import com.tom.vivecraftcompat.simulated.SableVRBridge;

@Mixin(BlockPlaceContext.class)
public class SimulatedBlockPlaceContextMixin {
	@Shadow
	protected boolean replaceClicked;

	@Inject(method = "getNearestLookingDirection", at = @At("HEAD"), cancellable = true)
	private void vivecraftcompat$useSableLocalNearestDirection(CallbackInfoReturnable<Direction> cir) {
		Direction[] directions = getSableLocalDirections();
		if (directions != null)
			cir.setReturnValue(directions[0]);
	}

	@Inject(method = "getNearestLookingVerticalDirection", at = @At("HEAD"), cancellable = true)
	private void vivecraftcompat$useSableLocalVerticalDirection(CallbackInfoReturnable<Direction> cir) {
		BlockPlaceContext context = (BlockPlaceContext) (Object) this;
		Player player = context.getPlayer();
		Direction direction = SableVRBridge.getSeatedPlacementVerticalDirection(context.getLevel(), player,
				context.getClickedPos().getCenter());
		if (direction != null)
			cir.setReturnValue(direction);
	}

	@Inject(method = "getNearestLookingDirections", at = @At("HEAD"), cancellable = true)
	private void vivecraftcompat$useSableLocalDirections(CallbackInfoReturnable<Direction[]> cir) {
		Direction[] directions = getSableLocalDirections();
		if (directions != null)
			cir.setReturnValue(directions);
	}

	private Direction[] getSableLocalDirections() {
		BlockPlaceContext context = (BlockPlaceContext) (Object) this;
		Player player = context.getPlayer();
		Direction[] directions = SableVRBridge.getSeatedPlacementDirections(context.getLevel(), player,
				context.getClickedPos().getCenter());
		if (directions == null)
			return null;

		if (this.replaceClicked)
			return directions;

		Direction forced = context.getClickedFace().getOpposite();
		int index = 0;
		while (index < directions.length && directions[index] != forced)
			index++;

		if (index > 0) {
			System.arraycopy(directions, 0, directions, 1, index);
			directions[0] = forced;
		}

		return directions;
	}
}

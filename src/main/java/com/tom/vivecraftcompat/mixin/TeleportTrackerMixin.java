package com.tom.vivecraftcompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.tom.vivecraftcompat.create.CreateSchematicVRInputBridge;

@Mixin(targets = "org.vivecraft.client_vr.gameplay.trackers.TeleportTracker")
public class TeleportTrackerMixin {
	private static final double MAX_REASONABLE_TELEPORT_DISTANCE = 128.0D;
	private static final double MAX_DESTINATION_DISTANCE_FROM_HIT = 8.0D;

	@Shadow(remap = false)
	private Vec3 movementTeleportDestination;

	@Shadow(remap = false)
	public double movementTeleportDistance;

	@Shadow(remap = false)
	public int movementTeleportArcSteps;

	@Inject(method = "isActive", at = @At("HEAD"), cancellable = true, remap = false)
	private void vivecraftcompat$disableTeleportForSchematicAlt(LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if (CreateSchematicVRInputBridge.shouldSuppressTeleport())
			cir.setReturnValue(false);
	}

	@Inject(method = "checkAndSetTeleportDestination", at = @At("RETURN"), cancellable = true, remap = false)
	private void vivecraftcompat$rejectBadPhysicsTeleport(LocalPlayer player, Vec3 arcStart, BlockHitResult hit,
			CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ())
			return;

		Vec3 destination = movementTeleportDestination;
		if (isBadTeleportDestination(player, hit, destination)) {
			movementTeleportDestination = Vec3.ZERO;
			movementTeleportDistance = 0.0D;
			movementTeleportArcSteps = 0;
			cir.setReturnValue(false);
		}
	}

	private static boolean isBadTeleportDestination(LocalPlayer player, BlockHitResult hit, Vec3 destination) {
		if (player == null || hit == null || destination == null)
			return true;
		if (!isFinite(destination))
			return true;
		if (destination.distanceTo(player.position()) > MAX_REASONABLE_TELEPORT_DISTANCE)
			return true;

		Vec3 hitLocation = hit.getLocation();
		if (!isFinite(hitLocation))
			return true;

		return destination.distanceTo(hitLocation) > MAX_DESTINATION_DISTANCE_FROM_HIT
				&& destination.distanceTo(Vec3.atCenterOf(hit.getBlockPos())) > MAX_DESTINATION_DISTANCE_FROM_HIT;
	}

	private static boolean isFinite(Vec3 value) {
		return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
	}
}

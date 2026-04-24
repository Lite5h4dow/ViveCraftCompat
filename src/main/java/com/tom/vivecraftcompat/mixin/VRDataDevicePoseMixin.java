package com.tom.vivecraftcompat.mixin;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.VRData.VRDevicePose;

import net.minecraft.world.phys.Vec3;

import com.tom.vivecraftcompat.simulated.SableVRBridge;

@Mixin(VRDevicePose.class)
public class VRDataDevicePoseMixin {
	@Shadow
	@Final
	private VRData data;

	@Inject(method = "getPosition", at = @At("RETURN"), cancellable = true, remap = false)
	private void vivecraftcompat$transformSableVrPosition(CallbackInfoReturnable<Vec3> cir) {
		cir.setReturnValue(SableVRBridge.transformVrPosition(this.data, cir.getReturnValue()));
	}

	@Inject(method = "getPositionF", at = @At("RETURN"), cancellable = true, remap = false)
	private void vivecraftcompat$transformSableVrPositionF(CallbackInfoReturnable<Vector3f> cir) {
		Vector3f position = cir.getReturnValue();
		Vec3 transformed = SableVRBridge.transformVrPosition(this.data,
				new Vec3(position.x, position.y, position.z));
		cir.setReturnValue(new Vector3f((float) transformed.x, (float) transformed.y, (float) transformed.z));
	}

	@Inject(method = "getScalePositionOffset", at = @At("RETURN"), cancellable = true, remap = false)
	private void vivecraftcompat$transformSableVrScaleOffset(float scale, CallbackInfoReturnable<Vector3f> cir) {
		cir.setReturnValue(SableVRBridge.transformVrDirection(this.data, cir.getReturnValue()));
	}

	@Inject(method = "getDirection", at = @At("RETURN"), cancellable = true, remap = false)
	private void vivecraftcompat$transformSableVrDirection(CallbackInfoReturnable<Vector3f> cir) {
		cir.setReturnValue(SableVRBridge.transformVrDirection(this.data, cir.getReturnValue()));
	}

	@Inject(method = "getCustomVector", at = @At("RETURN"), cancellable = true, remap = false)
	private void vivecraftcompat$transformSableVrCustomVector(Vector3fc vector,
			CallbackInfoReturnable<Vector3f> cir) {
		cir.setReturnValue(SableVRBridge.transformVrDirection(this.data, cir.getReturnValue()));
	}

	@Inject(method = "getMatrix", at = @At("RETURN"), cancellable = true, remap = false)
	private void vivecraftcompat$transformSableVrMatrix(CallbackInfoReturnable<Matrix4f> cir) {
		cir.setReturnValue(SableVRBridge.transformVrMatrix(this.data, cir.getReturnValue()));
	}
}

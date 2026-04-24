package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.HitResult;

import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.simulated.SimulatedVRInputBridge;

@Mixin(GameRenderer.class)
public class SimulatedGameRendererMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "pick(F)V", at = @At("TAIL"))
	private void vivecraftcompat$pickFromVRHand(float partialTicks, CallbackInfo ci) {
		if (!VRMode.isVRHandAiming() || this.minecraft.player == null || this.minecraft.level == null
				|| this.minecraft.screen != null)
			return;

		HitResult hit = SimulatedVRInputBridge.pickFromRightHand(this.minecraft.player,
				this.minecraft.player.blockInteractionRange(), partialTicks, false);
		if (hit == null || hit.getType() == HitResult.Type.MISS)
			return;

		this.minecraft.hitResult = hit;
		this.minecraft.crosshairPickEntity = null;
	}
}

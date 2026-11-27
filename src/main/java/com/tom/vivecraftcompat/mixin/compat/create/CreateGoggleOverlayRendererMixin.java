package com.tom.vivecraftcompat.mixin.compat.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.tom.vivecraftcompat.create.CreateGoggleOverlay;
import com.tom.vivecraftcompat.overlay.OverlayManager;

/**
 * Drops Create's HUD goggles panel while a floating overlay owns the same layer.
 * The overlay manager's capture re-enters {@code renderOverlay}; that call must
 * still draw into its framebuffer.
 */
@Mixin(GoggleOverlayRenderer.class)
public class CreateGoggleOverlayRendererMixin {
	@Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
	private static void vivecraftcompat$skipHudWhenPinned(GuiGraphics graphics, DeltaTracker partialTick,
			CallbackInfo ci) {
		if (!OverlayManager.isRendering() && OverlayManager.isOverlayDetached(CreateGoggleOverlay.ID))
			ci.cancel();
	}
}

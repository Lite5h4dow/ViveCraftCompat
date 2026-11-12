package com.tom.vivecraftcompat.mixin.compat.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.tom.vivecraftcompat.create.GoggleOverlayBillboard;

/**
 * Drops Create's HUD goggles panel while VR draws the same panel in world space
 * above the hovered block. The method still runs — {@link GoggleOverlayBillboard}
 * calls it — so the hover tracking and outline scheduling it performs are
 * unaffected; only the HUD copy is skipped.
 */
@Mixin(GoggleOverlayRenderer.class)
public class CreateGoggleOverlayRendererMixin {
	@Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
	private static void vivecraftcompat$skipHudPanelInVr(GuiGraphics graphics, DeltaTracker partialTick,
			CallbackInfo ci) {
		if (!GoggleOverlayBillboard.isDrawing() && GoggleOverlayBillboard.replacesHudPanel())
			ci.cancel();
	}
}

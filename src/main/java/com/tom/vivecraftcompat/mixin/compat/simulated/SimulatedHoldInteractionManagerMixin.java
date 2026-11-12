package com.tom.vivecraftcompat.mixin.compat.simulated;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;

import com.tom.vivecraftcompat.simulated.HoldInteractionBillboard;

/**
 * Drops Simulated's screen-centred hold-interaction panel while VR is drawing
 * the same panel in world space above the block instead. See
 * {@link HoldInteractionBillboard} for why the panel is re-posed rather than
 * rewritten.
 */
@Mixin(targets = "dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager")
public class SimulatedHoldInteractionManagerMixin {
	@Inject(method = "renderOverlay(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true)
	private static void vivecraftcompat$skipHudPanelInVr(GuiGraphics graphics, int width, int height, CallbackInfo ci) {
		if (HoldInteractionBillboard.replacesHudPanel())
			ci.cancel();
	}
}

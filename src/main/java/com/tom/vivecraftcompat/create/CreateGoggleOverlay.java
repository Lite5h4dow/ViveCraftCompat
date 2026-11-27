package com.tom.vivecraftcompat.create;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.tom.vivecraftcompat.ViveCraftCompat;
import com.tom.vivecraftcompat.overlay.OverlayManager;

/**
 * Named HUD layer for Create's goggles readout so the overlay manager can pin it.
 * <p>
 * The layer is only drawn while {@link OverlayManager} is capturing overlay
 * framebuffers; the normal HUD copy is still Create's own layer. When the player
 * detaches this layer into a floating overlay, the mixin on
 * {@link GoggleOverlayRenderer#renderOverlay} drops Create's HUD copy, so the
 * panel is drawn exactly once — inside the overlay framebuffer.
 */
public class CreateGoggleOverlay {
	public static final ResourceLocation ID = ResourceLocation.tryBuild(ViveCraftCompat.MODID, "create_goggles");

	public static void register(RegisterGuiLayersEvent event) {
		event.registerAboveAll(ID, CreateGoggleOverlay::render);
	}

	public static void render(GuiGraphics graphics, DeltaTracker partialTick) {
		if (!OverlayManager.isRendering())
			return;
		GoggleOverlayRenderer.renderOverlay(graphics, partialTick);
	}
}

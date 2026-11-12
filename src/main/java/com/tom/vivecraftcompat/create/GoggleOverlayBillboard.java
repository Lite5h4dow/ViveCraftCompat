package com.tom.vivecraftcompat.create;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.tom.vivecraftcompat.VRMode;

/**
 * Anchors the goggles readout to the block it describes instead of the HUD.
 * <p>
 * Create draws one panel for every pair of goggles in the game: kinetic speed,
 * stress impact and capacity, and whatever {@code IHaveGoggleInformation} adds
 * on top. Aeronautics' aviators are one of those — they register themselves
 * through {@code GogglesItem.addIsWearingPredicate} and their blocks supply
 * extra lines — so this covers both without touching either mod.
 * <p>
 * The panel is positioned off the mouse cursor inside
 * {@code GoggleOverlayRenderer.renderOverlay}, which in VR means it ends up on
 * the world-space GUI layer wherever the pointer happens to be rather than next
 * to the machine being inspected. Since the whole GUI is translated by the same
 * amount whenever there is no screen open, this re-poses that single call: the
 * panel is drawn above the hovered block, camera-facing.
 * <p>
 * The HUD copy is suppressed by {@code CreateGoggleOverlayRendererMixin} on the
 * same predicate, so the method still runs its hover bookkeeping exactly once
 * per frame and the panel is drawn exactly once.
 */
public class GoggleOverlayBillboard {
	private static final float PANEL_SCALE = 0.008F;
	private static final double LABEL_HEIGHT = 1.5D;

	/** Guards against our own call being cancelled by the HUD mixin. */
	private static boolean drawing;

	/** Whether the world-space copy is standing in for the HUD panel this frame. */
	public static boolean replacesHudPanel() {
		Minecraft minecraft = Minecraft.getInstance();
		return VRMode.isVR() && minecraft.screen == null && minecraft.player != null
				&& !minecraft.options.hideGui
				&& GogglesItem.isWearingGoggles(minecraft.player);
	}

	public static boolean isDrawing() {
		return drawing;
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
			return;

		if (!replacesHudPanel())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		BlockPos pos = GoggleOverlayRenderer.lastHovered;
		if (pos == null && minecraft.hitResult instanceof BlockHitResult blockHit)
			pos = blockHit.getBlockPos();
		if (pos == null)
			return;

		Vec3 camera = event.getCamera().getPosition();
		GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
		PoseStack pose = graphics.pose();

		pose.pushPose();
		pose.translate(pos.getX() + 0.5D - camera.x, pos.getY() + LABEL_HEIGHT - camera.y,
				pos.getZ() + 0.5D - camera.z);
		pose.mulPose(event.getCamera().rotation());
		pose.scale(-PANEL_SCALE, -PANEL_SCALE, PANEL_SCALE);
		// The panel is laid out around the GUI centre when the cursor sits at it,
		// which is where Vivecraft parks the pointer with no screen open.
		pose.translate(-graphics.guiWidth() / 2.0F, -graphics.guiHeight() / 2.0F, 0.0F);

		drawing = true;
		try {
			GoggleOverlayRenderer.renderOverlay(graphics, event.getPartialTick());
		} catch (RuntimeException e) {
			// A readout must never break the frame.
		} finally {
			drawing = false;
		}

		pose.popPose();
		minecraft.renderBuffers().bufferSource().endBatch();
	}
}

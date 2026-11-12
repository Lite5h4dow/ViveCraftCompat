package com.tom.vivecraftcompat.simulated;

import java.lang.reflect.Method;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Floating, camera-facing copy of Simulated's own throttle lever overlay.
 *
 * The mod already draws a complete lever readout — brass frame, value bar,
 * cursor and the numeric signal — through
 * ThrottleLeverHandler.renderOverlay, using Create's AllGuiTextures assets. In
 * the HUD that panel is pinned to the middle of the screen, which is useless
 * once you are standing over the lever in VR and want to watch the thing you are
 * actually moving.
 *
 * So this does not redraw the element: it transforms the GuiGraphics pose to a
 * world-space billboard above the lever and calls the same renderOverlay. Same
 * assets, same layout, same value, now attached to the block and turned to face
 * the player holding it.
 */
public class ThrottleLeverValueOverlay {
	private static final double LABEL_HEIGHT = 1.15D;
	private static final float PANEL_SCALE = 0.012F;
	// Virtual screen handed to renderOverlay. Its layout centres on (width/2, height/2),
	// so these values only matter for where the panel lands relative to the pose origin.
	private static final int VIRTUAL_WIDTH = 100;
	private static final int VIRTUAL_HEIGHT = 14;

	private static Method renderOverlayMethod;

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return;

		// Only while the lever is actually being controlled — that is the moment the
		// readout is worth having in the world rather than stuck on the HUD.
		BlockPos pos = SimulatedVRInputBridge.getThrottleLeverGripPos();
		Object handler = SimulatedVRInputBridge.getThrottleLeverHandler();
		if (pos == null || handler == null)
			return;

		Method render = renderOverlayFor(handler);
		if (render == null)
			return;

		Vec3 camera = event.getCamera().getPosition();
		GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
		PoseStack pose = graphics.pose();

		pose.pushPose();
		pose.translate(pos.getX() + 0.5D - camera.x, pos.getY() + LABEL_HEIGHT - camera.y,
				pos.getZ() + 0.5D - camera.z);
		pose.mulPose(event.getCamera().rotation());
		pose.scale(-PANEL_SCALE, -PANEL_SCALE, PANEL_SCALE);

		try {
			render.invoke(handler, graphics, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, false);
		} catch (ReflectiveOperationException | RuntimeException e) {
			// A readout must never break the frame or the input bridge.
			renderOverlayMethod = null;
		}

		pose.popPose();
		minecraft.renderBuffers().bufferSource().endBatch();
	}

	private static Method renderOverlayFor(Object handler) {
		if (renderOverlayMethod != null)
			return renderOverlayMethod;

		try {
			renderOverlayMethod = handler.getClass().getMethod("renderOverlay", GuiGraphics.class, int.class,
					int.class, boolean.class);
			return renderOverlayMethod;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}

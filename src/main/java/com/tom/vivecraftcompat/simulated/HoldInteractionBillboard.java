package com.tom.vivecraftcompat.simulated;

import java.lang.reflect.Method;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.tom.vivecraftcompat.VRMode;

/**
 * Moves Simulated's hold-interaction readouts out of the middle of the screen
 * and onto the block being held.
 * <p>
 * Every hold interaction — throttle lever, steering wheel, physics assembler,
 * sublevel handle — ends up in the same panel renderer,
 * {@code HoldInteractionManager.renderOverlay}, which pins the panel to
 * {@code (guiWidth / 2, guiHeight / 2)}. That is reasonable on a monitor and
 * useless in VR, where the block you are holding is usually off to one side and
 * the panel is a slab of brass hanging in your face at a fixed depth.
 * <p>
 * Rather than suppress the HUD copy and redraw anything, this re-poses the same
 * call: the panel is drawn above the block, turned to face the camera, using the
 * mod's own assets, layout and values. Suppression happens in
 * {@code SimulatedHoldInteractionManagerMixin}, gated on the same predicate, so
 * the panel appears exactly once.
 */
public class HoldInteractionBillboard {
	private static final double LABEL_HEIGHT = 1.15D;
	private static final float PANEL_SCALE = 0.012F;
	// Virtual canvas handed to renderOverlay. Its layout centres on
	// (width / 2, height / 2), so these only set where the panel sits relative to
	// the pose origin, not its size.
	private static final int VIRTUAL_WIDTH = 100;
	private static final int VIRTUAL_HEIGHT = 14;

	private static Method renderOverlayMethod;

	/**
	 * Whether the world-space copy is standing in for Simulated's HUD panel this
	 * frame. Both the mixin and the renderer ask this, so they cannot disagree and
	 * drop the readout altogether.
	 */
	public static boolean replacesHudPanel() {
		Minecraft minecraft = Minecraft.getInstance();
		return VRMode.isVR() && minecraft.screen == null && minecraft.player != null
				&& !minecraft.options.hideGui
				&& SimulatedVRInputBridge.getActiveHoldInteraction() != null;
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
			return;

		if (!replacesHudPanel())
			return;

		Object interaction = SimulatedVRInputBridge.getActiveHoldInteraction();
		BlockPos pos = SimulatedVRInputBridge.getActiveHoldInteractionPos();
		if (interaction == null || pos == null)
			return;

		Method render = renderOverlayFor(interaction);
		if (render == null)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		Vec3 camera = event.getCamera().getPosition();
		GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
		PoseStack pose = graphics.pose();

		pose.pushPose();
		pose.translate(pos.getX() + 0.5D - camera.x, pos.getY() + LABEL_HEIGHT - camera.y,
				pos.getZ() + 0.5D - camera.z);
		pose.mulPose(event.getCamera().rotation());
		pose.scale(-PANEL_SCALE, -PANEL_SCALE, PANEL_SCALE);

		try {
			render.invoke(interaction, graphics, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, false);
		} catch (ReflectiveOperationException | RuntimeException e) {
			// A readout must never break the frame or the input bridge.
			renderOverlayMethod = null;
		}

		pose.popPose();
		minecraft.renderBuffers().bufferSource().endBatch();
	}

	private static Method renderOverlayFor(Object interaction) {
		if (renderOverlayMethod != null)
			return renderOverlayMethod;

		try {
			renderOverlayMethod = interaction.getClass().getMethod("renderOverlay", GuiGraphics.class,
					int.class, int.class, boolean.class);
			return renderOverlayMethod;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}

package com.tom.vivecraftcompat.mixin;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.client_vr.render.helpers.RenderHelper;
import org.vivecraft.client_vr.render.helpers.VREffectsHelper;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tom.vivecraftcompat.overlay.OverlayManager;
import com.tom.vivecraftcompat.simulated.SableVRBridge;

import net.minecraft.client.Minecraft;

@Mixin(value = VREffectsHelper.class)
public abstract class VREffectsHelperMixin {
	private static int stableGuiModelViewDepth;

	@Inject(at = @At("HEAD"), method = "renderGuiAndShadow", remap = false)
	private static void suppressSableVrTransformForGui(float partialTicks, boolean depthAlways, boolean shadowFirst,
			CallbackInfo cbi) {
		SableVRBridge.pushGuiVrTransformSuppression();
	}

	@Inject(at = @At("RETURN"), method = "renderGuiAndShadow", remap = false)
	private static void restoreSableVrTransformAfterGui(float partialTicks, boolean depthAlways, boolean shadowFirst,
			CallbackInfo cbi) {
		SableVRBridge.popGuiVrTransformSuppression();
	}

	@Inject(at = @At("HEAD"), method = "renderGuiLayer", remap = false)
	private static void pushStableModelViewForGuiLayer(float partialTicks, boolean depthAlways, CallbackInfo cbi) {
		pushStableGuiModelView();
	}

	@Inject(at = @At("RETURN"), method = "renderGuiLayer", remap = false)
	private static void popStableModelViewForGuiLayer(float partialTicks, boolean depthAlways, CallbackInfo cbi) {
		popStableGuiModelView();
	}

	@Inject(at = @At("HEAD"), method = "render2D", remap = false)
	private static void pushStableModelViewForRender2D(float partialTicks, RenderTarget framebuffer, Vector3fc pos,
			Matrix4f rotation, boolean depthAlways, CallbackInfo cbi) {
		pushStableGuiModelView();
	}

	@Inject(at = @At("RETURN"), method = "render2D", remap = false)
	private static void popStableModelViewForRender2D(float partialTicks, RenderTarget framebuffer, Vector3fc pos,
			Matrix4f rotation, boolean depthAlways, CallbackInfo cbi) {
		popStableGuiModelView();
	}

	@Inject(at = @At("HEAD"), method = "renderPhysicalKeyboard", remap = false)
	private static void pushStableModelViewForPhysicalKeyboard(float partialTicks, CallbackInfo cbi) {
		pushStableGuiModelView();
	}

	@Inject(at = @At("RETURN"), method = "renderPhysicalKeyboard", remap = false)
	private static void popStableModelViewForPhysicalKeyboard(float partialTicks, CallbackInfo cbi) {
		popStableGuiModelView();
	}

	@Inject(at = @At(value = "FIELD", target = "Lorg/vivecraft/client_vr/gameplay/screenhandlers/KeyboardHandler;SHOWING:Z", opcode = Opcodes.GETSTATIC, ordinal = 1, remap = false), method = "renderGuiAndShadow", remap = false)
	private static void renderHudLayers(float partialTicks, boolean depthAlways, boolean shadowFirst, CallbackInfo cbi) {
		OverlayManager.renderLayers(l -> VREffectsHelper.render2D(partialTicks, l.getFramebuffer(), l.getPos(), l.getRotation(), false));
		if (KeyboardHandler.SHOWING) {
			RenderSystem.clearDepth(1.0D);
			RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		}
	}

	@Inject(at = @At("HEAD"), method = "shouldRenderCrosshair", remap = false, cancellable = true)
	private static void shouldRenderCrosshair(CallbackInfoReturnable<Boolean> cbi) {
		if (OverlayManager.isUsingController())cbi.setReturnValue(false);
	}

	private static void pushStableGuiModelView() {
		if (!SableVRBridge.shouldStabilizeGuiVrModelView())
			return;

		stableGuiModelViewDepth++;
		SableVRBridge.pushGuiVrTransformSuppression();
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.identity();
		RenderHelper.applyVRModelView(ClientDataHolderVR.getInstance().currentPass, modelViewStack);
		RenderSystem.applyModelViewMatrix();
	}

	private static void popStableGuiModelView() {
		if (stableGuiModelViewDepth <= 0)
			return;

		stableGuiModelViewDepth--;
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.popMatrix();
		RenderSystem.applyModelViewMatrix();
		SableVRBridge.popGuiVrTransformSuppression();
	}
}

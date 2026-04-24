package com.tom.vivecraftcompat.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyboardHandler;

import com.tom.vivecraftcompat.simulated.SimulatedVRInputBridge;

@Mixin(KeyboardHandler.class)
public class MinecraftKeyboardHandlerMixin {

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void vivecraftcompat$closeTypewriterKeyboardWithEscape(long windowPointer, int key, int scanCode, int action,
			int modifiers, CallbackInfo ci) {
		if (key == GLFW.GLFW_KEY_ESCAPE && action != GLFW.GLFW_RELEASE && SimulatedVRInputBridge.dismissTypewriterKeyboardFromInput())
			ci.cancel();
	}
}

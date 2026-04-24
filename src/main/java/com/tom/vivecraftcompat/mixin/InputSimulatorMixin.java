package com.tom.vivecraftcompat.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.provider.InputSimulator;

import com.tom.vivecraftcompat.overlay.OverlayManager;
import com.tom.vivecraftcompat.simulated.SimulatedVRInputBridge;

@Mixin(InputSimulator.class)
public class InputSimulatorMixin {

	private @Shadow(remap = false) static Set<Integer> PRESSED_KEYS;

	@Inject(at = @At("HEAD"), method = "typeChar(C)V", cancellable = true, remap = false)
	private static void typeChar(char character, CallbackInfo cbi) {
		if(OverlayManager.type(character))cbi.cancel();
		else if(SimulatedVRInputBridge.typeTypewriterCharacter(character))cbi.cancel();
	}

	@Inject(at = @At("HEAD"), method = "pressKey(I)V", cancellable = true, remap = false)
	private static void pressKey(int key, CallbackInfo cbi) {
		if(OverlayManager.key(key)) {
			cbi.cancel();
			PRESSED_KEYS.add(key);
		} else if(SimulatedVRInputBridge.pressTypewriterKey(key)) {
			cbi.cancel();
			PRESSED_KEYS.add(key);
		}
	}

	@Inject(at = @At("HEAD"), method = "releaseKey(I)V", cancellable = true, remap = false)
	private static void releaseKey(int key, CallbackInfo cbi) {
		if(SimulatedVRInputBridge.releaseTypewriterKey(key)) {
			cbi.cancel();
			PRESSED_KEYS.remove(key);
		}
	}

	@Inject(at = @At("HEAD"), method = "pressKeyForBind(I)V", cancellable = true, remap = false)
	private static void pressKeyForBind(int key, CallbackInfo cbi) {
		if(SimulatedVRInputBridge.pressTypewriterKey(key)) {
			cbi.cancel();
			PRESSED_KEYS.add(key);
		}
	}

	@Inject(at = @At("HEAD"), method = "releaseKeyForBind(I)V", cancellable = true, remap = false)
	private static void releaseKeyForBind(int key, CallbackInfo cbi) {
		if(SimulatedVRInputBridge.releaseTypewriterKey(key)) {
			cbi.cancel();
			PRESSED_KEYS.remove(key);
		}
	}
}

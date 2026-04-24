package com.tom.vivecraftcompat.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.provider.MCVR;
import org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction;

import net.neoforged.neoforge.common.NeoForge;

import com.tom.vivecraftcompat.events.VRBindingsEvent;
import com.tom.vivecraftcompat.create.CreateSeatVRBridge;
import com.tom.vivecraftcompat.create.CreateSchematicVRInputBridge;
import com.tom.vivecraftcompat.overlay.OverlayManager;
import com.tom.vivecraftcompat.simulated.SimulatedVRInputBridge;

@Mixin(MCVR.class)
public class MCVRMixin {
	protected @Shadow(remap = false) Map<String, VRInputAction> inputActions;
	protected @Shadow(remap = false) float seatedRot;

	@Inject(at = @At("HEAD"), method = "processBindings", remap = false)
	private void processBindings(CallbackInfo cbi) {
		CreateSchematicVRInputBridge.beforeProcessBindings(this.seatedRot);
		SimulatedVRInputBridge.beforeProcessBindings(this.seatedRot);
		if (!this.inputActions.isEmpty())
			NeoForge.EVENT_BUS.post(new VRBindingsEvent());
	}

	@Inject(at = @At("RETURN"), method = "processBindings", remap = false)
	private void afterProcessBindings(CallbackInfo cbi) {
		this.seatedRot = SimulatedVRInputBridge.afterProcessBindings(this.seatedRot);
		this.seatedRot = CreateSchematicVRInputBridge.afterProcessBindings(this.seatedRot);
		this.seatedRot = CreateSeatVRBridge.afterProcessBindings(this.seatedRot);
	}

	@Inject(at = @At("RETURN"), method = "populateInputActions", remap = false)
	private void populateInputActions(CallbackInfo cbi) {
		OverlayManager.populateListeners();
		SimulatedVRInputBridge.populateListeners();
	}
}

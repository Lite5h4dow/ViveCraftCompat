package com.tom.vivecraftcompat.mixin.compat.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.AllKeys;

import com.tom.vivecraftcompat.create.CreateSchematicVRInputBridge;

@Mixin(AllKeys.class)
public class CreateAllKeysMixin {

	@Inject(at = @At("HEAD"), method = "ctrlDown", cancellable = true, remap = false)
	private static void vivecraftcompat$useVRSchematicCtrl(CallbackInfoReturnable<Boolean> cir) {
		if (CreateSchematicVRInputBridge.isSchematicCtrlMode())
			cir.setReturnValue(true);
	}

	@Inject(at = @At("HEAD"), method = "altDown", cancellable = true, remap = false)
	private static void vivecraftcompat$useVRSchematicAlt(CallbackInfoReturnable<Boolean> cir) {
		if (CreateSchematicVRInputBridge.isSchematicAltMode())
			cir.setReturnValue(true);
	}

	@Inject(at = @At("HEAD"), method = "isPressed", cancellable = true, remap = false)
	private void vivecraftcompat$useVRSchematicCtrlAsActivateTool(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this == AllKeys.ACTIVATE_TOOL && CreateSchematicVRInputBridge.isSchematicCtrlMode())
			cir.setReturnValue(true);
	}
}

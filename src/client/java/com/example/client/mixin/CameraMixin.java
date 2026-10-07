package com.example.client.mixin;

import com.example.client.module.ZoomModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void rooty$zoomFov(CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(ZoomModule.modifyFov(cir.getReturnValue()));
	}
}

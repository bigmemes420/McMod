package com.example.client.mixin;

import com.example.client.module.NoHurtCamModule;
import com.example.client.module.TracersModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void rooty$noHurtCam(CameraRenderState state, PoseStack pose, CallbackInfo ci) {
		if (NoHurtCamModule.isEnabled()) {
			ci.cancel();
		}
	}

	/** Tracers need an unbobbed camera so screen-center origin stays fixed. */
	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
	private void rooty$tracersIgnoreBob(CameraRenderState state, PoseStack pose, CallbackInfo ci) {
		if (TracersModule.shouldIgnoreViewBobbing()) {
			ci.cancel();
		}
	}
}

package com.example.client.mixin;

import com.example.client.module.FullbrightModule;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When Fullbright is ON, force night-vision-intensity to 1 so the lightmap
 * fully brightens the world client-side.
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract", at = @At("RETURN"))
	private void rooty$fullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		if (FullbrightModule.isEnabled()) {
			state.nightVisionEffectIntensity = 1.0F;
			state.brightness = 1.0F;
		}
	}
}

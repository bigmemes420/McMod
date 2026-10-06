package com.example.client.mixin;

import com.example.client.module.XRayModule;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * MC 26.3 evidence: {@code SOLID_TERRAIN} has no translucent blend, so vertex
 * alpha only affects the image when the quad is written into the
 * {@code TRANSLUCENT} section buffer <em>and</em> the packed RGBA still carries
 * the fade alpha. {@link VertexConsumer#putBlockBakedQuad} copies
 * {@link com.mojang.blaze3d.vertex.QuadInstance} color into
 * {@link BufferBuilder#addVertex(float, float, float, int, float, float, int, int, float, float, float)},
 * which calls {@code putRgba}. This mixin re-applies the X-Ray mesh alpha on
 * that color argument — the last CPU-side write before the chunk mesh.
 */
@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {
	@ModifyVariable(
			method = "addVertex(FFFIFFIIFFF)V",
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 0
	)
	private int rooty$xrayKeepMeshAlpha(int color) {
		int alpha = XRayModule.getMeshAlpha();
		if (alpha > 0 && alpha < 255) {
			return XRayModule.applyFadeAlpha(color, alpha);
		}
		return color;
	}
}

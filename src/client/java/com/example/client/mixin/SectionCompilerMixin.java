package com.example.client.mixin;

import com.example.client.module.XRayModule;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * While X-Ray is fading a non-selected block, force its quads onto
 * {@link ChunkSectionLayer#TRANSLUCENT} so baked vertex alpha is honored —
 * including the SectionCompiler path that otherwise always writes SOLID for
 * force-opaque (e.g. leaves) outputs.
 */
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
	@ModifyVariable(
			method = "getOrBeginLayer",
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 0
	)
	private ChunkSectionLayer rooty$xrayForceTranslucent(ChunkSectionLayer layer) {
		if (XRayModule.isTranslucentPass()) {
			return ChunkSectionLayer.TRANSLUCENT;
		}
		return layer;
	}
}

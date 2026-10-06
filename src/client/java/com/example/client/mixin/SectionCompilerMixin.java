package com.example.client.mixin;

import com.example.client.module.XRayModule;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * MC 26.3: {@code SectionCompiler.lambda$compile$0} picks the buffer from
 * {@code BakedQuad.materialInfo().layer()}, while {@code lambda$compile$1}
 * (force-opaque) always passes {@link ChunkSectionLayer#SOLID}. SOLID uses
 * {@code SOLID_TERRAIN} which has no translucent blend — vertex alpha is
 * visually ignored. While X-Ray is fading a block, force every
 * {@code getOrBeginLayer} call onto {@link ChunkSectionLayer#TRANSLUCENT}
 * ({@code TRANSLUCENT_TERRAIN} / {@code BlendFunction.TRANSLUCENT}).
 */
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
	@ModifyVariable(
			method = "getOrBeginLayer",
			at = @At("HEAD"),
			argsOnly = true
	)
	private ChunkSectionLayer rooty$xrayForceTranslucent(ChunkSectionLayer layer) {
		if (XRayModule.isTranslucentPass()) {
			return ChunkSectionLayer.TRANSLUCENT;
		}
		return layer;
	}

	/**
	 * Force-opaque path hardcodes SOLID via GETSTATIC — rewrite to TRANSLUCENT
	 * while fading so leaves/cutout still honor slider alpha.
	 */
	@Redirect(
			method = "lambda$compile$1",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;SOLID:Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;",
					opcode = Opcodes.GETSTATIC
			)
	)
	private ChunkSectionLayer rooty$xrayReplaceForceOpaqueSolid() {
		if (XRayModule.isTranslucentPass()) {
			return ChunkSectionLayer.TRANSLUCENT;
		}
		return ChunkSectionLayer.SOLID;
	}
}

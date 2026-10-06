package com.example.client.mixin;

import com.example.client.module.XRayModule;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * X-Ray opacity 1–99%: multiply vertex alpha on non-selected blocks and mark
 * the current meshing pass so {@link SectionCompilerMixin} routes quads to the
 * translucent chunk layer (SOLID ignores alpha).
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
	@Shadow
	@Final
	private QuadInstance quadInstance;

	@Inject(
			method = "putQuadWithTint",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"
			)
	)
	private void rooty$xrayApplyAlpha(
			BlockQuadOutput output,
			float x,
			float y,
			float z,
			BlockAndTintGetter level,
			BlockState state,
			BlockPos pos,
			BakedQuad quad,
			CallbackInfo ci
	) {
		if (!XRayModule.shouldFade(state)) {
			return;
		}
		float alpha = XRayModule.getOpacityFraction();
		this.quadInstance.multiplyColor(ARGB.color(alpha, 0xFFFFFF));
		XRayModule.beginTranslucentPass();
	}

	@Inject(
			method = "putQuadWithTint",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V",
					shift = At.Shift.AFTER
			)
	)
	private void rooty$xrayClearTranslucent(
			BlockQuadOutput output,
			float x,
			float y,
			float z,
			BlockAndTintGetter level,
			BlockState state,
			BlockPos pos,
			BakedQuad quad,
			CallbackInfo ci
	) {
		XRayModule.endTranslucentPass();
	}
}

package com.example.client.mixin;

import com.example.client.module.XRayModule;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
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
 * X-Ray opacity 1–99%:
 * <ul>
 *   <li>Mark the whole {@code tesselateBlock} call as a translucent pass so
 *       {@link SectionCompilerMixin} routes every quad of a faded block onto
 *       {@code TRANSLUCENT} (SOLID ignores vertex alpha).</li>
 *   <li>Multiply vertex alpha on each quad to the current opacity fraction.</li>
 * </ul>
 * Prior fix set the translucent flag only around {@code put}, which is correct
 * in theory but fragile; scoping it to the full block tessellation matches how
 * {@code getOrBeginLayer} is invoked from the section compiler's BlockQuadOutput.
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
	@Shadow
	@Final
	private QuadInstance quadInstance;

	@Inject(method = "tesselateBlock", at = @At("HEAD"))
	private void rooty$xrayBeginBlock(
			BlockQuadOutput output,
			float x,
			float y,
			float z,
			BlockAndTintGetter level,
			BlockPos pos,
			BlockState state,
			BlockStateModel model,
			long seed,
			CallbackInfo ci
	) {
		if (XRayModule.shouldFade(state)) {
			XRayModule.beginTranslucentPass();
		}
	}

	@Inject(method = "tesselateBlock", at = @At("RETURN"))
	private void rooty$xrayEndBlock(
			BlockQuadOutput output,
			float x,
			float y,
			float z,
			BlockAndTintGetter level,
			BlockPos pos,
			BlockState state,
			BlockStateModel model,
			long seed,
			CallbackInfo ci
	) {
		XRayModule.endTranslucentPass();
	}

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
	}
}

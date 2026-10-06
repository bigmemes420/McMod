package com.example.client.mixin;

import com.example.client.module.XRayModule;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Absolute vertex-alpha X-Ray (opacity 1–99%):
 * <ul>
 *   <li>Mark the whole {@code tesselateBlock} as a translucent pass so
 *       {@link SectionCompilerMixin} routes quads onto TRANSLUCENT.</li>
 *   <li>Replace (not multiply) each vertex alpha with the slider byte —
 *       Meteor-style absolute alpha that SOLID would ignore.</li>
 * </ul>
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
	@Shadow
	@Final
	private QuadInstance quadInstance;

	@Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
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
		if (!XRayModule.shouldFade(state)) {
			return;
		}
		if (XRayModule.getOpacityAlpha() <= 0) {
			ci.cancel();
			return;
		}
		XRayModule.beginTranslucentPass();
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
		// Absolute alpha replace (keeps RGB) — not multiplyColor.
		for (int i = 0; i < 4; i++) {
			this.quadInstance.setColor(i, XRayModule.applyFadeAlpha(this.quadInstance.getColor(i)));
		}
	}
}

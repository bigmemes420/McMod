package com.example.client.mixin;

import com.example.client.module.XRayModule;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meteor Client {@code ModelBlockRendererMixin} port for MC 26.3, with the
 * SectionCompiler translucent-pass flag that MC 26.3's force-opaque SOLID
 * output path requires for non-selected alpha to actually show:
 * <ul>
 *   <li>Cancel tessellation when alpha is 0 (hidden)</li>
 *   <li>Meteor {@code multiplyColor} + absolute alpha bake before {@code put}</li>
 *   <li>Rewrite quad layer to TRANSLUCENT + {@link XRayModule#beginTranslucentPass}</li>
 *   <li>Selected / inactive ({@code alpha == -1}) left untouched at 100%</li>
 * </ul>
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
	@Shadow
	@Final
	private QuadInstance quadInstance;

	@Unique
	private static final ThreadLocal<Integer> ALPHAS = ThreadLocal.withInitial(() -> -1);

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
		int alpha = XRayModule.getAlpha(state, pos);
		ALPHAS.set(alpha);
		if (alpha == 0) {
			ci.cancel();
			return;
		}
		if (alpha > 0 && alpha < 255) {
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
		ALPHAS.set(-1);
	}

	@Inject(method = "putQuadWithTint", at = @At("HEAD"))
	private void rooty$xrayPutQuadMultiply(
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
		int alpha = ALPHAS.get();
		if (alpha != -1) {
			// Meteor: multiply existing vertex colors by (A,255,255,255).
			this.quadInstance.multiplyColor(ARGB.color(alpha, 255, 255, 255));
		}
	}

	@Inject(
			method = "putQuadWithTint",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"
			)
	)
	private void rooty$xrayPutQuadAbsolute(
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
		int alpha = ALPHAS.get();
		if (alpha == -1) {
			return;
		}
		// Absolute alpha replace after tint multiply — SOLID ignores relative alpha.
		for (int i = 0; i < 4; i++) {
			this.quadInstance.setColor(i, XRayModule.applyFadeAlpha(this.quadInstance.getColor(i), alpha));
		}
	}

	@ModifyArg(
			method = "putQuadWithTint",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"
			),
			index = 3
	)
	private BakedQuad rooty$xrayForceTranslucentLayer(BakedQuad quad) {
		int alpha = ALPHAS.get();
		if (alpha <= 0 || alpha >= 255) {
			return quad;
		}
		BakedQuad.MaterialInfo info = quad.materialInfo();
		if (info.layer() == ChunkSectionLayer.TRANSLUCENT) {
			return quad;
		}
		BakedQuad.MaterialInfo translucent = new BakedQuad.MaterialInfo(
				info.sprite(),
				ChunkSectionLayer.TRANSLUCENT,
				info.itemRenderType(),
				info.itemGlintRenderType(),
				info.itemGlintSpecialRenderType(),
				info.tintIndex(),
				info.shadeDirectionOverride(),
				info.lightEmission()
		);
		return new BakedQuad(
				quad.position0(),
				quad.position1(),
				quad.position2(),
				quad.position3(),
				quad.packedUV0(),
				quad.packedUV1(),
				quad.packedUV2(),
				quad.packedUV3(),
				quad.direction(),
				translucent
		);
	}

	@Inject(method = "shouldRenderFace", at = @At("RETURN"), cancellable = true)
	private void rooty$xrayModifyDrawSide(
			BlockAndTintGetter level,
			BlockState state,
			Direction direction,
			BlockPos neighborPos,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (!XRayModule.isActive()) {
			return;
		}
		BlockPos pos = neighborPos.relative(direction.getOpposite());
		cir.setReturnValue(XRayModule.modifyDrawSide(state, level, pos, direction, cir.getReturnValue()));
	}
}

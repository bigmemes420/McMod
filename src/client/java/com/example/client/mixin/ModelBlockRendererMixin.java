package com.example.client.mixin;

import com.example.client.module.XRayModule;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.List;

/**
 * Meteor Client {@code ModelBlockRendererMixin} port for MC 26.3:
 * cancel tessellation when alpha is 0, bake absolute vertex alpha for faded
 * blocks, rewrite the quad's {@link ChunkSectionLayer} to TRANSLUCENT, and
 * apply {@link XRayModule#modifyDrawSide} on face culling.
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
	@Shadow
	@Final
	private QuadInstance quadInstance;

	@Unique
	private static final ThreadLocal<Integer> ALPHAS = ThreadLocal.withInitial(() -> -1);

	@Inject(method = {"tesselateFlat", "tesselateAmbientOcclusion"}, at = @At("HEAD"), cancellable = true)
	private void rooty$xrayTesselate(
			BlockQuadOutput output,
			float x,
			float y,
			float z,
			List<?> parts,
			BlockAndTintGetter level,
			BlockState state,
			BlockPos pos,
			CallbackInfo ci
	) {
		int alpha = XRayModule.getAlpha(state, pos);
		if (alpha == 0) {
			ci.cancel();
			return;
		}
		ALPHAS.set(alpha);
	}

	@Inject(method = "putQuadWithTint", at = @At("HEAD"))
	private void rooty$xrayPutQuadAlpha(
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
		// Absolute alpha (Sodium Meteor path) — keeps RGB, replaces A.
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

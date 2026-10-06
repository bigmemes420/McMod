package com.example.client.mixin;

import com.example.client.module.XRayModule;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Meteor {@code FluidRendererMixin} (simplified): non-selected fluids respect
 * X-Ray opacity slider; selected fluid blocks stay 100% opaque.
 */
@Mixin(FluidRenderer.class)
public class FluidRendererMixin {
	@Unique
	private static final ThreadLocal<Integer> ALPHAS = ThreadLocal.withInitial(() -> -1);

	@Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
	private void rooty$xrayFluidTesselate(
			BlockAndTintGetter level,
			BlockPos pos,
			FluidRenderer.Output output,
			BlockState blockState,
			FluidState fluidState,
			CallbackInfo ci
	) {
		int alpha = XRayModule.getAlpha(fluidState.createLegacyBlock(), pos);
		if (alpha == 0) {
			ALPHAS.set(-1);
			ci.cancel();
			return;
		}
		ALPHAS.set(alpha);
	}

	@Inject(method = "tesselate", at = @At("RETURN"))
	private void rooty$xrayFluidTesselateEnd(
			BlockAndTintGetter level,
			BlockPos pos,
			FluidRenderer.Output output,
			BlockState blockState,
			FluidState fluidState,
			CallbackInfo ci
	) {
		ALPHAS.set(-1);
	}

	@Inject(method = "vertex", at = @At("HEAD"), cancellable = true)
	private void rooty$xrayFluidVertex(
			VertexConsumer builder,
			float x,
			float y,
			float z,
			int color,
			float u,
			float v,
			int lightCoords,
			CallbackInfo ci
	) {
		int alpha = ALPHAS.get();
		if (alpha == -1) {
			return;
		}
		builder.addVertex(x, y, z)
				.setColor(ARGB.red(color), ARGB.green(color), ARGB.blue(color), alpha)
				.setUv(u, v)
				.setLight(lightCoords)
				.setNormal(0.0F, 1.0F, 0.0F);
		ci.cancel();
	}

	@ModifyArg(
			method = "tesselate",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/block/FluidRenderer$Output;getBuilder(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
			),
			index = 0
	)
	private ChunkSectionLayer rooty$xrayFluidLayer(ChunkSectionLayer layer) {
		int alpha = ALPHAS.get();
		if (alpha > 0 && alpha < 255) {
			return ChunkSectionLayer.TRANSLUCENT;
		}
		return layer;
	}
}

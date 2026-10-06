package com.example.client.mixin;

import com.example.client.module.XRayModule;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Real-time X-Ray: whenever X-Ray is active (opacity < 100), skip the OPAQUE terrain group
 * (SOLID + CUTOUT) for this frame. No chunk remesh — the skip reads the live
 * opacity each draw.
 */
@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {
	@Inject(method = "renderGroup", at = @At("HEAD"), cancellable = true)
	private void rooty$xraySkipOpaque(
			ChunkSectionLayerGroup group,
			RenderPass renderPass,
			GpuSampler sampler,
			GpuTextureView atlas,
			boolean renderWireframeTerrain,
			CallbackInfo ci
	) {
		if (group == ChunkSectionLayerGroup.OPAQUE && XRayModule.shouldHideOpaqueTerrain()) {
			ci.cancel();
		}
	}
}
